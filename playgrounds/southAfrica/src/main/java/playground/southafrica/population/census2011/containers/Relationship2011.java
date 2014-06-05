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
 * The relationship within the household as described in the Census 2011 
 * questionnaire, question <b>P02_RELATION</b>.
 *
 * @author jwjoubert
 */
public enum Relationship2011 {
	Head, Partner, Sibling, Child, Grandchild, Parent, Grandparent, 
	Other, Unrelated, Unspecified, NotApplicable;
	
	private final static Logger LOG = Logger.getLogger(Relationship2011.class);
	
	/**
	 * Method accepting the given code as per Question <b>P02_RELATION</b> 
	 * in the census questionnaire.
	 *  
	 * @param code formal value as per final code list in census questionnaire
	 * @return a descriptive type enum.
	 */
	public static Relationship2011 parseRelationshipFromCensusCode(String code){
		if(code.contains(".")){
			return NotApplicable;
		}
		
		code = code.replaceAll(" ", "");
		int codeInt = Integer.parseInt(code);
		
		switch (codeInt) {
		case 1:
			return Head;
		case 2:
			return Partner;
		case 3: // Son/daughter
		case 4: // Adopted son/daughter
		case 5: // Stepson/stepdaughter
		case 10: // Son/daughter-in-law
			return Child;
		case 6: // Brother/sister
		case 11: // Brother/sister-in-law
			return Sibling;
		case 7: // Parent
		case 8: // Parent-in-law
			return Parent;
		case 9:
			return Grandchild;
		case 12:
			return Grandparent;
		case 13:
			return Other;
		case 14:
			return Unrelated;
		case 99:
			return Unspecified;
		}
		LOG.error("Unknown census code: " + code + "!! Returning 'Unspecified'");
		return Unspecified;
	}
	
	
	/**
	 * Method to return the relationship by parsing it from a string version of 
	 * the same  It is assumed the given String were originally created through 
	 * the <code>toString()</code> method. If not, and the input string is the 
	 * code from the census questionnaire, rather use the 
	 * {@link Relationship2011#parseRelationshipFromCensusCode(String)} method.
	 * 
	 * @param type
	 * @return
	 */
	public static Relationship2011 parseRelationshipFromString(String relationship){
		return Relationship2011.valueOf(relationship);
	}
	
	
	/**
	 * Method to return an integer code for a relationship type. The integer
	 * codes corresponds only vaguely to the codes used in the census 
	 * questionnaire.
	 * 
	 * @param type
	 * @return
	 */
	public static int getCode(Relationship2011 relationship){
		switch (relationship) {
		case Head:
			return 1;
		case Partner:
			return 2;
		case Sibling:
			return 3;
		case Child:
			return 4;
		case Grandchild:
			return 5;
		case Parent:
			return 6;
		case Grandparent:
			return 7;
		case Other:
			return 8;
		case Unrelated:
			return 9;
		case Unspecified:
			return 99;
		case NotApplicable:
			return 10;
		}
		LOG.error("Unknown relationship: " + relationship.toString() + "!! Returning code for 'Unspecified'");
		return 99;
	}
	

	/**
	 * Method to return an integer code for a gender.  The integer codes 
	 * corresponds to the codes used in the census questionnaire.
	 * 
	 * @param s
	 * @return
	 */
	public static int getCode(String s){
		return getCode(parseRelationshipFromString(s));
	}

	
	/**
	 * Method to return the relationship from a given code. The given integer 
	 * code is the one used internally by this class, and corresponds only
	 * vaguely with the original census questionnaire codes. If you want to 
	 * parse the population group from the actual census code, rather use the 
	 * method {@link Relationship2011#parseEmploymentFromCensusCode(String)}.
	 * 
	 * @param code
	 * @return
	 */
	public static Relationship2011 getRelationship(int code){
		switch (code) {
		case 1:
			return Head;
		case 2:
			return Partner;
		case 3:
			return Sibling;
		case 4:
			return Child;
		case 5:
			return Grandchild;
		case 6:
			return Parent;
		case 7:
			return Grandparent;
		case 8:
			return Other;
		case 9:
			return Unrelated;
		case 99:
			return Unspecified;
		case 10:
			return NotApplicable;
		}
		LOG.error("Unknown type code: " + code + "!! Returning 'Unspecified'");
		return Unspecified;
	}
	
}


