/* *********************************************************************** *
 * project: org.matsim.*
 * ComponentValidator.java
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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.matsim.contrib.sna.graph.analysis.Components;
import org.matsim.contrib.sna.snowball.SampledGraph;
import org.matsim.contrib.sna.snowball.SampledVertex;

import playground.johannes.socialnetworks.snowball2.social.SocialSampledGraphProjection;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseEdge;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraph;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseVertex;
import playground.johannes.socialnetworks.survey.ivt2009.graph.io.GraphReaderFacade;

/**
 * @author illenberger
 *
 */
public class ComponentValidator implements GraphValidator<SampledGraph> {

	private List<Set<SampledVertex>> orphanedComps;
	
	public List<Set<SampledVertex>> getOrphandComponents() {
		return orphanedComps;
	}
	
	@Override
	public boolean validate(SampledGraph graph) {
		Components components = new Components();
		List<Set<SampledVertex>> comps = components.components(graph);
		
		orphanedComps = new ArrayList<Set<SampledVertex>>(comps.size());
		
//		int i = 0;
		for(Set<SampledVertex> comp : comps) {
			boolean found = false;
			for(SampledVertex vertex : comp) {
				if(vertex.isSampled() && vertex.getIterationSampled() == 0) {
					found = true;
					break;
				}
			}
			
			if(!found) {
				orphanedComps.add(comp);
				
//				i++;
////				return false;
//				System.err.println("No seed");
//				
//				Distribution distr = new Distribution();
//				for(SampledVertex vertex : comp) {
//					if(vertex.isSampled())
//						distr.add(vertex.getIterationSampled());
//				}
//				
//				try {
//					Distribution.writeHistogram(distr.absoluteDistribution(), "/Users/jillenberger/Work/work/socialnets/data/ivt2009/raw/06-2010/tmp/" + i + ".comp.txt");
//					
//					BufferedWriter writer = new BufferedWriter(new FileWriter("/Users/jillenberger/Work/work/socialnets/data/ivt2009/raw/06-2010/tmp/" + i + ".vertices.txt"));
//					for(SampledVertex vertex : comp) {
//						writer.write(vertex.toString());
//						writer.newLine();
//					}
//					writer.close();
//				} catch (FileNotFoundException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
			}
		}
		
		return orphanedComps.isEmpty();
	}
	
	public static void main(String args[]) throws IOException {
		GraphReaderFacade reader = new GraphReaderFacade();
		SocialSampledGraphProjection<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> graph = reader.read("/Users/jillenberger/Work/work/socialnets/data/ivt2009/raw/09-2010/graph/graph.graphml");
		
		String output ="/Users/jillenberger/Work/work/socialnets/data/ivt2009/raw/09-2010/graph/";
		
		ComponentValidator validator = new ComponentValidator();
		if(!validator.validate(graph)) {
			List<Set<SampledVertex>> comps = validator.getOrphandComponents();
			int i = 0;
			for(Set<SampledVertex> comp : comps) {
				BufferedWriter writer = new BufferedWriter(new FileWriter(String.format("%1$s/component%2$s.txt", output, i)));
				for(SampledVertex vertex : comp) {
					writer.write(vertex.toString());
					writer.newLine();
				}
				writer.close();
				i++;
			}
		}
		
		
	}

}
