/* *********************************************************************** *
 * project: org.matsim.*
 * Frequency.java
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

import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;

import java.util.Set;

import javax.swing.text.AbstractDocument.LeafElement;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.sna.math.Distribution;
import org.matsim.contrib.sna.snowball.SampledVertex;

import playground.johannes.socialnetworks.graph.social.SocialEdge;
import playground.johannes.socialnetworks.graph.social.SocialVertex;

/**
 * @author illenberger
 *
 */
public class Frequency {

	public DescriptiveStatistics statistics(Set<SocialEdge> edges) {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		
		for(SocialEdge edge : edges) {
			stats.addValue(edge.getFrequency());
		}
		
		return stats;
	}
	
	public TObjectDoubleHashMap<SocialVertex> meanEdgeFrequency(Set<? extends SocialVertex> vertices) {
		TObjectDoubleHashMap<SocialVertex> values = new TObjectDoubleHashMap<SocialVertex>();
		
		for(SocialVertex vertex : vertices) {
			if(((SampledVertex)vertex).isSampled()) {
				if(vertex.getNeighbours().size() > 0) {
				double sum = 0;
				for(SocialEdge edge : vertex.getEdges())
					sum += edge.getFrequency();
				values.put(vertex, sum/(double)vertex.getEdges().size());
				}
			}
		}
		
		return values;
	}
	
	public TObjectDoubleHashMap<SocialVertex> sumEdgeFrequency(Set<? extends SocialVertex> vertices) {
		TObjectDoubleHashMap<SocialVertex> values = new TObjectDoubleHashMap<SocialVertex>();
		
		for(SocialVertex vertex : vertices) {
			if(((SampledVertex)vertex).isSampled()) {
				double sum = 0;
				for(SocialEdge edge : vertex.getEdges())
					sum += edge.getFrequency();
				values.put(vertex, sum);
			}
		}
		
		return values;
	}
	
	public Distribution sumEdgeFrequencyDistribtion(Set<? extends SocialVertex> vertices) {
		Distribution distr = new Distribution();
		TObjectDoubleHashMap<SocialVertex> values = sumEdgeFrequency(vertices);
		TObjectDoubleIterator<SocialVertex> it = values.iterator();
		
		for(int i = 0; i < values.size(); i++) {
			it.advance();
			distr.add(it.value());
		}
		
		return distr;
	}
	
	public Distribution freqLengthDistribution(Set<? extends SocialVertex> vertices) {
		Distribution distr = new Distribution();
		
		for(SocialVertex vertex : vertices) {
			if(((SampledVertex)vertex).isSampled()) {
				for(SocialEdge edge : vertex.getEdges())
					if(!Double.isNaN(edge.length()) && edge.length() > 0)
						distr.add(edge.getFrequency() * edge.length());
			}
		}
		
		return distr;
	}
	
	
	public TObjectDoubleHashMap<SocialVertex> sumFreqCost(Set<? extends SocialVertex> vertices) {
		TObjectDoubleHashMap<SocialVertex> values = new TObjectDoubleHashMap<SocialVertex>();
		
		for(SocialVertex vertex : vertices) {
			if(((SampledVertex)vertex).isSampled()) {
				double sum = 0;
				for(SocialEdge edge : vertex.getEdges()) {
					if(!Double.isNaN(edge.length()) && edge.length() > 0)
						sum += edge.getFrequency() * Math.log(edge.length());
				}
				values.put(vertex, sum);
			}
		}
		
		return values;
	}
	
	public Distribution sumFreqLengthDistribution(Set<? extends SocialVertex> vertices) {
		Distribution distr = new Distribution();
		TObjectDoubleHashMap<SocialVertex> values = sumFreqCost(vertices);
		TObjectDoubleIterator<SocialVertex> it = values.iterator();
		
		for(int i = 0; i < values.size(); i++) {
			it.advance();
			distr.add(it.value());
		}
		
		return distr;
	}
}
