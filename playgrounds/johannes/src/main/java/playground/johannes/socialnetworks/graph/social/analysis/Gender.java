/* *********************************************************************** *
 * project: org.matsim.*
 * Gender.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.graph.social.analysis;

import playground.johannes.socialnetworks.graph.social.SocialVertex;

/**
 * @author illenberger
 *
 */
public class Gender extends AbstractLinguisticAttribute {

	public static final String MALE = "m";
	
	public static final String FEMALE = "f";
	
	private static Gender instance;
	
	public static Gender getInstance() {
		if(instance == null)
			instance = new Gender();
		return instance;
	}

	@Override
	protected String attribute(SocialVertex v) {
		String gender = v.getPerson().getPerson().getSex();
		if(MALE.equalsIgnoreCase(gender))
			return MALE;
		else if(FEMALE.equalsIgnoreCase(gender))
			return FEMALE;
		else
			return null;
	}
}
