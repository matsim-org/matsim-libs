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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import playground.johannes.socialnetworks.graph.social.SocialVertex;

/**
 * @author illenberger
 *
 */
public class Gender {

	public static final String MALE = "m";
	
	public static final String FEMALE = "f";
	
	private static Gender instance;
	
	public static Gender getInstance() {
		if(instance == null)
			instance = new Gender();
		return instance;
	}
	
	public Map<SocialVertex, String> values(Set<? extends SocialVertex> vertices) {
		Map<SocialVertex, String> map = new HashMap<SocialVertex, String>(vertices.size());
		for(SocialVertex v : vertices) {
			String gender = v.getPerson().getPerson().getSex();
			if(MALE.equalsIgnoreCase(gender))
				map.put(v, MALE);
			else if(FEMALE.equalsIgnoreCase(FEMALE))
				map.put(v, FEMALE);
		}
		return map;
	}
	
	public SocioMatrix<String> countsMatrix(Set<? extends SocialVertex> vertices) {
		return SocioMatrixBuilder.countsMatrix(this.values(vertices));
	}
	
	public SocioMatrix<String> probaMatrix(Set<? extends SocialVertex> vertices) {
		Map<SocialVertex, String> values = this.values(vertices);
		return SocioMatrixBuilder.probaMatrix(values, values);
	}
}
