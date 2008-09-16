/* *********************************************************************** *
 * project: org.matsim.*
 * PersonGraphMLFileHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.johannes.socialnets;

import java.util.Map;

import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.io.GraphMLFileHandler;

/**
 * @author illenberger
 *
 */
public class PersonGraphMLFileHandler extends GraphMLFileHandler {

//	private Plans plans;
	
	@Override
	protected ArchetypeVertex createVertex(Map attrs) {
		ArchetypeVertex v = super.createVertex(attrs);
//		String personString = (String)attrs.get(UserDataKeys.PERSON_KEY);
//		int startPos = personString.indexOf("[id=") + 4;
//		int endPos = personString.indexOf("]", startPos);
//		String id = personString.substring(startPos, endPos);
//		Person person = plans.getPerson(id);
//		v.setUserDatum(UserDataKeys.PERSON_KEY, person, UserDataKeys.COPY_ACT);

		v.setUserDatum(UserDataKeys.ID, (String)attrs.get(UserDataKeys.ID), UserDataKeys.COPY_ACT);
		v.setUserDatum(UserDataKeys.X_COORD, Double.parseDouble((String) attrs.get(UserDataKeys.X_COORD)), UserDataKeys.COPY_ACT);
		v.setUserDatum(UserDataKeys.Y_COORD, Double.parseDouble((String) attrs.get(UserDataKeys.Y_COORD)), UserDataKeys.COPY_ACT);
		return v;
	}

//	public PersonGraphMLFileHandler(Plans plans) {
//		this.plans = plans;
//	}

}
