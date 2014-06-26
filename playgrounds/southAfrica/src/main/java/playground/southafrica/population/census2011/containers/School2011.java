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
 * The level of schooling currently being attended as described in the Census 
 * 2011 questionnaire, using a combination of question <b>P17_SCHOOLATTEND</b>,
 * and <b>P18_EDUINST</b>.
 * 
 * Take care: only children five years and older were surveyed, so day-care
 * is not covered, at least by definition.
 *
 * @author jwjoubert
 */
public enum School2011 {
	None, PreSchool, School, Tertiary, AdultEducation, HomeSchooling, Unknown, 
	Unspecified, NotApplicable;
	
	private final static Logger LOG = Logger.getLogger(School2011.class);
	
	/**
	 * Method accepting the given codes as per question <b>P17_SCHOOLATTEND</b> 
	 * and <b>P18_EDUINST</b> in the census questionnaire.
	 *  
	 * @param code formal value as per final code list in census questionnaire
	 * @return a descriptive type enum.
	 */
	public static School2011 parseEducationFromCensusCode(String attendance, String institution){
		/* First deal with attendance. */
		if(attendance.contains(".")){
			return NotApplicable;
		}
		attendance = attendance.replaceAll(" ", "");
		int codeAttendance = Integer.parseInt(attendance);
		switch (codeAttendance) {
		case 2:
			return None;
		case 3:
			return Unknown;
		case 9:
			return Unspecified;
		}

		/* Next, deal with which institution. */
		if(institution.contains(".")){
			return NotApplicable;
		}
		institution = institution.replaceAll(" ", "");
		int codeInstitution = Integer.parseInt(institution);
		
		switch (codeInstitution) {
		case 1:
			return PreSchool;
		case 2: // Normal school
		case 3: // Special school
			return School;
		case 4: // FET College
		case 5: // College
		case 6: // University & University of Technology
			return Tertiary;
		case 7: // Adult basic education and traing
		case 8: // PLiteracy
			return AdultEducation;
		case 9:
			return HomeSchooling;
		case 99:
			return Unspecified;
		}
		LOG.error("Unknown census code for education and institution: (" + attendance 
				+ "; " + institution + ")!! Returning 'Unspecified'");
		return Unspecified;
	}
	
	
	/**
	 * Method to return the school attendance by parsing it from a string 
	 * version of the same  It is assumed the given String were originally 
	 * created through the <code>toString()</code> method. If not, and the 
	 * input string is the code from the census questionnaire, rather use the 
	 * {@link School2011#parseEducationFromCensusCode(String, String)} method.
	 * 
	 * @param type
	 * @return
	 */
	public static School2011 parseEducationFromString(String education){
		return School2011.valueOf(education);
	}
	
	
	/**
	 * Method to return an integer code for school attendance. The integer
	 * codes corresponds only vaguely to the codes used in the census 
	 * questionnaire.
	 * 
	 * @param type
	 * @return
	 */
	public static int getCode(School2011 education){
		switch (education) {
		case None:
			return 0;
		case PreSchool:
			return 1;
		case School:
			return 2;
		case Tertiary:
			return 3;
		case AdultEducation:
			return 4;
		case HomeSchooling:
			return 5;
		case Unknown:
			return 6;
		case Unspecified:
			return 99;
		case NotApplicable:
			return 7;
		}
		LOG.error("Unknown education attendance: " + education.toString() 
				+ "!! Returning code for 'Unspecified'");
		return 99;
	}
	

	/**
	 * Method to return an integer code for school attendance.  The integer codes 
	 * corresponds to the codes used in the census questionnaire.
	 * 
	 * @param s
	 * @return
	 */
	public static int getCode(String s){
		return getCode(parseEducationFromString(s));
	}

	
	/**
	 * Method to return the school attendance from a given code. The given 
	 * integer code is the one used internally by this class, and corresponds 
	 * only vaguely with the original census questionnaire codes. If you want 
	 * to parse the population group from the actual census code, rather use 
	 * the method {@link School2011#parseEducationFromCensusCode(String, String)}.
	 * 
	 * @param code
	 * @return
	 */
	public static School2011 getSchool(int code){
		switch (code) {
		case 0:
			return None;
		case 1:
			return PreSchool;
		case 2:
			return School;
		case 3:
			return Tertiary;
		case 4:
			return AdultEducation;
		case 5:
			return HomeSchooling;
		case 6:
			return Unknown;
		case 99:
			return Unspecified;
		case 7:
			return NotApplicable;
		}
		LOG.error("Unknown educational code: " + code + "!! Returning 'Unspecified'");
		return Unspecified;
	}
	
}


