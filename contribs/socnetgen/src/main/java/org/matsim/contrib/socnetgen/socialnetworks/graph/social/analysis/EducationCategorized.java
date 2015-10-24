/* *********************************************************************** *
 * project: org.matsim.*
 * EducationCategorized.java
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

import gnu.trove.TDoubleArrayList;
import org.apache.commons.math.stat.correlation.PearsonsCorrelation;
import org.matsim.contrib.socnetgen.socialnetworks.graph.social.SocialEdge;
import org.matsim.contrib.socnetgen.socialnetworks.graph.social.SocialVertex;

import java.util.Set;

/**
 * @author illenberger
 *
 */
public class EducationCategorized extends Education {

	private static EducationCategorized instance;
	
	public static EducationCategorized getInstance() {
		if(instance == null)
			instance = new EducationCategorized();
		
		return instance;
	}
	
	@Override
	protected String attribute(SocialVertex v) {
		String val = v.getPerson().getEducation();
		if(val != null) {
			if(val.equals("6") || val.equals("7")) {
				return "academic";
			} else {
				return "non-academic";
			}
		}
		return null;
	}
	
	public double correlation(Set<? extends SocialEdge> edges) {
		if (edges.isEmpty())
			return Double.NaN;
		else {
			TDoubleArrayList values1 = new TDoubleArrayList(2 * edges.size());
			TDoubleArrayList values2 = new TDoubleArrayList(2 * edges.size());

			for (SocialEdge edge : edges) {
				double e1 = numericValue(edge.getVertices().getFirst());
				double e2 = numericValue(edge.getVertices().getSecond());
				
				if (e1 > -1 && e2 > -1) {
					values1.add(e1);
					values2.add(e2);

					values1.add(e2);
					values2.add(e1);
				}
			}

			return new PearsonsCorrelation().correlation(values1.toNativeArray(), values2.toNativeArray());
		}
	}
	
	private double numericValue(SocialVertex v) {
		String val = v.getPerson().getEducation();
		if(val != null) {
			if(val.equals("6") || val.equals("7")) {
				return 1;
			} else {
				return 0;
			}
		}
		
		return -1;
	}

}
