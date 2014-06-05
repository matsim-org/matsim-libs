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
 * The genders as described in the Census 2011 questionnaire, question 
 * <b>F03_SEX</b>.
 *
 * @author jwjoubert
 */
public enum Gender2011 {
	Male, Female, Unspecified;
	
	private final static Logger LOG = Logger.getLogger(Gender2011.class);
	
	/**
	 * Method accepting the given code as per Question <b>F03_SEX</b> 
	 * in the census questionnaire.
	 *  
	 * @param code formal value as per final code list in census questionnaire
	 * @return a descriptive type enum.
	 */
	public static Gender2011 parseGenderFromCensusCode(String code){
		int codeInt = Integer.parseInt(code);
		
		switch (codeInt) {
		case 1:
			return Male;
		case 2:
			return Female;
		case 9:
			return Unspecified;
		}
		LOG.error("Unknown census code: " + code + "!! Returning 'Unspecified'");
		return Unspecified;
	}
	
	
	/**
	 * Method to return the gender by parsing it from a string version of the 
	 * same  It is assumed the given String were originally created through 
	 * the <code>toString()</code> method. If not, and the input string is the 
	 * code from the census questionnaire, rather use the 
	 * {@link Gender2011#parseEmploymentFromCensusCode(String)} method.
	 * 
	 * @param type
	 * @return
	 */
	public static Gender2011 parseGenderFromString(String gender){
		return Gender2011.valueOf(gender);
	}
	
	
	/**
	 * Method to return an integer code for a gender. The integer codes 
	 * corresponds to the codes used in the census questionnaire.
	 * 
	 * @param type
	 * @return
	 */
	public static int getCode(Gender2011 gender){
		switch (gender) {
		case Male:
			return 1;
		case Female:
			return 2;
		case Unspecified:
			return 9;
		}
		LOG.error("Unknown type: " + gender.toString() + "!! Returning code for 'Unspecified'");
		return 9;
	}
	

	/**
	 * Method to return an integer code for a gender.  The integer codes 
	 * corresponds to the codes used in the census questionnaire.
	 * 
	 * @param s
	 * @return
	 */
	public static int getCode(String s){
		return getCode(parseGenderFromString(s));
	}

	
	/**
	 * Method to return the gender from a given code. The given integer code is 
	 * the one used internally by this class, and also corresponds with the 
	 * original census questionnaire codes. If you want to parse the population 
	 * group from the actual census code, rather use the method 
	 * {@link Gender2011#parseEmploymentFromCensusCode(String)}.
	 * 
	 * @param code
	 * @return
	 */
	public static Gender2011 getGender(int code){
		switch (code) {
		case 1:
			return Male;
		case 2:
			return Female;
		case 9:
			return Unspecified;
		}
		LOG.error("Unknown type code: " + code + "!! Returning 'Unspecified'");
		return Unspecified;
	}
	
	
	/**
	 * Currently MATSim only support "m" for male and "f" for female. To 
	 * accommodate this, this method converts the census description to a
	 * relevant MATSim description.
	 * 
	 * @param gender
	 * @return
	 */
	public static String getMatsimGender(Gender2011 gender){
		switch (gender) {
		case Male:
			return "m";
		case Female:
			return "f";
		case Unspecified:
			break;
		}
		return null;
	}
	
}


