/* *********************************************************************** *
 * project: org.matsim.*
 * SocioMatrix.java
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
package playground.johannes.socialnetworks.survey.ivt2009.analysis;

import gnu.trove.TObjectIntHashMap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.snowball.SampledVertex;

import playground.johannes.socialnetworks.graph.social.SocialEdge;
import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.social.SocialVertex;

/**
 * @author illenberger
 *
 */
public abstract class SocioMatrixLegacy {

	private List<String> attrs;
	
	protected abstract String getAttributeValue(SocialVertex vertex);
	 
	public double[][] socioMatrix(SocialGraph graph) {
		Set<String> values = new HashSet<String>();
		for(SocialVertex vertex : graph.getVertices()) {
			String value = getAttributeValue(vertex);
			if(value != null)
				values.add(value);
		}
		
		attrs = new ArrayList<String>(values);
		double[][] matrix = new double[attrs.size()][attrs.size()];
		
		for(SocialEdge edge : graph.getEdges()) {
			String att1 = getAttributeValue(edge.getVertices().getFirst());
			String att2 = getAttributeValue(edge.getVertices().getSecond());
			
			if(att1 != null && att2 != null) {
				int idx_i = attrs.indexOf(att1);
				int idx_j = attrs.indexOf(att2);
				
				matrix[idx_i][idx_j]++;
				if(idx_i != idx_j)
					matrix[idx_j][idx_i]++;
			}
		}
		
		return matrix;
	}
	
	public List<String> getAttributes() {
		return attrs;
	}
	
	public TObjectIntHashMap<String> distribution(Set<? extends SocialVertex> vertices) {
		TObjectIntHashMap<String> distr = new TObjectIntHashMap<String>();
		for(SocialVertex vertex : vertices) {
			String key = getAttributeValue(vertex);
			if(key != null)
				distr.adjustOrPutValue(key, 1, 1);
		}
		
		return distr;
	}
	
	public double[][] normalizedSocioMatrix(double[][] matrix, TObjectIntHashMap<String> distr, List<String> values) {
		double[][] normMatrix = new double[matrix.length][matrix.length];
		
		for(int i = 0; i < normMatrix.length; i++) {
			double sum = 0;
			for(int j = 0; j < normMatrix.length; j++) {
				sum += matrix[i][j];
			}
			for(int j = 0; j < normMatrix.length; j++) {
				normMatrix[i][j] = matrix[i][j]/sum;
			}
		}
		
		return normMatrix;
	}
	
//	public double[][] localAverage(Map<Vertex, ?> values, Object[] indices) {
//		
//		
//	}
	
	public double[][] socioMatrixLocalAvr(SocialGraph graph) {
		Set<String> values = new HashSet<String>();
		for(SocialVertex vertex : graph.getVertices()) {
			String value = getAttributeValue(vertex);
			if(value != null) {
				values.add(convert(value));
			}
		}
		
		attrs = new ArrayList<String>(values);
		double[][] matrix = new double[attrs.size()][attrs.size()];
		double[][] count = new double[attrs.size()][attrs.size()];
		
		for(SocialVertex vertex : graph.getVertices()) {
			if(((SampledVertex)vertex).isSampled()) {
			String att1 = convert(getAttributeValue(vertex));
			if (att1 != null) {
				int idx_i = attrs.indexOf(att1);
				int cnt[] = new int[attrs.size()];
				int total = 0;
				for (SocialEdge edge : vertex.getEdges()) {
					String att2 = convert(getAttributeValue(edge.getOpposite(vertex)));
					if(att2 != null) {
						int idx_j = attrs.indexOf(att2);
						cnt[idx_j]++;
						total++;
					}
				}
				
				for(int j = 0; j < matrix.length; j++) {
					if(total > 0) {
					matrix[idx_i][j] += cnt[j]/(double)total;
					count[idx_i][j]++;
					}
				}
			}
			}
		}
		
		for(int i = 0; i < matrix.length; i++) {
			for(int j = 0; j < matrix.length; j++) {
				matrix[i][j] = matrix[i][j]/count[i][j]; 
			}
		}
		
		return matrix;
	}
	
	private String convert(String value) {
//		if(value == null)
//			return null;
//		
//		if(value.equalsIgnoreCase("6") || value.equalsIgnoreCase("7"))
//			return "academic";
//		else
//			return "nonacademic";
		return value;
	}
}
