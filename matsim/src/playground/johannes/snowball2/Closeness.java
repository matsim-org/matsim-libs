/* *********************************************************************** *
 * project: org.matsim.*
 * Closeness.java
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

import playground.johannes.snowball.Histogram;
import edu.uci.ics.jung.graph.Graph;

/**
 * @author illenberger
 *
 */
public class Closeness implements VertexStatistic {

	private Centrality centrality;
	
	public Closeness(Centrality centrality) {
		this.centrality = centrality;
	}
	
	public Histogram getHistogram() {
		return centrality.getClosenessHistogram();
	}

	public Histogram getHistogram(double min, double max) {
		return centrality.getBetweennessHistogram(min, max);
	}

	public double run(Graph g) {
		if(!centrality.didRun())
			centrality.run(g);
		
		return centrality.getGraphCloseness();
	}

}
