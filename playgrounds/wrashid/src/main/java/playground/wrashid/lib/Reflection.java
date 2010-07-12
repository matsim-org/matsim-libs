package playground.wrashid.lib;

import java.lang.reflect.Field;

import org.matsim.core.population.ActivityImpl;

public class Reflection {

	
	/**
	 * Set the value of a field within an object.
	 * @param targetObject
	 * @param fieldName
	 * @param newValue
	 */
	public static void setField(Object targetObject, String fieldName, Object newValue){
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
	
}
