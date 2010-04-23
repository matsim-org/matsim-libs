/* *********************************************************************** *
 * project: org.matsim.*
 * GraphTask.java
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

/**
 * Representation of a generic algorithm that modifies an existing graph or
 * creates a modified copy of a graph.
 * 
 * @author illenberger
 * 
 */
public interface GraphFilter<G extends Graph> {

	/**
	 * Applies a modification to <tt>graph</tt> or creates and returns a
	 * modified copy of <tt>graph</tt>.
	 * 
	 * @param graph
	 *            a graph.
	 * @return the modified graph.
	 */
	public G apply(G graph);

}
