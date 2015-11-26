/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.nmviljoen.network.maliklab;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.LinkedList;

import playground.nmviljoen.network.JungCentrality;
import playground.nmviljoen.network.JungClusters;
import playground.nmviljoen.network.JungGraphDistance;
import playground.nmviljoen.network.NmvLink;
import playground.nmviljoen.network.NmvNode;
import playground.nmviljoen.network.generator.ShortestPath;
import edu.uci.ics.jung.graph.DirectedGraph;

public class MalikLabExperiments {
	
	static DirectedGraph<NmvNode, NmvLink> myGraphGrid; 
	static DirectedGraph<NmvNode, NmvLink> myGraphMalik;
	static DirectedGraph<NmvNode, NmvLink> myGraphGhost;
	public MalikLabExperiments() {       
	}
	public static void main(String[] args) throws FileNotFoundException{
		int runs = 3;
		for (int y = 3; y<=runs;y++){
			String path = args[0];
			String sim =args[1];
			
			//if it's BASE
//			path = path+y+"/";
			
			//if it's a simulation
			path = path+y+"/"+sim+y+"/";
			
			myGraphMalik = TriGraphConstructor.constructMalikGraph(path);
			System.out.println("Malik Graph created");
//			myGraphGrid = TriGraphConstructor.constructGridGraph(path);
			myGraphGrid = TriGraphConstructor.constructGridGraphSim(path);
			System.out.println("Grid Graph created");
//			int[][] assocList = TriGraphConstructor.layerMalik(path,myGraphMalik,myGraphGrid);
			int[][] assocList = TriGraphConstructor.layerMalikSim(path);
			
			myGraphGhost = TriGraphConstructor.constructGhostGraph(path, assocList);
			System.out.println("The ghost lives");
			
			LinkedList<NmvLink> linkListGrid = new LinkedList<NmvLink>(myGraphGrid.getEdges());
			ArrayList<NmvNode> nodeListGrid = new ArrayList<NmvNode>(myGraphGrid.getVertices());
			LinkedList<NmvLink> linkListGhost = new LinkedList<NmvLink>(myGraphGhost.getEdges());
			ArrayList<NmvNode> nodeListGhost = new ArrayList<NmvNode>(myGraphGhost.getVertices());
			LinkedList<NmvLink> linkListMalik = new LinkedList<NmvLink>(myGraphMalik.getEdges());
			ArrayList<NmvNode> nodeListMalik = new ArrayList<NmvNode>(myGraphMalik.getVertices());
			String shortFile=path+"MalikShortPathStat.csv";
			ShortestPath.collectShortest(myGraphGrid, myGraphMalik, linkListGrid, linkListMalik, nodeListGrid, nodeListMalik, assocList, shortFile);
			
			//Grid graph metrics
			String Gridpath=path+"GridGraph";
			
			//Centrality scores
			
			JungCentrality.calculateAndWriteUnweightedBetweenness(myGraphGrid,Gridpath+"unweightedNodeBetweenness.csv", Gridpath+"unweightedEdgeBetweenness.csv",nodeListGrid, linkListGrid);
			JungCentrality.calculateAndWriteUnweightedCloseness(myGraphGrid, Gridpath+"unweightedCloseness.csv", nodeListGrid);
			JungCentrality.calculateAndWriteUnweightedEigenvector(myGraphGrid, Gridpath+"unweightedEigen.csv", nodeListGrid);
			JungCentrality.calculateAndWriteDegreeCentrality(myGraphGrid, Gridpath+"Degree.csv", nodeListGrid, linkListGrid);

			//Clustering
			JungClusters.calculateAndWriteClusteringCoefficient(myGraphGrid, Gridpath+"clusterCoeff.csv");
			JungClusters.calculateAndWriteWeakComponents(myGraphGrid, Gridpath+"weakComp.csv");
			JungClusters.calculateAndWriteTriadicCensus(myGraphGrid, Gridpath+"triadCensus.csv");

			//Graph distance
			JungGraphDistance.calculateAndWriteUnweightedDistances(myGraphGrid, path+"unweightedDist.csv");
			
			//Ghost graph metrics
			String Ghostpath=path+"GhostGraph";
			
			//Centrality scores
			
			JungCentrality.calculateAndWriteUnweightedBetweenness(myGraphGhost,Ghostpath+"unweightedNodeBetweenness.csv", Ghostpath+"unweightedEdgeBetweenness.csv",nodeListGhost, linkListGhost);
			JungCentrality.calculateAndWriteUnweightedCloseness(myGraphGhost, Ghostpath+"unweightedCloseness.csv", nodeListGhost);
			JungCentrality.calculateAndWriteUnweightedEigenvector(myGraphGhost, Ghostpath+"unweightedEigen.csv", nodeListGhost);
			JungCentrality.calculateAndWriteDegreeCentrality(myGraphGhost, Ghostpath+"Degree.csv", nodeListGhost, linkListGhost);

			//Clustering
			JungClusters.calculateAndWriteClusteringCoefficient(myGraphGhost, Ghostpath+"clusterCoeff.csv");
			JungClusters.calculateAndWriteWeakComponents(myGraphGhost, Ghostpath+"weakComp.csv");
			JungClusters.calculateAndWriteTriadicCensus(myGraphGhost, Ghostpath+"triadCensus.csv");

			//Graph distance
			JungGraphDistance.calculateAndWriteUnweightedDistances(myGraphGhost, Ghostpath+"unweightedDist.csv");
		}
		
		
			
		
	}

}
