package playground.wrashid.lib;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.Assert;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.ActivityImpl;

public class Reflection {

	/**
	 * Set the value of a field within an object.
	 * 
	 * @param targetObject
	 * @param fieldName
	 * @param newValue
	 */
	public static void setField(Object targetObject, String fieldName, Object newValue) {
		Class<?> c = targetObject.getClass();
		try {
			Field f = c.getDeclaredField(fieldName);
			f.setAccessible(true);
			f.set(targetObject, newValue);
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static Object callMethod(Object targetObject, String methodName, Object[] params) {
		final Method methods[] = targetObject.getClass().getDeclaredMethods();
		for (int i = 0; i < methods.length; ++i) {
			if (methodName.equals(methods[i].getName())) {
				try {
					methods[i].setAccessible(true);
					return methods[i].invoke(targetObject, params);
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			}
		}

		return null;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Reflection r=new Reflection();
		
		callMethod(r, "abc", null);
	}

	public void abc() {
		System.out.println("abc");
	}

}
