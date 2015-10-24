/* *********************************************************************** *
 * project: org.matsim.*
 * GenderNumeric.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package org.matsim.contrib.socnetgen.socialnetworks.graph.social.analysis;

import gnu.trove.TObjectDoubleHashMap;
import org.matsim.contrib.socnetgen.sna.graph.Vertex;
import org.matsim.contrib.socnetgen.sna.graph.analysis.AbstractVertexProperty;
import org.matsim.contrib.socnetgen.socialnetworks.graph.social.SocialVertex;
import org.matsim.core.population.PersonUtils;

import java.util.Set;

/**
 * @author illenberger
 *
 */
public class GenderNumeric extends AbstractVertexProperty {

	private static GenderNumeric instance;
	
	public static GenderNumeric getInstance() {
		if(instance == null)
			instance = new GenderNumeric();
		
		return instance;
	}
	
	@Override
	public TObjectDoubleHashMap<Vertex> values(Set<? extends Vertex> vertices) {
		TObjectDoubleHashMap<Vertex> values = new TObjectDoubleHashMap<Vertex>(vertices.size());
		
		for(Vertex vertex : vertices) {
			String gender = PersonUtils.getSex(((SocialVertex) vertex).getPerson().getPerson());
			
			if(Gender.FEMALE.equalsIgnoreCase(gender))
				values.put(vertex, 1.0);
			else if(Gender.MALE.equalsIgnoreCase(gender))
				values.put(vertex, 0.0);
			
		}
		
		return values;
	}

}
