/* *********************************************************************** *
 * project: org.matsim.*
 * GraphTaskComposite.java
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
package playground.johannes.socialnetworks.graph.analysis;

import org.matsim.contrib.sna.graph.Graph;

import playground.johannes.socialnetworks.utils.Composite;

/**
 * @author illenberger
 *
 */
public class GraphFilterComposite<G extends Graph> extends Composite<GraphFilter<G>> implements GraphFilter<G>{

	@Override
	public G apply(G graph) {
		for(GraphFilter<G> filter : components) {
			graph = filter.apply(graph);
		}
		
		return graph;
	}
}
