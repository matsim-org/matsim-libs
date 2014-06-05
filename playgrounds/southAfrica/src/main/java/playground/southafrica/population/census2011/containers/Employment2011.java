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
 * The employment status as described in the Census 2011 questionnaire,
 * question <b>P23_EMPLOYMENTSTATUS</b>. The official definition is used, and
 * is therefore a derived variable, namely <b>DERP_EMPLOY_STATUS_OFFICIAL</b>
 *
 * @author jwjoubert
 */
public enum Employment2011 {
	Employed, Unemployed, Discouraged, Inactive, NotApplicable;
	
	private final static Logger LOG = Logger.getLogger(Employment2011.class);
	
	/**
	 * Method accepting the given code as per Question <b>P23_EMPLOYMENTSTATUS</b> 
	 * in the census questionnaire. Note, the code of the derived, official, 
	 * variable <b>DERP_EMPLOY_STATUS_OFFICIAL</b> is used.
	 *  
	 * @param employment formal value as per final code list in census questionnaire
	 * @return a descriptive type enum.
	 */
	public static Employment2011 parseEmploymentFromCensusCode(String employment){
		if(employment.contains(".")){
			return NotApplicable;
		}
		employment = employment.replaceAll(" ", "");
		int codeInt = Integer.parseInt(employment);
		
		switch (codeInt) {
		case 1:
			return Employed;
		case 2:
			return Unemployed;
		case 3:
			return Discouraged;
		case 4:
			return Inactive;
		}
		LOG.error("Unknown census code: " + employment + "!! Returning 'Inactive'");
		return Inactive;
	}
	
	
	/**
	 * Method to return the employment status by parsing it from a string 
	 * version of the same  It is assumed the given String were originally 
	 * created through the <code>toString()</code> method. If not, and the 
	 * input string is the code from the census questionnaire, rather use
	 * the {@link Employment2011#parseEmploymentFromCensusCode(String)} method.
	 * 
	 * @param employment
	 * @return
	 */
	public static Employment2011 parseEmploymentFromString(String employment){
		return Employment2011.valueOf(employment);
	}
	
	
	/**
	 * Method to return an integer code for t employment status. The integer 
	 * codes corresponds to the codes used in the census questionnaire, with
	 * a few exceptions: the code for 'Not applicable'.
	 * 
	 * @param employment
	 * @return
	 */
	public static int getCode(Employment2011 employment){
		switch (employment) {
		case Employed:
			return 1;
		case Unemployed:
			return 2;
		case Discouraged:
			return 3;
		case Inactive:
			return 4;
		case NotApplicable:
			return 5;
		}
		LOG.error("Unknown type: " + employment.toString() + "!! Returning code for 'Inactive'");
		return 4;
	}
	

	/**
	 * Method to return an integer code for the employment status. The integer 
	 * codes corresponds to the codes used in the census questionnaire, with
	 * a few exceptions: the code for 'Not applicable'.
	 * 
	 * @param s
	 * @return
	 */
	public static int getCode(String s){
		return getCode(parseEmploymentFromString(s));
	}

	
	/**
	 * Method to return the population group from a given code. The given
	 * integer code is the one used internally by this class. It corresponds 
	 * with the original census questionnaire codes, but there are differences.
	 * If you want to parse the population group from the actual census code,
	 * rather use the method {@link Employment2011#parseEmploymentFromCensusCode(String)}.
	 * 
	 * @param code
	 * @return
	 */
	public static Employment2011 getEmployment(int code){
		switch (code) {
		case 1:
			return Employed;
		case 2:
			return Unemployed;
		case 3:
			return Discouraged;
		case 4: 
			return Inactive;
		case 5:
			return NotApplicable;
		}
		LOG.error("Unknown type code: " + code + "!! Returning 'NotApplicable'");
		return NotApplicable;
	}
	
}


