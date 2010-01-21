/* *********************************************************************** *
 * project: org.matsim.*
 * RandomGraphGenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.graph.spatial.generators;

import java.io.IOException;

import org.matsim.contrib.sna.gis.CRSUtils;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.Degree;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseEdge;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseGraphBuilder;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseVertex;
import org.matsim.contrib.sna.graph.spatial.io.SpatialGraphMLWriter;

import playground.johannes.socialnetworks.graph.generators.ErdosRenyiGenerator;
import playground.johannes.socialnetworks.graph.spatial.io.Population2SpatialGraph;

/**
 * @author illenberger
 *
 */
public class RandomGraphGenerator {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Population2SpatialGraph reader = new Population2SpatialGraph(CRSUtils.getCRS(21781));
		SpatialSparseGraph graph = reader.read("/Users/fearonni/vsp-work/work/socialnets/data/schweiz/complete/plans/plans.0.001.xml");
		
		ErdosRenyiGenerator<SpatialSparseGraph, SpatialSparseVertex, SpatialSparseEdge> generator = new ErdosRenyiGenerator<SpatialSparseGraph, SpatialSparseVertex, SpatialSparseEdge>(new SpatialSparseGraphBuilder(graph.getCoordinateReferenceSysten()));
		graph = generator.generate(graph, 0.001, 4711);
		
		System.out.println(new Degree().distribution(graph.getVertices()).mean());
//		SpatialGraphAnalyzer.analyze(graph, "/Users/fearonni/vsp-work/work/socialnets/mcmc/", false, SpatialGrid.readFromFile("/Users/fearonni/vsp-work/work/socialnets/data/schweiz/zrh100km/popdensity/popdensity.1000.xml"));
		SpatialGraphMLWriter writer = new SpatialGraphMLWriter();
		writer.write(graph, "/Users/fearonni/vsp-work/work/socialnets/mcmc/graph.graphml");
	}

}
