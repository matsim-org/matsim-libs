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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;

import org.matsim.core.utils.io.IOUtils;

import playground.nmviljoen.network.JungCentrality;
import playground.nmviljoen.network.JungClusters;
import playground.nmviljoen.network.JungGraphDistance;
import playground.nmviljoen.network.NmvLink;
import playground.nmviljoen.network.NmvNode;
import playground.nmviljoen.network.salience.SampleNetworkBuilder;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;

public class GhostNetwork {
	/** MyNode and MyLink are classes at the end of the script*/
	DirectedGraph<NmvNode, NmvLink> myGraphGhost; 
	public void constructGhostGraph(){
		//get the linklist
		int[][] linkList= GhostLinkList.linkList();
		
		//write out full link list for testing
		BufferedWriter bf = IOUtils.getBufferedWriter("/Users/nadiaviljoen/Documents/PhD_gridNetwork/GhostProtocol/TestingFullPath/allEdges.csv");
		try{
			bf.write("From,To");
			bf.newLine();
			for (int j =0; j< linkList.length;j++){
				bf.write(String.format("%d,%d\n",linkList[j][0],linkList[j][1]));
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally{
			try {
				bf.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		//now create a weighted linkList
		int[][] weightedLinkListTemp = new int[linkList.length][3];
		int count=1;
		weightedLinkListTemp[0][0] = linkList[0][0];
		weightedLinkListTemp[0][1] = linkList[0][1];
		weightedLinkListTemp[0][2] = 1;
		boolean flag = false; //it is a new link
		for (int p =1; p<linkList.length;p++){
			flag = false;
			for (int s=0; s<count;s++){
				if((linkList[p][0]==weightedLinkListTemp[s][0])&&(linkList[p][1]==weightedLinkListTemp[s][1])){
					weightedLinkListTemp[s][2]=weightedLinkListTemp[s][2]+1;//increment the weight
					flag = true;
					break;
				}
			}
			if (!flag){ //then add a new link
				weightedLinkListTemp[count][0] = linkList[p][0];
				weightedLinkListTemp[count][1] = linkList[p][1];
				weightedLinkListTemp[count][2] = 1;
				count++;
			}
		}
		
		int[][] weightedLinkListFin = new int[count][3];
		for (int f=0;f<count;f++){
			weightedLinkListFin[f][0]=weightedLinkListTemp[f][0];
			weightedLinkListFin[f][1]=weightedLinkListTemp[f][1];
			weightedLinkListFin[f][2]=weightedLinkListTemp[f][2];
		}
		
		//write weighted edgelist to file - tested it, it creates the list correctly
//		BufferedWriter be = IOUtils.getBufferedWriter("/Users/nadiaviljoen/Documents/PhD_gridNetwork/GhostProtocol/TestingFullPath/uniqueEdges.csv");
//		try{
//			be.write("From,To,Weight");
//			be.newLine();
//			for (int j =0; j< weightedLinkListFin.length;j++){
//				be.write(String.format("%d,%d,%d\n",weightedLinkListFin[j][0],weightedLinkListFin[j][1],weightedLinkListFin[j][2]));
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		} finally{
//			try {
//				be.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
		
		
		double defaultTransProb = 1;
		
		myGraphGhost = new DirectedSparseMultigraph<NmvNode, NmvLink>();
		//create the node list
		int [] uniqueNodesTemp = new int[linkList.length];
		count=0;
		flag = true;
		int cur1=0;
		int cur2=0;
		int r=0;
		uniqueNodesTemp[0]=linkList[0][0];
		for (int y = 0; y<linkList.length;y++){
			cur1=linkList[y][0];
			cur2=linkList[y][1];
			r=0;
			flag=true;
			while ((flag)&&(r<=count)){
				if(uniqueNodesTemp[r]==cur1){
					flag=false;
				}else r++;
			}	
			if (flag){
				count++;
				uniqueNodesTemp[count]=cur1;
			}
			r=0;
			flag = true;
			while ((flag)&&(r<=count)){
				if(uniqueNodesTemp[r]==cur2){
					flag=false;
				}else r++;
			}	
			if (flag){
				count++;
				uniqueNodesTemp[count]=cur2;
			}
		}
		int[] uniqueNodes = new int[count+1];
		for (int w =0; w<=count;w++){
			uniqueNodes[w]=uniqueNodesTemp[w];
		}
		
		ArrayList<NmvNode> nodeList = new ArrayList<NmvNode>();
		//what you have in linkList is the grid graph ID not the index
		for (int m =0; m<uniqueNodes.length;m++){
			NmvNode currentNode = new NmvNode("","", 0, 0);
			currentNode = new NmvNode(Integer.toString(m),Integer.toString(uniqueNodes[m]),0,0);
			nodeList.add(m, currentNode);
		}
		int indexTo = 0;
		int indexFrom = 0;
		for (int w = 0; w<weightedLinkListFin.length;w++){
			for (int q =0;q<uniqueNodes.length;q++){
				if(uniqueNodes[q]==weightedLinkListFin[w][0]){
					indexFrom=q;
					break;
				}
			}
			for (int p =0;p<uniqueNodes.length;p++){
				if(uniqueNodes[p]==weightedLinkListFin[w][1]){
					indexTo=p;
					break;
				}
			}
			myGraphGhost.addEdge(new NmvLink(Integer.toString(w),weightedLinkListFin[w][2],defaultTransProb),nodeList.get(indexFrom),nodeList.get(indexTo),EdgeType.DIRECTED);
		}

	}
	public static void calcMetrics(DirectedGraph<NmvNode, NmvLink> myGraphCalc, ArrayList<NmvNode> nodeList,LinkedList<NmvLink> linkList, String [] filenames ){

		//Centrality scores
		JungCentrality.calculateAndWriteUnweightedBetweenness(myGraphCalc,filenames[1], filenames[2],nodeList, linkList);
		JungCentrality.calculateAndWriteDegreeCentrality(myGraphCalc, filenames[0], nodeList, linkList);

		//Clustering
		JungClusters.calculateAndWriteClusteringCoefficient(myGraphCalc, filenames[3]);
		JungClusters.calculateAndWriteWeakComponents(myGraphCalc, filenames[4]);
		JungClusters.calculateAndWriteTriadicCensus(myGraphCalc, filenames[5]);

		//Graph distance
		JungGraphDistance.calculateAndWriteUnweightedDistances(myGraphCalc, filenames[6]);
	}
	public static void main(String[] args) throws FileNotFoundException{
		
		final GhostNetwork myAppGrid = new GhostNetwork();
		myAppGrid.constructGhostGraph();
		LinkedList<NmvLink> linkListGhost = new LinkedList<NmvLink>(myAppGrid.myGraphGhost.getEdges());
		ArrayList<NmvNode> nodeListGhost = new ArrayList<NmvNode>(myAppGrid.myGraphGhost.getVertices());
		String [] filenames = new String [7];
		filenames[0]= "/Users/nadiaviljoen/Documents/PhD_gridNetwork/GhostProtocol/Experiment1/degreeMetrics/degreeFile_Ghost.csv";
		filenames[1]= "/Users/nadiaviljoen/Documents/PhD_gridNetwork/GhostProtocol/Experiment1/degreeMetrics/nodeBetUnweighted_Ghost.csv";
		filenames[2]= "/Users/nadiaviljoen/Documents/PhD_gridNetwork/GhostProtocol/Experiment1/degreeMetrics/edgeBetUnweighted_Ghost.csv";
		filenames[3]= "/Users/nadiaviljoen/Documents/PhD_gridNetwork/GhostProtocol/Experiment1/clusterMetrics/clusterFile_Ghost.csv";
		filenames[4]= "/Users/nadiaviljoen/Documents/PhD_gridNetwork/GhostProtocol/Experiment1/clusterMetrics/weakCompFile_Ghost.csv";
		filenames[5]= "/Users/nadiaviljoen/Documents/PhD_gridNetwork/GhostProtocol/Experiment1/clusterMetrics/triadFile_Ghost.csv";
		filenames[6]= "/Users/nadiaviljoen/Documents/PhD_gridNetwork/GhostProtocol/Experiment1/clusterMetrics/distUnweightedFile_Ghost.csv";
		calcMetrics(myAppGrid.myGraphGhost,nodeListGhost,linkListGhost,filenames);
//		JungCentrality.calculateAndWriteDegreeCentrality(myAppGrid.myGraphGhost, "/Users/nadiaviljoen/Documents/PhD_gridNetwork/GhostProtocol/TestingFullPath/degree_TEST.csv", nodeListGhost, linkListGhost);
	}

}
