package jexcelunit.excel;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import jexcelunit.classmodule.PrimitiveChecker;

/*
 * Created : 2017.02.25
 * Vendor : Taehoon Seo
 * Description : Read Excel Sheet and Convert to Testcases set.
 * 
 * */
@SuppressWarnings("rawtypes")
public class ExcelReader {
	public final Attribute[] TESTDATASET = Attribute.values();

	private HashMap<String,String> classFullNames = new HashMap<>();
	private DataFormatter formatter=new DataFormatter();
	private FileInputStream inputstream=null;
	private ArrayList<String> testModes= new ArrayList<String>();
	private FormulaEvaluator formularEvaluator;
	private XSSFWorkbook workbook= null;
	public ExcelReader(String filePath) throws IOException{
		this.workbook = getWorkbook(filePath);
	}

	public XSSFWorkbook getWorkbook(String filePath) throws IOException{
		//open Excel File.
		File file = new File(filePath);
		inputstream = new FileInputStream(file);
		return new XSSFWorkbook(inputstream);
	}

	private void setClassMap(XSSFSheet clzmetSheet){
		XSSFRow clzNames= clzmetSheet.getRow(0);
		Iterator<Cell> it= clzNames.cellIterator();
		Cell cell=null;
		while(it.hasNext()){
			cell=it.next();
			String fullString = formatter.formatCellValue(cell);
			String key= fullString.substring(fullString.lastIndexOf(".")+1, fullString.length());
			classFullNames.put(key, fullString);
		}
		classFullNames.put("String", String.class.getName());
		classFullNames.put("Integer", Integer.class.getName());
		classFullNames.put("Short", Short.class.getName());
		classFullNames.put("Float", Float.class.getName());
		classFullNames.put("Double", Double.class.getName());
		classFullNames.put("Date", Date.class.getName());
		classFullNames.put("Long", Long.class.getName());
		classFullNames.put("Number", Number.class.getName());
		classFullNames.put("BigInteger", BigInteger.class.getName());

	}

	public ArrayList<String> getTestSheetMode(){
		return testModes;
	}

	private XSSFSheet getMockSheet(){
		for(int sheet_index=0;sheet_index<workbook.getNumberOfSheets();sheet_index++){
			XSSFSheet sheet= workbook.getSheetAt(sheet_index);
			if(sheet!=null) {
				if(sheet.getSheetName().toLowerCase().contains("mock")) {
					return sheet;
				}
			}

		}
		return null;
	}

	public ArrayList<MockVO> readMocks() throws Exception{
		XSSFSheet mockSheet =getMockSheet();
		if(mockSheet == null) return null;
		XSSFRow row = null;
		XSSFCell cell = null;

		int colIndex =0, maxRow =2 ,maxConsParam = 0, maxField = 0;
		row = mockSheet.getRow(maxRow++);
		cell = row.getCell(colIndex);
		String cellString = cell.getStringCellValue();

		while(!CheckingUtil.isNullOrEmpty(cellString)){
			//Count ConsParam.
			if(cellString.toLowerCase().contains("consparam")){
				maxConsParam++;
			}
			//Count Field
			if(cellString.toLowerCase().contains("field")){
				maxField++;
			}
			row = mockSheet.getRow(maxRow++);
			if(row !=null){
				cell = row.getCell(colIndex);
				cellString= formatter.formatCellValue(cell);	
			}else cellString= null;
		}
		colIndex++;

		//Read Mock
		row = mockSheet.getRow(2); //mock Name
		if(row !=null){
			cell = row.getCell(colIndex);
			cellString= formatter.formatCellValue(cell);	
		}
		//mock �̸��� ������ ���� ��ȯ.
		ArrayList<MockVO> mockList= new ArrayList<MockVO>();
		MockVO mock=null;
		while(!CheckingUtil.isNullOrEmpty(cellString)){ //Mock Name �� ������  ���� ����.
			int currentRow = 2;
			row = mockSheet.getRow(currentRow++);
			if(row !=null){
				cell = row.getCell(colIndex);
				if(cell ==null) break;
				cellString= formatter.formatCellValue(cell);
				if (cellString == null) break;
			}

			mock= new MockVO();
			//1. mockName
			String mockName = cellString;

			//2. Constructor
			row=mockSheet.getRow(currentRow++);
			if(row ==null) break;

			cell = row.getCell(colIndex);
			String fullcons= formatter.formatCellValue(cell);//������ ����.
			if(!CheckingUtil.isNullOrEmpty(fullcons)){
				String clzname =fullcons.substring(0, fullcons.indexOf('(')); //Class name�� �и�
				String classFullname= classFullNames.get(clzname);//��Ű���̸��� ������ Ŭ������.
				try {
					Class mockClass= Class.forName(classFullname);
					Constructor con =findStringToConstructor(fullcons, mockClass);

					if(con ==null) throw new Exception("Can't not Found The Constructor of Mock Class");

					//3. Constructor Parameters
					Class[] paramTypes = con.getParameterTypes();

					row= mockSheet.getRow(currentRow); //First Param
					cell= row.getCell(colIndex);
					String paramString=null;
					paramString= formatter.formatCellValue(cell);

					if(paramTypes ==null && paramString !=null)
						throw new Exception("Detected Wrong Constructor Parameter.\n at "+ mockSheet.getSheetName() + " Row :" + (currentRow) +" Col :" + colIndex+1);
					//Constructor Parameter�� ����
					ArrayList<Object> params= new ArrayList<Object>();
					int offset= currentRow;
					for(int paramIndex =offset; paramIndex<offset+paramTypes.length; paramIndex++){
						row= mockSheet.getRow(currentRow++);
						cell= row.getCell(colIndex);
						if(cell ==null) break;
						paramString= formatter.formatCellValue(cell);
						Object param =null;
						if(!CheckingUtil.isNullOrEmpty(paramString)) 
							param=PrimitiveChecker.convertObject(paramTypes[paramIndex-offset], paramString);	
						params.add(param);
						//						if(paramString=="" || paramString ==null){
						//							throw new Exception("Constructor Parameter is Missing.");
						//						}
					}
					currentRow = offset+maxConsParam; //Skip Empty Row.

					//4. field and fieldValue
					offset = currentRow;
					String fieldString= null ,fieldValue=null;
					Map<Field,Object> fieldSet = new HashMap<Field, Object>();// field - RealObject.

					for(int fieldIndex = offset; fieldIndex < offset+(maxField*2); fieldIndex++){	
						//Field Name
						row = mockSheet.getRow(currentRow++);
						cell = row.getCell(colIndex);
						if(cell ==null) fieldString = null;
						fieldString= formatter.formatCellValue(cell);

						//Field Value
						row = mockSheet.getRow(currentRow++);
						cell = row.getCell(colIndex);
						if(cell ==null) fieldValue= null;
						fieldValue= formatter.formatCellValue(cell);

						/* fieldName & fieldValue*/

						if(!CheckingUtil.isNullOrEmpty(fieldString)){
							String[] fieldName = fieldString.split(" ");
							Field[] fields = mockClass.getDeclaredFields();
							boolean found =false;
							for(Field targetField : fields){
								Class targetFieldClass= targetField.getType();
								String targetFieldName= targetField.getName();
								if(targetFieldClass.getName().contains(fieldName[0]) && targetFieldName.equals(fieldName[1]))
								{
									found =true;
									Object targetValue=null;
									Class targetType=targetField.getType();
									if(PrimitiveChecker.isClassCollection(targetField.getType())){
										String genericType= targetField.getGenericType().toString();
										if(genericType!=null){
											String componentStr= genericType.substring(genericType.indexOf("<")+1, genericType.indexOf(">"));
											Class componentType=Class.forName(componentStr);
											targetValue=PrimitiveChecker.ConvertToCollection(targetType, fieldValue,componentType);	
										}
									}
									if(targetValue==null)
										targetValue= PrimitiveChecker.convertObject(targetType, fieldValue);
									fieldSet.put(targetField, targetValue);
									break;
								}
							}
							if(!found) throw new Exception("Can't not found the field in Class of the Mock at " +mockSheet.getSheetName() + " Row : " + (currentRow-1)+ " Col : " + colIndex);

						}else if(!CheckingUtil.isNullOrEmpty(fieldValue) && CheckingUtil.isNullOrEmpty(fieldString))
							throw new Exception("Wrong Input in This Mock at " +mockSheet.getSheetName() + " Row : " + (currentRow-1)+ " Col : " + colIndex);
						else break;
					}
					mock.setMockName(mockName);
					mock.setMockClass(mockClass);
					mock.setConstructor(con);
					if(params.size()>0)
						mock.setConsParams(params);
					if(fieldSet.size()>0)
						mock.setFieldSet(fieldSet);
					mockList.add(mock);

				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (Exception e){
					e.printStackTrace();
				}
			}
			colIndex++;
		}//end Main While loop

		if(mockList.size() >0) {
			return mockList;
		}else return null;
	}

	/*
	 * Excel Reading Issue
	 * */
	public ArrayList<ArrayList<TestcaseVO>> readExcel() throws IOException{
		ArrayList<ArrayList<TestcaseVO>> caselists= new ArrayList<ArrayList<TestcaseVO>>();
		formularEvaluator = new XSSFFormulaEvaluator(workbook);
		if(workbook!=null){
			//Read ClassName. and main Tain Map<Simple Name,Full Name>
			XSSFSheet clzhidden= workbook.getSheet("ClassMethodhidden");
			if(clzhidden!=null){
				setClassMap(clzhidden);
			}

			//Read sheet
			for(int sheet_index=0;sheet_index<workbook.getNumberOfSheets();sheet_index++){


				//�׽�Ʈ ��Ʈ�� �о���̴� �κ�.
				if(!workbook.isSheetHidden(sheet_index) && !workbook.getSheetName(sheet_index).contains("Mock")){
					ArrayList<TestcaseVO> caselist=new ArrayList<TestcaseVO>();
					XSSFSheet xssfsheet = null;
					xssfsheet=workbook.getSheetAt(sheet_index);

					//Read First Line and Check Test Mode.
					XSSFRow firstRow = xssfsheet.getRow(0);
					XSSFCell testModeCell= firstRow.getCell(1);
					String testMode= formatter.formatCellValue(testModeCell);
					if(testMode.equals("") || testMode==null)
						testModes.add("Scenario");
					else
						testModes.add(testMode);

					//Read Second line and set vo info.
					XSSFRow secondRow = xssfsheet.getRow(1);
					XSSFCell infocell = null;
					int colSize= secondRow.getPhysicalNumberOfCells();
					int[] voOption = new int[colSize];
					for(int i=0; i < colSize; i++){
						infocell= secondRow.getCell(i);

						for(int j =0; j < TESTDATASET.length; j++){
							if(infocell != null)
								if(infocell.getRichStringCellValue().getString().contains(TESTDATASET[j].toString())){						
									//remember index

									voOption[i]=j;
									break;
								}	
						}
					}

					//loop to convert data to VO Object. Except First, Second Row.
					for(int i= 2; i<xssfsheet.getPhysicalNumberOfRows(); i++){
						XSSFRow currentRow= xssfsheet.getRow(i);
						if(currentRow.getCell(0)!=null){
						
						if(formatter.formatCellValue(currentRow.getCell(0)).length()==0)
							break;
						
						TestcaseVO vo= new TestcaseVO();
						vo.setSheetName(xssfsheet.getSheetName());
						//get Cell Values
						
							for(int j= 0 ; j<colSize; j ++){
								XSSFCell currentCell = currentRow.getCell(j);	
								if(currentCell !=null){
									//Set vo Values.
									setVOvalue(vo,TESTDATASET[voOption[j]],workbook,currentCell);
								}else{
									if(j<voOption.length-1)//�Ķ���Ͱ� �ΰ��ΰ�츦  ���.
										if(currentCell==null && voOption[j]==voOption[j+1]){
											currentRow.createCell(j);
											setVOvalue(vo,TESTDATASET[voOption[j]],workbook,currentCell);
										}
								}
							}
						 caselist.add(vo);
						}
						
					}
					caselists.add(caselist); //save sheets		

				}

			}
			// ArrayList<TestCaseVO> �� List���·� ����.
			//Invoker���� global�� Suite Index�� �ξ� Parameterize �����Լ��� �ٸ� �迭�� �����ϵ���.
			workbook.close();
		}
		if(inputstream !=null)
			inputstream.close();

		return caselists;
	} 

	private Constructor findStringToConstructor(String cellString,Class targetClass){
		//�Ķ���� �и�.
		String paramFullText =cellString.substring(cellString.indexOf('(')+1,cellString.indexOf(')'));
		String[] paramsText=paramFullText.equals("")?new String[]{}:paramFullText.split(",");

		Constructor[] cons =targetClass.getDeclaredConstructors();//�����ڸ� ã��.
		for(Constructor con : cons){
			boolean find=true;
			Class[] params = con.getParameterTypes();//�Ķ���ͺи�.
			if(params.length==paramsText.length){
				for(int index=0; index<paramsText.length; index++){
					String paramType = paramsText[index].split(" ")[0];
					if(!params[index].getSimpleName().equals(paramType))
					{
						find=false;//Wrong Constructor
						break;
					}	
				}	
			}else find=false;
			if(find){ //������ ã��.
				return con;
			}
		}
		return null;
	}


	//Set Vo values depend on first Row
	private void setVOvalue(TestcaseVO vo , Attribute OPTION, XSSFWorkbook workbook, XSSFCell currentCell){
		//change values to String


		switch(OPTION){
		case TestName: //�׽�Ʈ �̸�
			vo.setTestname(formatter.formatCellValue(currentCell));
			break;
		case TestClass: //Set Class and Set Constructor
			String fullcons= formatter.formatCellValue(currentCell);//���� ����.
			String clzname =fullcons.substring(0, fullcons.indexOf('(')); //Class name�� �и�
			String classFullname= classFullNames.get(clzname);//��Ű���̸��� ������ Ŭ������.

			try {
				Class testClass= Class.forName(classFullname);
				vo.setTestclass(testClass);
				Constructor con = findStringToConstructor(fullcons, testClass);
				vo.setConstructor(con);
				Class[] params = con.getParameterTypes();
				if(params!=null)
					if(params.length>0)vo.setCons_param(params);
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		case ConsParam:

			//Extract Parameter Type and convert Object or Mock Object;
			String con_paramString= formatter.formatCellValue(currentCell);//String value

			Class[] con_paramTypes= null;
			con_paramTypes= vo.getCons_param();
			try{
				if(con_paramTypes==null && con_paramString !=""){
					throw new Exception("Detected Wrong Constructor Parameter.\n at "+currentCell.getSheet().getSheetName()+"\n at "+(currentCell.getRowIndex()+1));
				}
				
				if(con_paramTypes!=null){
					int index= vo.getConstructorParams().size();
					if(index< con_paramTypes.length){
						Class con_targetType =con_paramTypes[index];
						Object conparam=PrimitiveChecker.convertObject(con_targetType,con_paramString);
						vo.addConstructorParam(conparam);
					}	
				}
				
			}catch(Exception e){
				e.printStackTrace();
			}
			break;
		case TestMethod://Set Method and Parameter Types
			String fullmet= formatter.formatCellValue(currentCell);
			String metName =fullmet.substring(fullmet.indexOf(' ')+1, fullmet.indexOf('(')); //Method name ����

			String metParamFullText =fullmet.substring(fullmet.indexOf('(')+1,fullmet.indexOf(')'));
			String[] metParamsText=metParamFullText.equals("")?new String[]{}:metParamFullText.split(",");

			Class metclass=vo.getTestclass();
			Method[] mets=null;
			mets=metclass.getDeclaredMethods();
			for(Method met : mets){
				boolean find=true;
				if(metName.equals(met.getName())){
					Class[] params =null;
					params= met.getParameterTypes();
					if(params==null && metParamsText.length==0){ find =true;}
					else if(params.length==metParamsText.length){
						for(int mp_index=0; mp_index<metParamsText.length; mp_index++){
							String paramType = metParamsText[mp_index].split(" ")[0];
							if(!params[mp_index].getSimpleName().equals(paramType))
							{
								find=false;//Wrong
								break;
							}	
						}	
					}else find=false;
					if(find){
						vo.setMet(met);
						if(params.length!=0&&params!=null)
							vo.setMet_param(params);
						break;
					}	
				}
			}

			break;
		case MetParam: //Extract parameter and convert Object
			String met_paramString= formatter.formatCellValue(currentCell);//String value
			Class[] met_paramTypes= null;
			met_paramTypes=vo.getMet_param();
			try{
				//�Ķ���Ͱ� ���µ� ���� �ִ°��
				if(met_paramTypes==null && met_paramString !=""){
					throw new Exception("Detected Wrong Method Parameter.\n at "+currentCell.getSheet().getSheetName()+"\n at "+(currentCell.getRowIndex()+1));
				}
				//�Ķ���Ͱ� �ִ°��.
				if(met_paramTypes !=null){
					int met_param_index= vo.getMethodParams().size();
					if(met_param_index < met_paramTypes.length){
						Class met_targetType=met_paramTypes[met_param_index]; //Current Param Type
						Object metparam=PrimitiveChecker.convertObject(met_targetType,met_paramString);
						vo.addMethodParam(metparam);	
					}
				}
			}catch(Exception e){
				e.printStackTrace();
			}
			break;
		case Expected:
			String expectString= formatter.formatCellValue(currentCell,formularEvaluator);//String value
			Class returnType = vo.getMet().getReturnType();			
			if(!returnType.equals(void.class) || returnType !=null){
				Object expect= PrimitiveChecker.convertObject(returnType, expectString);
				vo.setExpect(expect);	
			}
			break;
		case Result:
			break;
		case Success:
			break;
		}
	}



}