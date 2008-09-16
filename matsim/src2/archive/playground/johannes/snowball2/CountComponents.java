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

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import playground.johannes.statistics.GraphStatistics;
import edu.uci.ics.jung.graph.Graph;

/**
 * @author illenberger
 *
 */
public class CountComponents extends GraphStatistic {
	
	public CountComponents(String outputDir) {
		super(outputDir);
	}
	
	public DescriptiveStatistics calculate(Graph g, int iteration, DescriptiveStatistics reference) {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		stats.addValue(GraphStatistics.getDisconnectedComponents(g).size());
		return stats;
	}
}
