package jexcelunit.classmodule;

import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("rawtypes")
public class PrimitiveChecker {

	public static Class unWrapping(Class wrapper){
		//Numeric
		if(wrapper.equals(Short.class)) return short.class;
		else if(wrapper.equals(Long.class)) return long.class;
		else if(wrapper.equals(Integer.class)) return int.class;
		else if(wrapper.equals(Byte.class)) return byte.class;
		else if(wrapper.equals(Short.class)) return short.class;
		//Floating Type
		else if(wrapper.equals(Double.class)) return double.class;
		else if(wrapper.equals(Float.class)) return float.class;
		//BooleanType
		else if(wrapper.equals(Boolean.class)) return boolean.class;
		//etc
		else if(wrapper.equals(Void.class)) return void.class;
		else if(wrapper.equals(Character.class)) return char.class;


		else return wrapper;

	}

	//Check this type is Wrapper class about primitive type.
	public static boolean isWrapperClass(Class type){
		if(
				type.equals(Short.class) 
				|| type.equals(Double.class)
				|| type.equals(Long.class)
				|| type.equals(Byte.class)
				|| type.equals(Float.class)
				|| type.equals(Integer.class)
				|| type.equals(Boolean.class)
				|| type.equals(Character.class)
				|| type.equals(String.class)
				|| type.equals(StringBuffer.class)
				|| type.equals(Object.class)
				)
			return true;
		else if(type.getSuperclass() !=null){
			if(type.getSuperclass().equals(Number.class))
				return true;
			else return false;
		}
		else return false;
	}

	public static int getFloatingType(Class type){
		if(type.equals(float.class) || type.equals(Float.class))
			return 1;
		else if(type.equals(double.class)|| type.equals(Double.class))
			return 0;
		else 
			return -1;
	}


	@SuppressWarnings("unchecked")
	public static Object convertObject(Class targetType,String paramString){
		try {
			Object paramObject=null;

			//wrapper
			if(isWrapperClass(targetType))
			{
				paramObject=targetType.getConstructor(String.class).newInstance(paramString.trim());
				return paramObject;
			}	

			//primitive
			else if(targetType.equals(char.class))
				return paramString.toCharArray()[0];
			else if(targetType.equals(char[].class))
				return paramString.toCharArray();
			else if(targetType.equals(int.class))	return (int)Integer.parseInt(paramString);
			else if(targetType.equals(double.class))	return (double)Double.parseDouble(paramString);
			else if(targetType.equals(float.class))	return (float)Float.parseFloat(paramString);
			else if(targetType.equals(short.class))	return (short)Short.parseShort(paramString);
			else if(targetType.equals(Date.class))	
			{		
				try {
					return new SimpleDateFormat().parse(paramString);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} //need To parse
			}	
			else if(targetType.equals(boolean.class)) return (boolean)Boolean.parseBoolean(paramString);
			else if(targetType.equals(byte.class)) return (byte)Byte.parseByte(paramString);

			else if(targetType.isArray()) {
				//if value is array.
				String[] arrayItems = paramString.split(",");
				Object item = null;
				ArrayList list= new ArrayList();
				for(String itemStr : arrayItems) {
					itemStr= itemStr.trim();
					item = PrimitiveChecker.convertObject(targetType.getComponentType(), itemStr);
					list.add(item);
				}
				return list.toArray();
			}
			else if(isClassCollection(targetType)){
				//TODO
				//������Ʈ Ÿ���� �˾Ƴ�����..
				if(paramString.equals("emptyList")) return new ArrayList();
				else if(paramString.equals("emptySet")) return new HashSet();
				else if(paramString.equals("emptyMap")) return new HashMap();
				else {
					String[] arrayItems = paramString.split(",");
					Object item = null;
					ArrayList list= new ArrayList();
					for(String itemStr : arrayItems) {
						itemStr= itemStr.trim();
						item = itemStr;
						list.add(item);
					}
					return list;
				}
			}
			else if(paramString.toLowerCase().equals("null"))
				return null;
			else //mock;
				return paramString;	
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | NoSuchMethodException | SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	@SuppressWarnings("unchecked")
	public static Object ConvertToCollection(Class targetType, String paramString, Class componentType){
		try {
			if(List.class.isAssignableFrom(targetType)){
				
				List list = (List) targetType.newInstance();
				if(paramString.equals("emptyList")) return list;
				String[] arrayItems = paramString.split(",");
				Object item = null;
				for(String itemStr : arrayItems) {
					itemStr= itemStr.trim();
					item = PrimitiveChecker.convertObject(componentType, itemStr);
					list.add(item);
				}
				return list;
			}
			//2. Set
			else if(Set.class.isAssignableFrom(targetType)){
				Set set =(Set) targetType.newInstance();
				if(paramString.equals("emptySet")) return set;
				String[] arrayItems = paramString.split(",");
				Object item = null;
				for(String itemStr : arrayItems) {
					itemStr= itemStr.trim();
					item = PrimitiveChecker.convertObject(componentType, itemStr);
					set.add(item);
				}
				return set;
			}
			//3. Map
			else if(Map.class.isAssignableFrom(targetType)){
				Map map=(Map) targetType.newInstance();
				if(paramString.equals("emptyMap")) return map;
				//TODO
				return map;
			}
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				 | SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	/*
	 *  �ǵ�ġ ���� ClassInfo �ε��� ���ƾ��ϴ°��� Ű����Ʈ.
	 *  ����� ��� �����غ���.
	 * 
	 * 3. 
	 * */

	public static boolean isPrimitiveOrWrapper(Class type){
		boolean flag= false;
		if(type.isPrimitive())
			flag= true;
		else if(isWrapperClass(type))
			flag=true;
		return flag; 
	}

	public static boolean isUserClass(Class type){
		return !isClassCollection(type) && !isJavaType(type) &&!isPrimitiveOrWrapper(type) && !type.isArray();
	}

	public static boolean isClassCollection(Class c) {
		return Collection.class.isAssignableFrom(c) || Map.class.isAssignableFrom(c);
	}
	public static boolean isCollection(Object ob) {
		return ob instanceof Collection || ob instanceof Map;
	}
	public static boolean isJavaType(Class type){
		Package packageName = type.getPackage();
		if(packageName !=null){
			return packageName.getName().startsWith("java.");
		}
		else return false;
	}
}
