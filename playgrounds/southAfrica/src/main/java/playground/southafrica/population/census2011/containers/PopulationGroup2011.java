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
 * The population groups as described in the Census 2011 questionnaire,
 * question <b>P05_POP_GROUP</b>.
 *
 * @author jwjoubert
 */
public enum PopulationGroup2011 {
	BlackAfrican, Coloured, IndianAsian, White, Other, NotApplicable;
	
	private final static Logger LOG = Logger.getLogger(PopulationGroup2011.class);
	
	/**
	 * Method accepting the given code as per Question <b>P05_POP_GROUP</b> 
	 * in the census questionnaire.
	 *  
	 * @param code formal value as per final code list in census questionnaire
	 * @return a descriptive type enum.
	 */
	public static PopulationGroup2011 parseTypeFromCensusCode(String code){
		if(code.contains(".")){
			return NotApplicable;
		}
		code = code.replaceAll(" ", "");
		int codeInt = Integer.parseInt(code);
		
		switch (codeInt) {
		case 1:
			return BlackAfrican;
		case 2:
			return Coloured;
		case 3:
			return IndianAsian;
		case 4:
			return White;
		case 5:
			return Other;
		}
		LOG.error("Unknown census code: " + code + "!! Returning 'Other'");
		return Other;
	}
	
	
	/**
	 * Method to return the population group by parsing it from a string version 
	 * of the same  It is assumed the given String were originally created 
	 * through the <code>toString()</code> method. If not, and the input 
	 * string is the code from the census questionnaire, rather use
	 * the {@link PopulationGroup2011#parseTypeFromCensusCode(String)} method.
	 * 
	 * @param type
	 * @return
	 */
	public static PopulationGroup2011 parseTypeFromString(String type){
		return PopulationGroup2011.valueOf(type);
	}
	
	
	/**
	 * Method to return an integer code for a population group. The integer 
	 * codes corresponds to the codes used in the census questionnaire, with
	 * a few exceptions: the code for 'Not applicable'.
	 * 
	 * @param type
	 * @return
	 */
	public static int getCode(PopulationGroup2011 type){
		switch (type) {
		case BlackAfrican:
			return 1;
		case Coloured:
			return 2;
		case IndianAsian:
			return 3;
		case White:
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
	 * Method to return an integer code for a housing  The integer codes 
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
	 * Method to return the population group from a given code. The given
	 * integer code is the one used internally by this class. It corresponds 
	 * with the original census questionnaire codes, but there are differences.
	 * If you want to parse the population group from the actual census code,
	 * rather use the method {@link PopulationGroup2011#parseTypeFromCensusCode(String)}.
	 * 
	 * @param code
	 * @return
	 */
	public static PopulationGroup2011 getType(int code){
		switch (code) {
		case 1:
			return BlackAfrican;
		case 2:
			return Coloured;
		case 3:
			return IndianAsian;
		case 4: 
			return White;
		case 5:
			return Other;
		case 6:
			return NotApplicable;
		}
		LOG.error("Unknown type code: " + code + "!! Returning 'NotApplicable'");
		return NotApplicable;
	}
	
}


