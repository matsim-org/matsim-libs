/* *********************************************************************** *
 * project: org.matsim.*
 * SpatialFilterComposite.java
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
package org.matsim.contrib.socnetgen.sna.graph.spatial.analysis;

import org.matsim.contrib.common.collections.Composite;
import org.matsim.contrib.socnetgen.sna.graph.analysis.GraphFilter;
import org.matsim.contrib.socnetgen.sna.graph.spatial.SpatialGraph;

/**
 * @author illenberger
 *
 */
public class SpatialFilterComposite extends Composite<GraphFilter<SpatialGraph>> implements GraphFilter<SpatialGraph> {

	/* (non-Javadoc)
	 * @see org.matsim.contrib.socnetgen.sna.graph.analysis.GraphFilter#apply(Graph)
	 */
	@Override
	public SpatialGraph apply(SpatialGraph graph) {
		SpatialGraph newGraph = graph;
		for(GraphFilter<SpatialGraph> filter : components) {
			newGraph = filter.apply(newGraph);
		}
		return newGraph;
	}

}
