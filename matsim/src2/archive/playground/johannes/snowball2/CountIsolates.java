/* *********************************************************************** *
 * project: org.matsim.*
 * CountIsolates.java
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

import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;

/**
 * @author illenberger
 *
 */
public class CountIsolates extends GraphStatistic {

	public CountIsolates(String outputDir) {
		super(outputDir);
	}

	@SuppressWarnings("unchecked")
	@Override
	public DescriptiveStatistics calculate(Graph g, int iteration,
			DescriptiveStatistics reference) {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		Set<Vertex> vertices = g.getVertices();
		int count = 0;
		for(Vertex v : vertices)
			if(v.degree() == 0)
				count++;
		stats.addValue(count);
		return stats;
	}

}
