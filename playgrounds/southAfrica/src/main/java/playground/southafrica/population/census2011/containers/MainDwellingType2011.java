package playground.southafrica.population.census2011.containers;

import org.apache.log4j.Logger;

/**
 * The main dwelling types as described in the Census 2011 questionnaire,
 * question <b>H02_DWELLINGMAIN</b.
 *
 * @author jwjoubert
 */
public enum MainDwellingType2011 {
	FormalHouse, TraditionalDwelling, Apartment, Cluster,
	Townhouse, SemiDetachedHouse, BackyardFormal, BackyardInformal, 
	Informal, CaravanTent, Other, Unknown, NotApplicable;
		
		private final static Logger LOG = Logger.getLogger(MainDwellingType2011.class);
	
	/**
	 * Method accepting the given code as per Question <b>H02_DWELLINGMAIN</b> 
	 * in the census questionnaire.
	 *  
	 * @param code formal value as per final code list in census questionnaire
	 * @return a descriptive type enum.
	 */
	public static  MainDwellingType2011 parseTypeFromCensusCode(String code){
		if(code.contains(".")){
			return NotApplicable;
		}
		code = code.replaceAll(" ", "");
		int codeInt = Integer.parseInt(code);
		
		switch(codeInt){
		case 1:
			return FormalHouse;
		case 2:
			return TraditionalDwelling;
		case 3:
			return Apartment;
		case 4:
			return Cluster;
		case 5:
			return Townhouse;
		case 6:
			return SemiDetachedHouse;
		case 7:
			return BackyardFormal;
		case 8:
			return BackyardInformal;
		case 9:
			return Informal;
		case 10: 
			return BackyardFormal;
		case 11:
			return CaravanTent;
		case 12:
			return Other;
		}
		return Unknown;
	}
	
	
	/**
	 * Method to return the main dwelling type by parsing it from a string
	 * version of the same type. It is assumed the given String were originally
	 * created through the <code>toString()</code> method. If not, and
	 * the input string is the code from the census questionnaire, rather use
	 * the {@link MainDwellingType2011#parseTypeFromCensusCode(String)} method.
	 * 
	 * @param type
	 * @return
	 */
	public static  MainDwellingType2011 parseTypeFromString(String type){
		if(type.equalsIgnoreCase("FormalHouse")){
			return FormalHouse;
		} else if(type.equalsIgnoreCase("TraditionalDwelling")){
			return TraditionalDwelling;
		} else if(type.equalsIgnoreCase("Apartment")){
			return Apartment;
		} else if(type.equalsIgnoreCase("Cluster")){
			return Cluster;
		} else if(type.equalsIgnoreCase("Townhouse")){
			return Townhouse;
		} else if(type.equalsIgnoreCase("SemiDetachedHouse")){
			return SemiDetachedHouse;
		} else if(type.equalsIgnoreCase("BackyardFormal")){
			return BackyardFormal;
		} else if(type.equalsIgnoreCase("BackyardInformal")){
			return BackyardInformal;
		} else if(type.equalsIgnoreCase("Informal")){
			return Informal;
		} else if(type.equalsIgnoreCase("CaravanTent")){
			return CaravanTent;
		} else if(type.equalsIgnoreCase("Other")){
			return Other;
		} else if(type.equalsIgnoreCase("Unknown")){
			return Unknown;
		} else if(type.equalsIgnoreCase("NotApplicable")){
			return NotApplicable;
		} else{
			LOG.error("Unknown maindwelling type: " + type + "!! Returning 'Unknown'");
			return Unknown;
		}
	}
	
	
	/**
	 * Method to return an integer code from a main dwelling type. The integer
	 * codes corresponds to the codes used in the census questionnaire, with
	 * a few exceptions: the codes for 'Unspecified' and 'Not applicable'.
	 * 
	 * @param type
	 * @return
	 */
	public static int getCode( MainDwellingType2011 type){
		switch (type) {
		case FormalHouse:
			return 1;
		case TraditionalDwelling:
			return 2;
		case Apartment:
			return 3;
		case Cluster:
			return 4;
		case Townhouse:
			return 5;
		case SemiDetachedHouse:
			return 6;
		case BackyardFormal:
			return 7;
		case BackyardInformal:
			return 8;
		case Informal:
			return 9;
		case CaravanTent:
			return 11;
		case Other:
			return 12;
		case Unknown:
			return 13;
		case NotApplicable:
			return 14;
		}
		LOG.error("Unknown type: " + type.toString() + "!! Returning code for 'Unknown'");
		return 13;
	}
	
	
	/**
	 * Method to return an integer code from a main dwelling type description. 
	 * The integer codes corresponds to the codes used in the census questionnaire, 
	 * with a few exceptions: the codes for 'Unspecified' and 'Not applicable'.
	 * 
	 * @param s the String description of the main dwelling type.
	 * @return
	 */
	public static int getCode(String s){
		return getCode(parseTypeFromString(s));
	}
	
	
	/**
	 * Method to return the main dwelling type from a given code. The given
	 * integer code is the one used internally by this class. It corresponds 
	 * with the original census questionnaire codes, but there are differences.
	 * If you want to parse the dwelling type from the actual census code,
	 * rather use the method {@link MainDwellingType2011#parseTypeFromCensusCode(String)}.
	 * 
	 * @param code
	 * @return
	 */
	public static  MainDwellingType2011 getMainDwellingType(int code){
		switch (code) {
		case 1:
			return FormalHouse;
		case 2:
			return TraditionalDwelling;
		case 3:
			return Apartment;
		case 4:
			return Cluster;
		case 5:
			return Townhouse;
		case 6:
			return SemiDetachedHouse;
		case 7:
			return BackyardFormal;
		case 8:
			return BackyardInformal;
		case 9:
			return Informal;
		case 11:
			return CaravanTent;
		case 12:
			return Other;
		case 13:
			return Unknown;
		case 14:
			return NotApplicable;
		}
		LOG.error("Unknown type code: " + code + "!! Returning 'Unknown'");
		return Unknown;
	}
	
	
}
