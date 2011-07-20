/* *********************************************************************** *
 * project: org.matsim.*
 * AnalyzeSeed2SeedAPL.java
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
package playground.johannes.studies.snowball;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.sna.graph.SparseEdge;
import org.matsim.contrib.sna.graph.SparseGraph;
import org.matsim.contrib.sna.graph.SparseVertex;
import org.matsim.contrib.sna.graph.analysis.GraphAnalyzer;
import org.matsim.contrib.sna.graph.io.SparseGraphMLReader;
import org.matsim.contrib.sna.snowball.SampledGraphProjection;

import playground.johannes.socialnetworks.snowball2.analysis.SeedAPLTask;
import playground.johannes.socialnetworks.snowball2.io.SampledGraphProjMLReader;

/**
 * @author illenberger
 * 
 */
public class AnalyzeSeed2SeedAPL {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		SampledGraphProjMLReader<SparseGraph, SparseVertex, SparseEdge> reader = new SampledGraphProjMLReader<SparseGraph, SparseVertex, SparseEdge>(new SparseGraphMLReader());
		SampledGraphProjection<SparseGraph, SparseVertex, SparseEdge> graph = reader.readGraph("");

		Map<String, DescriptiveStatistics> map = GraphAnalyzer.analyze(graph, new SeedAPLTask());
		GraphAnalyzer.writeStatistics(map, "", true);
	}

}
