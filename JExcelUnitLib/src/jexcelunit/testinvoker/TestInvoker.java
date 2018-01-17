package jexcelunit.testinvoker;


import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.closeTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import jexcelunit.classmodule.PrimitiveChecker;
import jexcelunit.excel.ExcelReader;
import jexcelunit.excel.ExcelResultSaver;
import jexcelunit.excel.MockVO;
import jexcelunit.excel.TestcaseVO;

/*****
 * Ŭ���� ���� : Reflection�� ���� ���� �׽��� �ڵ�.
 * �� Ŭ������ import�ϰԵ� �κ��� ���ԵǸ� �� ���ֵ�, CoffeeMaker�� �˰� �����ʴ�. ��,Ư�� ������Ʈ�� ������ ����.
 * �� Ŭ������ ��ӹ޾� �׽�Ʈ�ϰ��� �ϴ� ������Ʈ�� �°� ����ϸ� �ȴ�.
 * Date: 2016/03/18
 * Student Num : 2010112469
 * Major : ��ǻ�� ���� 
 * Name : ������ 
 * (���÷����� ����ϸ� �׽�Ʈ�޼ҵ�� ����� �� ���ϴ� �׽�Ʈ ��ü�� ������ �и��� ����)
 * sys
 **/
@SuppressWarnings("rawtypes")
@RunWith(Parameterized.class) //�׽�Ʈ ���̽��� �̿��Ұ��̴�.
public class TestInvoker {
	private static final JExcelLogger logger= new JExcelLogger();

	private static final Map<Class, Object> classmap=new HashMap<Class, Object>(); //�ؽ������� �׽�Ʈ�� �ʿ��� ��ü���� �ϳ����� �����Ѵ�.
	private static final ArrayList<Class> exceptionlist=new ArrayList<Class>();//����� ���� ���� Ŭ�������� ��Ƶδ� ��.
	private static final Map<String,String> sheets=new HashMap<String,String>();
	protected static final HashMap<String,Object> mock=new HashMap<String,Object>();//��ũ��ü ����
	private static int sheetNum= -1,rowIndex=0,testnumber=0;
	private static String jexcelPath, logPath;

	private static boolean[][] success=null;
	private static String[][] result=null;
	private static int[] rowSize=null;
	private static String currentSheet=null;

	//�׽�Ʈ ���̽����� Ȯ���� method_params
	private String sheet=null;
	private String testname=null;
	private Class targetclz=null;
	private Constructor constructor = null;
	private Object[] constructor_params=null;
	private Method targetmethod=null;
	private Object[] method_params=null;
	private Object expectedResult=null;

	//�׽�Ʈ�̸�, �׽�Ʈ�� Ŭ����, �׽�Ʈ�Ķ����,  �׽�Ʈ�� �޼ҵ��̸�, �Ķ���͵�,�������� JUnit�� �о�� �����Ű�� �κ��̴�.
	public TestInvoker(String sheet,String testname,Class targetclz,Constructor constructor,Object[] constructor_params,Method targetmethod,Object[] method_params,Object expectedResult){
		this.sheet=sheet;
		this.testname= (String)testname;
		this.targetclz=targetclz;
		this.constructor=constructor;
		this.constructor_params=constructor_params;
		this.expectedResult=expectedResult;
		this.targetmethod=targetmethod;
		this.method_params=method_params;

		if(currentSheet !=sheet){ 
			rowIndex=0; //�� �ʱ�ȭ
			sheetNum++;
			currentSheet=sheet;
			logger.testLog('['+currentSheet+"] Suite is Started");
			if(sheets.get(sheet).equals("Scenario")){ //���ο� �ó����� �׽�Ʈ
				logger.suiteLog("Scenario Test Suite " + sheetNum);
				classmap.clear();
			}
			else{
				logger.suiteLog("Unit Test Mode");
			} 
		}

		logger.testLog((testnumber++) + " [" + this.testname + "] Test is Started");
		logger.testLog("Test Target : " + this.constructor);
		logger.testLog("ConstructorInput : " + Arrays.toString(this.constructor_params));
		logger.testLog("Test Method : " + this.targetmethod);
		logger.testLog("MethodInput : " + Arrays.toString(this.method_params));
	}


	/*
	 * 1. ������̼����� ���� path�� �о��.
	 * 2. �޾ƿ�  path��  �ٽ� �����Ҷ� ���.
	 * 3. suite�� ���� row index�� �� �����ؾ��ϴ°� .
	 * 4. �׷��ٿ� suiteInfo ��� �ɹ�Ŭ������ �ּ� �����ϴ°� ��������.
	 * */
	public static Collection parmeterizingExcel(String jexcelLocation,String logLocation) throws InstantiationException{
		//��Ÿ�����͸� ������ �� �ۿ�����.
		//�ڵ鷯 �������� Ÿ�� ������Ʈ ������ �����Ұ�.
		jexcelPath=jexcelLocation;
		logPath=logLocation;
		File file = new File(jexcelLocation);
		ArrayList<ArrayList<TestcaseVO>> testcases=null;
		Object[][] parameterized= null;

		if(file.exists()){
			try {
				ExcelReader reader = new ExcelReader(jexcelLocation);
				testcases = reader.readExcel();

				if(testcases.size()>0){

					int total_row_index=0, maxRow=0,suiteNum=0;
					rowSize=new int[testcases.size()]; //suite��  rowSize�� �����Ұ�.
					for(ArrayList<TestcaseVO> testcase : testcases){
						int size= testcase.size();
						rowSize[suiteNum++]=size;
						total_row_index+=size;
						if(size>maxRow) maxRow=size;
					}
					parameterized = new Object[total_row_index][8];
					//init success
					success= new boolean[testcases.size()][maxRow];//�������������Ұ�
					for(int i=0; i<testcases.size(); i++)
						Arrays.fill(success[i], true);

					result= new String[testcases.size()][maxRow];//����� ����.


					//Setting SheetNames and TestMode.
					ArrayList<String> sheetModes=reader.getTestSheetMode();
					if( sheetModes.size() != testcases.size()) throw new InstantiationException("Check Sheet Info");

					for(int i=0; i<sheetModes.size(); i++){
						ArrayList<TestcaseVO> testcase = testcases.get(i);
						if(testcase.size()>0)
							sheets.put(testcase.get(0).getSheetName(), sheetModes.get(i));
						else throw new InstantiationException("There's no Test Case in the Sheet : "+testcase.get(0).getSheetName());
					}

					//Set @Parameters.
					int row_index=0;
					for(ArrayList<TestcaseVO> testcase : testcases){
						if(row_index < total_row_index){
							for(TestcaseVO currentCase: testcase){
								parameterized[row_index][0]=currentCase.getSheetName();
								parameterized[row_index][1]=currentCase.getTestname();
								parameterized[row_index][2]=currentCase.getTestclass();
								parameterized[row_index][3]=currentCase.getConstructor();
								parameterized[row_index][4]=currentCase.getConstructorParams().toArray();
								parameterized[row_index][5]=currentCase.getMet();
								parameterized[row_index][6]=currentCase.getMethodParams().toArray();
								parameterized[row_index][7]=currentCase.getExpect();	
								row_index++;
							}
						}
					}
				}

				//setUp Mock Object
				ArrayList<MockVO> mockList= reader.readMocks();
				if(mockList!=null)
					for(MockVO mockItem : mockList){

						logger.suiteLog("Class : "+mockItem.getConstructor());
						ArrayList<Object> consParams= mockItem.getConsParams();
						Object mockObject =null;
						if(consParams!=null ){
							Object [] params = consParams.toArray();
							mockObject = mockItem.getConstructor().newInstance(params);
							for(Object param : params){
								logger.suiteLog("Constructor Param : "+param);	
							}
						}
						else {
							mockObject = mockItem.getConstructor().newInstance();
						}

						if(mockObject ==null){
							logger.suiteFatal("Cant not Make Mock Object "+ " \""+mockItem.getMockName()+"\"");
							throw new Exception("Cant not Make Mock Object "+ " \""+mockItem.getMockName()+"\"");
						}

						Map<Field,Object> fieldSet = mockItem.getFieldSet();
						if(fieldSet !=null) {
							int index=0;
							Field[] fields = new Field[fieldSet.size()];
							Class[] fieldTypes= new Class[fieldSet.size()];
							Object[] values= new Object[fieldSet.size()];

							for(Field f : fieldSet.keySet()) {
								fields[index] = f;
								fieldTypes[index]= f.getType();
								values[index++] =fieldSet.get(f);							
							}
							values=getMock(fieldTypes, values);

							index=0;
							for(Field f:fields){
								f.setAccessible(true);
								f.set(mockObject, values[index]);
								logger.suiteLog("Set Field " + f.getName() +" : " + values[index++]);
							}
						}
						if(mock.get(mockItem.getMockName()) !=null){
							logger.suiteFatal("Duplicate Mock Name Error : " +mockItem.getMockName());
							throw new Exception("Duplicate Mock Name Error : " +mockItem.getMockName());
						}

						logger.suiteLog("Set the Mock : "+mockItem.getMockName()+"\n");
						mock.put(mockItem.getMockName(), mockObject);
					}

			} catch (Exception e) {
				logger.suiteFatal("Unknown Fatal Error in ParameterizingExcel");
				e.printStackTrace();
			}
		}
		//�о���� ����Ʈ�� String, Class, Object[] Object, String Object�� �ٱ����.
		return Arrays.asList(parameterized);
	} 

	//����ڰ� �����ϴ� �ͼ����� �ִ°�� �� �Լ��� ���� �����ش�
	public static void addException(Class e){
		exceptionlist.add(e);
	}

	private void handleException(Exception e){
		//e.printStackTrace();
		StackTraceElement[] exceptionClass=e.getCause().getStackTrace();
		if(exceptionClass!=null)
			for (StackTraceElement s: exceptionClass){ //���÷��� �ͼ����� ������ stacktrace ���
				System.out.println(s);
				if(s.getClassName().equals(Method.class.getName()))break;
			}
		if(!exceptionlist.isEmpty())
			for(Class ex :exceptionlist){
				if(e.getCause().getClass().equals(ex)){
					System.out.println(e.getCause()); //���� ���� ���
					break;
				}
			}
	}


	//TODO : �ó����� �׽�Ʈ ����.
	@Before
	public void setObj(){

		if(sheets.get(sheet).equals("Scenario")){
			if(!classmap.containsKey(targetclz)&& targetmethod !=null){ //�ó����� �׽�Ʈ���� ������ ��ü�� ���°��
				if(makeTestInstance())
					logger.testLog("Target " + targetclz+ " is created.");
				else
					logger.testFatal("Target Class is Abstract or Interface Type. : " + targetclz);
			}	
		}
		else if(sheets.get(sheet).equals("Units")){
			if(classmap.containsKey(targetclz))
				classmap.remove(targetclz);
			if(makeTestInstance())
				logger.testLog("Target " + targetclz+ " is created.");
			else
				logger.testFatal("Target Class is Abstract or Interface Type. : " + targetclz);
		}

		rowIndex++;
	}
	private boolean makeTestInstance(){
		constructor.setAccessible(true);
		if(!this.targetclz.isInterface()  && !Modifier.isAbstract( targetclz.getModifiers())){
			try {
				if(constructor_params.length==0)
					classmap.put(targetclz, constructor.newInstance());
				else{
					Class[] paramTypes=constructor.getParameterTypes();
					Object[] params= getMock(paramTypes,constructor_params);
					classmap.put(targetclz, constructor.newInstance(params));
				}
				return true;
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				logger.suiteFatal("Reflection Error.");
				handleException(e);
			} catch (Exception e) {
				logger.suiteFatal("Unknown Fatal Error in ParameterizingExcel");
				e.printStackTrace();
			}
		}else
			return false;
		return false;
	}

	private static Object[] getMock(Class[] types, Object[] params) throws Exception{
		for(int i= 0; i<types.length; i++){
			if(params[i]!=null){
				Class paramClass=params[i].getClass();
				if(PrimitiveChecker.isPrimitiveOrWrapper(paramClass))
					paramClass= PrimitiveChecker.unWrapping(paramClass);
				if(types[i].equals(String.class) && paramClass.equals(String.class)){
					Object mockObject= mock.get(params[i]);
					if(mockObject !=null)
						params[i]=mockObject;
				}
				if(!types[i].equals(paramClass)){ // ���� ó�� �Ŀ��� Ÿ���� ���� ���� ���. 1. primitive Ÿ�԰� wrapper Ÿ���� ����.	
					Object mockObject=mock.get(params[i]);
					if( types[i].isInstance(mockObject) && mockObject!=null){
						logger.testLog("The Mock named " + params[i] + " is set. ( " + mockObject + " )");
						params[i]=mockObject;
					}else{
						fail();
						throw new Exception("Wrong Parameter Types");
					}
				}
			}
		}
		return params;
	} 

	private static Object getMock(Class types, Object params) throws Exception{
			if(params!=null){
				Class paramClass=params.getClass();
				if(PrimitiveChecker.isPrimitiveOrWrapper(paramClass))
					paramClass= PrimitiveChecker.unWrapping(paramClass);
				if(types.equals(String.class) && paramClass.equals(String.class)){
					Object mockObject= mock.get(params);
					if(mockObject !=null)
						params=mockObject;
				}
				if(!types.equals(paramClass)){ // ���� ó�� �Ŀ��� Ÿ���� ���� ���� ���. 1. primitive Ÿ�԰� wrapper Ÿ���� ����.	
					Object mockObject=mock.get(params);
					if( types.isInstance(mockObject) && mockObject!=null){
						logger.testLog("The Mock named " + params + " is set. ( " + mockObject + " )");
						params=mockObject;
					}else{
						fail();
						throw new Exception("Wrong Parameter Types");
					}
				}
			}
			return params;
	} 
	
	private void constructor_test(){
		try{
			logger.testLog("Constructor Test (Test Method doesn't exist)");
			constructor.setAccessible(true);
			if(constructor_params.length==0)
				assertNotNull(constructor.newInstance());
			else{
				//Ÿ���� �ȸ����� mock ��ü �����ð�.
				Class[] paramTypes=constructor.getParameterTypes();
				Object[] params= getMock(paramTypes,constructor_params);
				assertNotNull(constructor.newInstance(params));
			}
		}catch(AssertionError e){
			success[sheetNum][rowIndex-1]=false;
			throw(e);
		}
		catch(Exception e){handleException(e);}
	}


	private void assertion(Object testResult, Object expectedResult, Class resultType) throws AssertionError, Exception{
		if(testResult ==null || expectedResult ==null){
			logger.testLog("Test Result/ Expect Result : "+ testResult + " / " + expectedResult);
			assertThat(testResult, is(expectedResult));
		}

		else if(PrimitiveChecker.isPrimitiveOrWrapper(resultType)){ //���ð� �׽�Ʈ
			switch(PrimitiveChecker.getFloatingType(resultType)){
			case 1:
				Double result= new Double(Float.toString((float)testResult));
				Double expect= new Double(Float.toString((float)expectedResult));
				logger.testLog("FloatType Test Result/ Expect Result : "+ result + " / " + expect);
				assertThat(result,
						is(closeTo(expect, 0.00001)));
				break;
			case 0:
				logger.testLog("DoubleType Test Result/ Expect Result : "+ testResult + " / " + expectedResult);
				assertThat((double)testResult,is(closeTo((double)expectedResult, 0.00001)));
				break;
			default:
				logger.testLog("Test Result/ Expect Result : "+ testResult + " / " + expectedResult);
				assertThat(testResult,is(expectedResult));
			}


		}else if(PrimitiveChecker.isClassCollection(resultType)){//Collection�ΰ��
			logger.testLog(resultType+ " Result Asserting... ");
			if(List.class.isAssignableFrom(resultType)){
				List listResult= (List) testResult, listExpect= (List) expectedResult;
				if(listResult.size() ==0 && listExpect.size() ==0){
					logger.testLog("Test Result/ Expect Result : Empty List");
					assertThat(listResult.isEmpty(),is(listExpect.isEmpty()));
				}else{
					Iterator resultIt=listResult.iterator(), expectIt= listExpect.iterator();
					while(resultIt.hasNext() && expectIt.hasNext()){
						Object r=resultIt.next() , e=expectIt.next();
						if(r.getClass().equals(e.getClass())){
							logger.testLog("Test Result/ Expect Result : "+ r + " / " + e);
							assertion(r, e, e.getClass());	
						}else{
							e= getMock(r.getClass(),e);
							assertion(r, e, e.getClass());	
						}
							
					}
				}
			}
			else if(Set.class.isAssignableFrom(resultType)){

			}
			else if(Map.class.isAssignableFrom(resultType)){

			}

		}else{//����� ���ð�ü�� �ƴ� ���� ��ü�ΰ��
			logger.testLog(resultType+ " Result Asserting... ");
			Field[] flz =resultType.getDeclaredFields();
			if(flz!=null)
				for(Field f: flz){
					if (!f.isSynthetic()){
						f.setAccessible(true);
						Class memberclz=f.getType();
						try {
							auto_Assert(testResult,expectedResult, f, memberclz);
						} catch (IllegalArgumentException | IllegalAccessException e) {handleException(e);}
					}
				}
		}
	}

	/**********************************************************************
	 * �̸�         : auto_Assert
	 * �Ķ����   : 
	 * 			Object testresult	: �׽�Ʈ �޼ҵ带 ������ �� ���� ���� ��ü. ����� ���ǰ�ü�̰ų�,  
	 * 			Field  f			: testresult�� �ɹ� ������ �̸�
	 * 			Class  memeberclz	: testresult�� �ɹ� ������ Ÿ�� 
	 * ����         : �������� �����ش�. PrimitiveType�� �ƴѰ�� ��ü ������ �ʵ尪�� ������ ������ �����ش�.
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 ********************************************************************** */
	private void auto_Assert(Object testresult,Object expectedResult, Field f,Class memberclz ) throws IllegalArgumentException, IllegalAccessException, AssertionError, Exception{
		Object resultField= f.get(testresult), expectField= f.get(expectedResult);
		
		if(resultField==null && expectField==null){
			logger.testLog("Field Test Result/ Expect Result : "+ resultField + " / " + expectField);
			assertThat(resultField,is(expectField));
		}
		else if(PrimitiveChecker.isPrimitiveOrWrapper(memberclz) ){ //�Ϲ� ���Һ�
			assertion(resultField,expectField,memberclz);
		}
		else if(memberclz.isArray()){//�迭���� ��
			if(Array.getLength(resultField) == Array.getLength(expectField)){
				for(int i= 0; i<Array.getLength(resultField); i++){
					if(Array.get(resultField, i)!=null &&Array.get(expectField, i)!=null){
						Object re =Array.get(resultField, i);
						Object ex = Array.get(expectField, i);
						logger.testLog("Array Field Test Result/ Expect Result : "+ re + " / " + ex);
						assertion(re,ex,f.getType());
					}
				}
			}
			else 
				fail("Array Size doesn't match");

		}else if(PrimitiveChecker.isCollection(expectField)){//�÷��� ���� ��
			Collection expect=(Collection) expectField;
			Collection result=(Collection) resultField;
			Iterator ex_it = expect.iterator();
			Iterator re_it = result.iterator();
			while(ex_it.hasNext() && re_it.hasNext()){
				Object ex=ex_it.next();
				Object re=re_it.next();
				logger.testLog("Collection Item Test Result/ Expect Result : "+ re + " / " + ex);
				assertThat(re, is(ex));
			}
		}else 
			fail("There's Custom Object Field.");
		//��� �ʿ�.
	}	

	/* �̽� 
	 *  ��ũ��ü�ε� ��ũ��ü�� primitive Ÿ���ΰ�� ? isMock �÷��׸� �δ°� �����Ű�����
	 * */
	@Test
	public void testMethod() throws Throwable {

		if(targetmethod==null){ //������ �׽�Ʈ�� ���.
			constructor_test();
			return;
		}

		Object testResult=null;

		if(targetmethod!=null)
			targetmethod.setAccessible(true);//private �޼ҵ带 �׽�Ʈ�ϱ� ����

		//Method param ��ũ��ü ����.
		Class[] paramsTypes= targetmethod.getParameterTypes();
		Object[] params= getMock(paramsTypes, method_params);
		try {			

			testResult=targetmethod.invoke(classmap.get(targetclz), params);
			logger.testLog("Test Method "+ targetmethod + " is invoked Successfully.");

			if(targetmethod.getReturnType()==null ||targetmethod.getReturnType().equals(void.class));
			else{
				Class[] type=null; Object[] returnObj=null;
				type= new Class[1];
				type[0]=targetmethod.getReturnType();
				if(testResult !=null){
					result[sheetNum][rowIndex-1]=testResult.toString();
				}else result[sheetNum][rowIndex-1] = "null";

				if(expectedResult !=null){
					returnObj=new Object[1];
					returnObj[0]=expectedResult;
					if(type!=null)
						if(!type[0].equals(expectedResult.getClass())){//������ mock��ü�ΰ��.
							returnObj=getMock(type,returnObj);
							expectedResult=returnObj[0];
						}
				}
				//����
				assertion(testResult,expectedResult,type[0]);
			}

		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			success[sheetNum][rowIndex-1]=false;
			if(exceptionlist.contains(e.getClass())){
				result[sheetNum][rowIndex-1]="Method Exception Occurred";
				logger.testFatal(result[sheetNum][rowIndex-1]);
				StackTraceElement[] elem =new StackTraceElement[1];			
				elem[0]=new StackTraceElement(targetclz.getName(), targetmethod.getName(), targetclz.getCanonicalName(),1);
				e.setStackTrace(elem);
				throw(e);
			}
			else {
				result[sheetNum][rowIndex-1]="InitError : Check Cell's data or Custom Exception";
				logger.testFatal(result[sheetNum][rowIndex-1]);
				Throwable fillstack=e.fillInStackTrace();
				Throwable cause=null;
				if(fillstack !=null){
					cause= fillstack.getCause(); 
					if(cause!=null) cause.printStackTrace();
					fail();
					throw(cause);
				}//Method Exception.
			}
		}catch(AssertionError e){
			success[sheetNum][rowIndex-1]=false;
			StackTraceElement[] elem =new StackTraceElement[1];			
			elem[0]=new StackTraceElement(targetclz.getName(), targetmethod.getName(), targetclz.getCanonicalName(),1);
			e.setStackTrace(elem);
			throw(e);
		}catch(Exception e){
			e.printStackTrace();
			fail();
		}
	}
	@After
	public void checkEndSuite(){
		logger.testLog("["+testname+"] Test is Finished\n");

		if(success[sheetNum].length==testnumber)
			logger.testLog('['+currentSheet +"] Suite is Finished");
	}
	//�������� �� ��� ����.
	@AfterClass
	public static void saveResult(){
		try {
			// ./logs/suite.log , testing.log , excel.log
			Set<String> sheetNames=sheets.keySet();
			Iterator sit= sheetNames.iterator();
			ExcelResultSaver save=new ExcelResultSaver(jexcelPath,logPath);
			for(int i=0; i<=sheetNum&&sit.hasNext(); i++){
				String sheetname=(String)sit.next();
				save.writeResults(sheetname, rowSize[i], result[i], success[i]);
				save.writeTestLog(sheetname, rowSize[i]);
			}

			save.write();
			save.close();
			logger.stop();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}