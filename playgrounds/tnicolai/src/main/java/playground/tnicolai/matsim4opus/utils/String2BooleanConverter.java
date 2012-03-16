package playground.tnicolai.matsim4opus.utils;

public class String2BooleanConverter {
	
	public static boolean getBoolean(final String value){
		
		if(value != null & value.equalsIgnoreCase("true"))
			return true;
		return false;
	}
}
