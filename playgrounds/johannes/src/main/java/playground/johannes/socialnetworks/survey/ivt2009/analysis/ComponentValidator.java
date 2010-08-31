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

import java.util.List;
import java.util.Set;

import org.matsim.contrib.sna.graph.analysis.Components;
import org.matsim.contrib.sna.snowball.SampledGraph;
import org.matsim.contrib.sna.snowball.SampledVertex;

/**
 * @author illenberger
 *
 */
public class ComponentValidator implements GraphValidator<SampledGraph> {

	/* (non-Javadoc)
	 * @see playground.johannes.socialnetworks.survey.ivt2009.analysis.GraphValidator#validate(org.matsim.contrib.sna.graph.Graph)
	 */
	@Override
	public boolean validate(SampledGraph graph) {
		Components components = new Components();
		List<Set<SampledVertex>> comps = components.components(graph);
		
		for(Set<SampledVertex> comp : comps) {
			boolean found = false;
			for(SampledVertex vertex : comp) {
				if(vertex.isSampled() && vertex.getIterationSampled() == 0) {
					found = true;
					break;
				}
			}
			
			if(!found)
//				return false;
				System.err.println("No seed");
		}
		
		return true;
	}

}
