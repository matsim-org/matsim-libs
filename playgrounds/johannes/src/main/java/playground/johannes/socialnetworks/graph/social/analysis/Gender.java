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

import gnu.trove.TDoubleArrayList;

import java.util.Set;

import org.apache.commons.math.stat.correlation.PearsonsCorrelation;

import playground.johannes.socialnetworks.graph.social.SocialEdge;
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
	
	public double correlation(Set<? extends SocialEdge> edges) {
		TDoubleArrayList values1 = new TDoubleArrayList(2 * edges.size());
		TDoubleArrayList values2 = new TDoubleArrayList(2 * edges.size());
		
		for(SocialEdge edge : edges) {
			String g1 = edge.getVertices().getFirst().getPerson().getPerson().getSex();
			String g2 = edge.getVertices().getSecond().getPerson().getPerson().getSex();
			
			if(g1 != null && g2 != null) {
				int val1 = 0;
				if(g1.equalsIgnoreCase(FEMALE))
					val1 = 1;
				
				int val2 = 0;
				if(g2.equalsIgnoreCase(FEMALE))
					val2 = 1;
				
				values1.add(val1);
				values2.add(val2);
				
				values1.add(val2);
				values2.add(val1);
			}
		}
		
		return new PearsonsCorrelation().correlation(values1.toNativeArray(), values2.toNativeArray());
	}
}
