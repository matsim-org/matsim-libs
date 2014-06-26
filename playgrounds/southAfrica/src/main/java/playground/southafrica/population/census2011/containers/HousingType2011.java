/* *********************************************************************** *
 * project: org.matsim.*
 * LivingQuarterjava
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.southafrica.population.census2011.containers;

import org.apache.log4j.Logger;


/**
 * The housing types as described in the Census 2011 questionnaire,
 * question <b>H01_QUARTERS</b>.
 *
 * @author jwjoubert
 */
public enum HousingType2011 {
	House, Hostel, Hotel, OldAgeHome, Other, NotApplicable;
	
	private final static Logger LOG = Logger.getLogger(HousingType2011.class);
	
	/**
	 * Method accepting the given code as per Question <b>H01_QUARTERS</b> 
	 * in the census questionnaire.
	 *  
	 * @param code formal value as per final code list in census questionnaire
	 * @return a descriptive type enum.
	 */
	public static HousingType2011 parseTypeFromCensusCode(String code){
		if(code.contains(".")){
			return NotApplicable;
		}
		code = code.replaceAll(" ", "");
		int codeInt = Integer.parseInt(code);
		
		switch (codeInt) {
		case 1:
			return House;
		case 2:
			return Hostel;
		case 3:
			return Hotel;
		case 4:
			return OldAgeHome;
		case 5:
			return Other;
		}
		LOG.error("Unknown census code: " + code + "!! Returning 'Other'");
		return Other;
	}
	
	
	/**
	 * Method to return the housing type by parsing it from a string version 
	 * of the same  It is assumed the given String were originally created 
	 * through the <code>toString()</code> method. If not, and the input 
	 * string is the code from the census questionnaire, rather use
	 * the {@link HousingHousingType20112011#parseHousingType2011FromCensusCode(String)} method.
	 * 
	 * @param type
	 * @return
	 */
	public static HousingType2011 parseTypeFromString(String type){
		return HousingType2011.valueOf(type);
	}
	
	
	/**
	 * Method to return an integer code from a housing  The integer codes 
	 * corresponds to the codes used in the census questionnaire, with
	 * a few exceptions: the code for 'Not applicable'.
	 * 
	 * @param type
	 * @return
	 */
	public static int getCode(HousingType2011 type){
		switch (type) {
		case House:
			return 1;
		case Hostel:
			return 2;
		case Hotel:
			return 3;
		case OldAgeHome:
			return 4;
		case Other:
			return 5;
		case NotApplicable:
			return 6;
		}
		LOG.error("Unknown type: " + type.toString() + "!! Returning code for 'Other'");
		return 5;
	}
	

	/**
	 * Method to return an integer code from a housing  The integer codes 
	 * corresponds to the codes used in the census questionnaire, with
	 * a few exceptions: the code for 'Not applicable'.
	 * 
	 * @param s
	 * @return
	 */
	public static int getCode(String s){
		return getCode(parseTypeFromString(s));
	}

	
	/**
	 * Method to return the housing type from a given code. The given integer 
	 * code is the one used internally by this class. It corresponds with the 
	 * original census questionnaire codes, but there are differences. If you 
	 * want to parse the dwelling type from the actual census code, rather use 
	 * the method {@link MainDwellingHousingType20112011#parseHousingType2011FromCensusCode(String)}.
	 * 
	 * @param code
	 * @return
	 */
	public static HousingType2011 getHousingType(int code){
		switch (code) {
		case 1:
			return House;
		case 2:
			return Hostel;
		case 3:
			return Hotel;
		case 4: 
			return OldAgeHome;
		case 5:
			return Other;
		case 6:
			return NotApplicable;
		}
		LOG.error("Unknown type code: " + code + "!! Returning 'NotApplicable'");
		return NotApplicable;
	}
	
}


