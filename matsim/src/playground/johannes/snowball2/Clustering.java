/* *********************************************************************** *
 * project: org.matsim.*
 * Clustering.java
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

import java.util.Map;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import playground.johannes.snowball.Histogram;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.statistics.GraphStatistics;

/**
 * @author illenberger
 *
 */
public class Clustering extends GraphStatistic {

	public Clustering(String outputDir) {
		super(outputDir);
	}

	@SuppressWarnings("unchecked")
	@Override
	public DescriptiveStatistics calculate(Graph g, int iteration, DescriptiveStatistics reference) {
		Map<Vertex, Double> values = GraphStatistics.clusteringCoefficients(g);
		DescriptiveStatistics stats = new DescriptiveStatistics();

		if (g instanceof SampledGraph) {
			for (Vertex v : values.keySet()) {
				if (!((SampledVertex) v).isAnonymous()) {
					if (v.degree() == 1)
						stats.addValue(0.0);
					else
						stats.addValue(values.get(v));
				}
			}
		} else {
			for (Vertex v : values.keySet()) {
				if (v.degree() == 1)
					stats.addValue(0.0);
				else
					stats.addValue(values.get(v));
			}
		}

		dumpStatistics(getStatisticsMap(stats), iteration);
		
		if(reference != null) {
			Histogram hist = new Histogram(100, reference.getMin(), reference.getMax());
			plotHistogram(stats.getValues(), hist, iteration);
		} else {
			plotHistogram(stats.getValues(), new Histogram(100), iteration);
		}
		
		return stats;
	}

}
