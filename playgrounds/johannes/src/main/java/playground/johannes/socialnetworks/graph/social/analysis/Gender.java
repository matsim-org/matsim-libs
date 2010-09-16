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

import gnu.trove.TObjectIntHashMap;
import playground.johannes.socialnetworks.graph.social.SocialEdge;
import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.social.SocialVertex;

/**
 * @author illenberger
 *
 */
public class Gender {

	public int[][] socioMatrix(SocialGraph graph) {
		int[][] matrix = new int[2][2];
		
		for(SocialEdge edge : graph.getEdges()) {
			String sex_i = edge.getVertices().getFirst().getPerson().getPerson().getSex();
			String sex_j = edge.getVertices().getSecond().getPerson().getPerson().getSex();
			
			if(sex_i != null && sex_j != null) {
				int idx_i = 0;
				if(sex_i.equalsIgnoreCase("f"))
					idx_i = 1;
				
				int idx_j = 0;
				if(sex_j.equalsIgnoreCase("f"))
					idx_j = 1;
				
				matrix[idx_i][idx_j]++;
				if(idx_i != idx_j)
					matrix[idx_j][idx_i]++;
			}
		}
		
		return matrix;
	}
	
	public TObjectIntHashMap<String> distribution(SocialGraph graph) {
		final String male = "m";
		final String female = "f";
		
		TObjectIntHashMap<String> distr = new TObjectIntHashMap<String>();
		
		for(SocialVertex vertex : graph.getVertices()) {
			if(male.equalsIgnoreCase(vertex.getPerson().getPerson().getSex()))
				distr.adjustOrPutValue(male, 1, 1);
			else if(female.equalsIgnoreCase(vertex.getPerson().getPerson().getSex()))
				distr.adjustOrPutValue(female, 1, 1);
		}
		
		return distr;
	}
}
