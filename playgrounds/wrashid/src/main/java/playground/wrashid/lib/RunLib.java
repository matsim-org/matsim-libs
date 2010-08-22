package playground.wrashid.lib;

public class RunLib {

	public static int getRunNumber(Object runObject){
		return getRunNumber(runObject.getClass());
	}
	
	public static int getRunNumber(Class c){
		String className=c.getSimpleName();
		int runNumber=new Integer(removeNonNumericChars(className));
		return runNumber;
	}
	
	public static String removeNonNumericChars(String str){
		int currentPosition=0;
		while(str.length()>currentPosition){
			char c=str.charAt(currentPosition);
			if (Character.isDigit(c)){
				currentPosition++;
			} else {
				str=str.substring(1);
			}
		}
		
		return str;
	}
	
}
