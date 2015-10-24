/* *********************************************************************** *
 * project: org.matsim.*
 * APLDistributionAnalyzer.java
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
package playground.johannes.studies.smallworld;

import org.matsim.contrib.socnetgen.sna.graph.Graph;
import org.matsim.contrib.socnetgen.sna.graph.analysis.GraphAnalyzer;
import org.matsim.contrib.socnetgen.sna.graph.io.SparseGraphMLReader;
import playground.johannes.socialnetworks.graph.analysis.AnalyzerTaskComposite;
import playground.johannes.socialnetworks.graph.analysis.ExtendedTopologyAnalyzerTask;
import playground.johannes.socialnetworks.graph.analysis.TopologyAnalyzerTask;

import java.io.IOException;

/**
 * @author illenberger
 *
 */
public class APLDistributionAnalyzer {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		AnalyzerTaskComposite composite = new AnalyzerTaskComposite();
		composite.addTask(new TopologyAnalyzerTask());
		composite.addTask(new ExtendedTopologyAnalyzerTask());
		
		SparseGraphMLReader reader = new SparseGraphMLReader();
		Graph graph = reader.readGraph("/Users/jillenberger/vsp/work/data/graphs/hep-th/hep-th.graphml");
		
		GraphAnalyzer.analyze(graph, composite, "/Users/jillenberger/vsp/work/data/graphs/hep-th/analysis/");

	}

}
