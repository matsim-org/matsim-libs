/* *********************************************************************** *
 * project: org.matsim.*
 * CountComponents.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.johannes.snowball2;

import edu.uci.ics.jung.algorithms.cluster.ClusterSet;
import edu.uci.ics.jung.algorithms.cluster.WeakComponentClusterer;
import edu.uci.ics.jung.graph.Graph;

/**
 * @author illenberger
 *
 */
public class CountComponents implements GraphStatistic {
	
	private WeakComponentClusterer wcc = new WeakComponentClusterer();
	
	private ClusterSet cSet;

	public double run(Graph g) {
		cSet = wcc.extract(g);
		return cSet.size();
	}

	public ClusterSet getClusterSet() {
		return cSet;
	}
}
