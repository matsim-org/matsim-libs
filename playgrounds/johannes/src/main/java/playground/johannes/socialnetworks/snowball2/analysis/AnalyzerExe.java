/* *********************************************************************** *
 * project: org.matsim.*
 * AnalyzerExe.java
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
package playground.johannes.socialnetworks.snowball2.analysis;

import org.matsim.contrib.sna.snowball.SampledGraph;
import org.matsim.contrib.sna.snowball.spatial.io.SampledSpatialGraphMLReader;

import playground.johannes.socialnetworks.graph.analysis.GraphAnalyzer;
import playground.johannes.socialnetworks.graph.analysis.SimpleGraphPropertyFactory;
import playground.johannes.socialnetworks.graph.analysis.StandardAnalyzerTask;

/**
 * @author illenberger
 *
 */
public class AnalyzerExe {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SampledSpatialGraphMLReader reader = new SampledSpatialGraphMLReader();
		SampledGraph graph = reader.readGraph("/Users/jillenberger/Work/work/socialnets/data/ivt2009/graph/graph.graphml");

//		GraphAnalyzer.analyze(graph, new SampledGraphPropertyFactory(), new StandardAnalyzerTask());
		GraphAnalyzer.analyze(graph, new SimpleGraphPropertyFactory(), new StandardAnalyzerTask());
	}

}
