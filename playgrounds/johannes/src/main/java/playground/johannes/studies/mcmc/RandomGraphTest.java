/* *********************************************************************** *
 * project: org.matsim.*
 * RandomGraphTest.java
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
package playground.johannes.studies.mcmc;

import org.matsim.contrib.sna.graph.SparseEdge;
import org.matsim.contrib.sna.graph.SparseGraph;
import org.matsim.contrib.sna.graph.SparseGraphBuilder;
import org.matsim.contrib.sna.graph.SparseVertex;

import playground.johannes.socialnetworks.graph.generators.RandomGraphGenerator;
import playground.johannes.socialnetworks.statistics.LogNormalDistribution;

/**
 * @author illenberger
 *
 */
public class RandomGraphTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SparseGraphBuilder builder = new SparseGraphBuilder();
		LogNormalDistribution func = new LogNormalDistribution(1, 2.5, 1);
		RandomGraphGenerator<SparseGraph, SparseVertex, SparseEdge> generator = new RandomGraphGenerator<SparseGraph, SparseVertex, SparseEdge>(func, builder, 12315);
		
		int N = 500;
		generator.generate(N, 41);
		
		int invalidOld = 0;
		int invalidNew = generator.getInvalidGraphs();
		int cnt = 1;
		while(invalidNew > invalidOld) {
			invalidOld = invalidNew;
			cnt++;
			generator.generate(N, 41);
			invalidNew = generator.getInvalidGraphs();
			
			System.out.println(String.format("Draw %1$s: Invlalid graphs = %2$s, invalid sequences = %3$s.", cnt, generator.getInvalidGraphs(), generator.getInvalidSequences()));
		}

	}

}
