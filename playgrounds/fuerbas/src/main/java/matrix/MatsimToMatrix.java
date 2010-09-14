/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityTimeAnalyzer.java
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

package matrix;

import gnu.trove.TIntDoubleHashMap;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.sna.graph.matrix.AdjacencyMatrix;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;

import playground.johannes.socialnetworks.graph.matrix.EdgeCostFunction;
import playground.johannes.socialnetworks.graph.matrix.MatrixCentrality;
import playground.johannes.socialnetworks.graph.matrix.WeightedDijkstraFactory;


public class MatsimToMatrix {
	
	@SuppressWarnings("unchecked")
	public static void main(String args[]) throws IOException {
	
	Scenario scenario = new ScenarioImpl();
	NetworkImpl network = (NetworkImpl) scenario.getNetwork();
	new MatsimNetworkReader(scenario).readFile(args[0]);
	BufferedWriter bwLinks = new BufferedWriter(new FileWriter(args[1]));	// Output Directory for List of Links
	BufferedWriter bwBetw = new BufferedWriter(new FileWriter(args[2]));	//Output Directory for Betweenness Calculation Results
	
	final List<Node> nodeList = new ArrayList<Node>(network.getNodes().size());
	final List<Link> linkList = new ArrayList<Link>(network.getLinks().size());	
	final List<HashMap<Integer, Double>> costList = new ArrayList<HashMap<Integer, Double>>();
	
	AdjacencyMatrix y = new AdjacencyMatrix();
		
	for (Node node : network.getNodes().values()) {
		int idx = y.addVertex();
		nodeList.add(idx, node);
	}
	
	for (int i=0; i<=network.getLinks().size(); i++){
		costList.add(i, new HashMap());
	}
	
	int linkIdx = 0;
	for (Link link : network.getLinks().values()) {
		if (nodeList.contains(link.getFromNode())) {
			int from; int to;
			from=nodeList.indexOf(link.getFromNode());
			to=nodeList.indexOf(link.getToNode());
			if (!y.getEdge(to, from) || !y.getEdge(from, to)) {
			y.addEdge(nodeList.indexOf(link.getFromNode()), nodeList.indexOf(link.getToNode()));
			linkList.add(linkIdx, link);		
			costList.get(from).put((to), link.getLength()/link.getFreespeed());					
			costList.get(to).put((from), link.getLength()/link.getFreespeed());
			bwLinks.write(linkIdx+"\t"+link.getId().toString());
			bwLinks.newLine();
			linkIdx++;
			}
		}			
	}
	bwLinks.close();
	
	
	class EdgeCost implements EdgeCostFunction {

		private double costs;
		@Override
		public double edgeCost(int i, int j) {
			
			costs = costList.get(i).get(j);
			return costs;

		}		
	}
	
	EdgeCost edgeCost = new EdgeCost();
	System.out.println("EDGE COST INIT "+Runtime.getRuntime().freeMemory());
	
	WeightedDijkstraFactory factory = new WeightedDijkstraFactory(edgeCost);
	System.out.println("WEIGHTED DIJKSTRA INIT "+Runtime.getRuntime().freeMemory());
	
	MatrixCentrality MatrixCent = new MatrixCentrality(1);
	System.out.println("MATRIX CENT INIT "+Runtime.getRuntime().freeMemory());
	
	MatrixCent.setDijkstraFactory(factory);
	System.out.println("MATRIX CENT SET FACTORY "+Runtime.getRuntime().freeMemory());
		
	MatrixCent.run(y);
	System.out.println("MATRIX CENT RUN CPLT "+Runtime.getRuntime().freeMemory());
	
	TIntDoubleHashMap[] betweenness = MatrixCent.getEdgeBetweenness();

	double betwFromTo;	
	
	for (Link link : linkList) {

		Integer from=0; Integer to=0;		
		from=nodeList.indexOf(link.getFromNode());
		to=nodeList.indexOf(link.getToNode());		
		betwFromTo=betweenness[from].get(to);
		
		bwBetw.write(link.getId().toString()+"\t"+betwFromTo);	//Output: linkIdx TAB Betweenness
		bwBetw.newLine();
				
	}
	bwBetw.close();
	}
}
