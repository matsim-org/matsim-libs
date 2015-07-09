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

package playground.nmviljoen.network.generator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.matsim.core.utils.io.IOUtils;

import com.orientechnologies.orient.core.storage.OStorage.SIZE;

import playground.nmviljoen.network.JungCentrality;
import playground.nmviljoen.network.JungClusters;
import playground.nmviljoen.network.JungGraphDistance;
import playground.nmviljoen.network.NmvLink;
import playground.nmviljoen.network.generator.Grid;
import playground.nmviljoen.network.salience.SampleNetworkBuilder;
import playground.nmviljoen.network.NmvNode;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;

public class GenDirectedNetwork {
	/** MyNode and MyLink are classes at the end of the script*/
	DirectedGraph<NmvNode, NmvLink> myGraphGrid; 
	DirectedGraph<NmvNode, NmvLink> myGraphHub;

	/** Creates a new instance of BasicDirectedGraph */
	public GenDirectedNetwork() {       
	}
	public void constructGridGraph(){
		//get the linklist
		int row=50;
		int col = 50;
		int [][] linkList = Grid.linkList(row, col);
		double defaultWeight = 1;
		double defaultTransProb = 1;
		myGraphGrid = new DirectedSparseMultigraph<NmvNode, NmvLink>();
		//create the node list
		ArrayList<NmvNode> nodeList = new ArrayList<NmvNode>();
		for (int n =1; n<=row*col;n++){
			NmvNode currentNode = new NmvNode("","", 0, 0);
			currentNode = new NmvNode(Integer.toString(n-1),Integer.toString(n),0,0);
			nodeList.add(n-1, currentNode);
		}
		int indexTo;
		int indexFrom;
		for (int w = 0; w<linkList.length;w++){
			indexFrom =linkList[w][0] -1;
			indexTo = linkList[w][1] -1;
			myGraphGrid.addEdge(new NmvLink(Integer.toString(w),defaultWeight,defaultTransProb),nodeList.get(indexFrom),nodeList.get(indexTo),EdgeType.DIRECTED);
		}

	}
	public void constructMalikGraph(){
		//get the linklist
		int core=2;
		int n = 5;
		int [][] linkList = Malik.linkList(core*n*2+core*(core-1), 3);
		double defaultWeight = 1;
		double defaultTransProb = 1;
		myGraphHub = new DirectedSparseMultigraph<NmvNode, NmvLink>();
		//create the node list
		ArrayList<NmvNode> nodeList = new ArrayList<NmvNode>();
		for (int m =1; m<=core*n+core;m++){
			NmvNode currentNode = new NmvNode("","", 0, 0);
			currentNode = new NmvNode(Integer.toString(m-1),Integer.toString(m),0,0);
			nodeList.add(m-1, currentNode);
		}
		int indexTo;
		int indexFrom;
		for (int w = 0; w<linkList.length;w++){
			indexFrom =linkList[w][0] -1;
			indexTo = linkList[w][1] -1;
			myGraphHub.addEdge(new NmvLink(Integer.toString(w),defaultWeight,defaultTransProb),nodeList.get(indexFrom),nodeList.get(indexTo),EdgeType.DIRECTED);
		}

	}
	public void testFile(DirectedGraph<NmvNode, NmvLink> myGraphT, ArrayList nodeList, String graphTestFile, String nodeTestFile, String linkTestFile) throws FileNotFoundException{
		//THIS WAY OF WRITING IT OUT IS NOT VERY USEFUL RIGHT NOW
		//		try {
		//			File fileNode = new File(nodeTestFile);
		//			FileWriter fw = new FileWriter(fileNode.getAbsoluteFile());
		//		
		//			BufferedWriter bw = new BufferedWriter(fw);
		//			for (int i = 0; i<nodeList.size();i++){
		//				bw.write(myGraphT.getNeighbors(nodeList.get(i)));	
		//				myGraphT.get
		//				
		//			}
		//			
		//			bw.close();
		////			System.out.println("Nodes written to file");
		////			System.out.println("Writing links...");
		//		} catch (IOException e) {
		//			e.printStackTrace();
		//		}
		try {
			File fileNode = new File(graphTestFile);
			FileWriter fw = new FileWriter(fileNode.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(myGraphT.toString());
			bw.close();
			//			System.out.println("Graph written to file");
			//			System.out.println("Writing nodes...");
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			File fileNode = new File(nodeTestFile);
			FileWriter fw = new FileWriter(fileNode.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(myGraphT.getVertices().toString());
			bw.close();
			//			System.out.println("Nodes written to file");
			//			System.out.println("Writing links...");
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			File fileEdge = new File(linkTestFile);
			FileWriter fw = new FileWriter(fileEdge.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(myGraphT.getEdges().toString());
			bw.close();
			//			System.out.println("Edges written to file");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void calcMetrics(DirectedGraph<NmvNode, NmvLink> myGraphCalc, ArrayList<NmvNode> nodeList,LinkedList<NmvLink> linkList, String [][] filenames ){
		SampleNetworkBuilder.writeGraphML(myGraphCalc, filenames[9][0]);

		//Centrality scores
		JungCentrality.calculateAndWriteUnweightedBetweenness(myGraphCalc,filenames[0][0], filenames[1][0],nodeList, linkList);
		JungCentrality.calculateAndWriteUnweightedCloseness(myGraphCalc, filenames[2][0], nodeList);
		JungCentrality.calculateAndWriteUnweightedEigenvector(myGraphCalc, filenames[3][0], nodeList);
		JungCentrality.calculateAndWriteDegreeCentrality(myGraphCalc, filenames[4][0], nodeList, linkList);

		//Clustering
		JungClusters.calculateAndWriteClusteringCoefficient(myGraphCalc, filenames[5][0]);
		JungClusters.calculateAndWriteWeakComponents(myGraphCalc, filenames[6][0]);
		JungClusters.calculateAndWriteTriadicCensus(myGraphCalc, filenames[7][0]);

		//Graph distance
		JungGraphDistance.calculateAndWriteUnweightedDistances(myGraphCalc, filenames[8][0]);
	}
	public static void main(String[] args) throws FileNotFoundException{

		//filenameVector - GridNetwork
		String [][] filenamesGrid = new String [10][2];
		filenamesGrid[0][0]=args[0];
		filenamesGrid[0][1]="nodeBetUnweighted"; //centrality
		filenamesGrid[1][0]=args[1];
		filenamesGrid[1][1]="edgeBetUnweighted"; //centrality
		filenamesGrid[2][0]=args[2];
		filenamesGrid[2][1]="nodeCloseUnweighted"; //centrality
		filenamesGrid[3][0]=args[3];
		filenamesGrid[3][1]="nodeEigenUnweighted"; //centrality
		filenamesGrid[4][0]=args[4];
		filenamesGrid[4][1]="degreeFile"; //centrality
		filenamesGrid[5][0]=args[5];
		filenamesGrid[5][1]="clusterFile"; //clustering measures
		filenamesGrid[6][0]=args[6];
		filenamesGrid[6][1]="weakCompFile"; //clustering measures
		filenamesGrid[7][0]=args[7];
		filenamesGrid[7][1]="triadFile"; //clustering measures
		filenamesGrid[8][0]=args[8];
		filenamesGrid[8][1]="distUnweightedFile"; //distance statistics
		filenamesGrid[9][0]=args[9];
		filenamesGrid[9][1]="graphML";

		//filenameVector - HubNetwork
		String [][] filenamesHub = new String [10][2];
		filenamesHub[0][0]=args[10];
		filenamesHub[0][1]="nodeBetUnweighted"; //centrality
		filenamesHub[1][0]=args[11];
		filenamesHub[1][1]="edgeBetUnweighted"; //centrality
		filenamesHub[2][0]=args[12];
		filenamesHub[2][1]="nodeCloseUnweighted"; //centrality
		filenamesHub[3][0]=args[13];
		filenamesHub[3][1]="nodeEigenUnweighted"; //centrality
		filenamesHub[4][0]=args[14];
		filenamesHub[4][1]="degreeFile"; //centrality
		filenamesHub[5][0]=args[15];
		filenamesHub[5][1]="clusterFile"; //clustering measures
		filenamesHub[6][0]=args[16];
		filenamesHub[6][1]="weakCompFile"; //clustering measures
		filenamesHub[7][0]=args[17];
		filenamesHub[7][1]="triadFile"; //clustering measures
		filenamesHub[8][0]=args[18];
		filenamesHub[8][1]="distUnweightedFile"; //distance statistics
		filenamesHub[9][0]=args[19];
		filenamesHub[9][1]="graphML";

		//general
		//String shortListFile = args[20];

		final GenDirectedNetwork myAppGrid = new GenDirectedNetwork(); //this was made "final" by one of the visualization transformers
		myAppGrid.constructGridGraph();  
		myAppGrid.constructMalikGraph();
		System.out.println("Graphs constructed");
		LinkedList<NmvLink> linkListGrid = new LinkedList<NmvLink>(myAppGrid.myGraphGrid.getEdges());
		ArrayList<NmvNode> nodeListGrid = new ArrayList<NmvNode>(myAppGrid.myGraphGrid.getVertices());
		LinkedList<NmvLink> linkListHub = new LinkedList<NmvLink>(myAppGrid.myGraphHub.getEdges());
		ArrayList<NmvNode> nodeListHub = new ArrayList<NmvNode>(myAppGrid.myGraphHub.getVertices());
		//myAppGrid.testFile(myAppGrid.myGraph,nodeList,"/Users/nadiaviljoen/Documents/PhD_gridNetwork/graphTEST.csv", "/Users/nadiaviljoen/Documents/PhD_gridNetwork/nodeTEST.csv", "/Users/nadiaviljoen/Documents/PhD_gridNetwork/linkTEST.csv");

		//		myAppGrid.calcMetrics(myAppGrid.myGraphGrid,nodeListGrid,linkListGrid,filenamesGrid);
		//		myAppGrid.calcMetrics(myAppGrid.myGraphHub,nodeListHub,linkListHub,filenamesHub);

		//int [][] assocList = LayerGraphs.assocList(linkListGrid, linkListHub, nodeListGrid, nodeListHub);
		String shortListFile;
		String assocFile;
		for (int rep = 0; rep<1000;rep++){
//			shortListFile = "/Users/nadiaviljoen/Documents/workspace/ArticleRegister/GridNetworkFiles/Malik/fixShortestPath/shortListFile";
//			shortListFile =shortListFile.concat(String.valueOf(rep));
//			shortListFile =shortListFile.concat(".csv");
			assocFile = "/Users/nadiaviljoen/Documents/workspace/ArticleRegister/GridNetworkFiles/baseline50x50/assocFile/assocFile";
			assocFile =assocFile.concat(String.valueOf(rep));
			assocFile =assocFile.concat(".csv");
			int [][] assocList = LayerMalik.assocList(myAppGrid.myGraphGrid,linkListGrid, linkListHub, nodeListGrid, nodeListHub);
//			ShortestPath.collectShortest(myAppGrid.myGraphGrid,myAppGrid.myGraphHub,linkListGrid, linkListHub, nodeListGrid, nodeListHub, assocList,shortListFile);
			BufferedWriter bw = IOUtils.getBufferedWriter(assocFile);
			try{
				bw.write("Malik id, Malik index,Grid id, Grid index");
				bw.newLine();
				for (int j =0; j< assocList.length;j++){
					bw.write(String.format("%d,%d,%d,%d\n",assocList[j][0],assocList[j][1],assocList[j][2],assocList[j][3]));
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally{
				try {
					bw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}


		//		int [][] assocList = LayerMalik.assocList(myAppGrid.myGraphGrid,linkListGrid, linkListHub, nodeListGrid, nodeListHub);
		//		ShortestPath.collectShortest(myAppGrid.myGraphGrid,linkListGrid, linkListHub, nodeListGrid, nodeListHub, assocList,shortListFile);


		//		BufferedWriter bw = IOUtils.getBufferedWriter("/Users/nadiaviljoen/Documents/workspace/ArticleRegister/GridNetworkFiles/Malik/TEST3.csv");
		//		try{
		//			bw.write("Malik Node List");
		//			bw.newLine();
		//			bw.write("Malik id, Malik index");
		//			bw.newLine();
		//			for (int y = 0; y<nodeListHub.size();y++){
		//				bw.write(String.format("%s,%d\n",nodeListHub.get(y).getId(),y));
		//			}
		//			bw.newLine();
		//			bw.write("Grid Node List");
		//			bw.newLine();
		//			bw.write("Grid id, Grid index");
		//			bw.newLine();
		//			for (int y = 0; y<nodeListGrid.size();y++){
		//				bw.write(String.format("%s,%d\n",nodeListGrid.get(y).getId(),y));
		//			}
		//			bw.newLine();
		//			bw.write("assocList");
		//			bw.newLine();
		//			bw.write("Malik id, Malik index,Grid id, Grid index");
		//			bw.newLine();
		//			for (int j =0; j< assocList.length;j++){
		//				bw.write(String.format("%d,%d,%d,%d\n",assocList[j][0],assocList[j][1],assocList[j][2],assocList[j][3]));
		//			}
		//		} catch (IOException e) {
		//			e.printStackTrace();
		////			LOG.error("Oops, couldn't write to file.");
		//		} finally{
		//			try {
		//				bw.close();
		//			} catch (IOException e) {
		//				e.printStackTrace();
		////				LOG.error("Oops, couldn't close");
		//			}
		//		}





	}
}
