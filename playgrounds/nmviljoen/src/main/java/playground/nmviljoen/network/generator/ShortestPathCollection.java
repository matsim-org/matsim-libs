package playground.nmviljoen.network.generator;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import playground.nmviljoen.network.NmvLink;
import playground.nmviljoen.network.NmvNode;
import playground.nmviljoen.network.generator.Grid;


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

public class ShortestPathCollection {
	private static final boolean TRUE = false;
	/** MyNode and MyLink are classes at the end of the script*/
	DirectedGraph<NmvNode, NmvLink> myGraphGrid; 
	DirectedGraph<NmvNode, NmvLink> myGraphHub;

	/** Creates a new instance of BasicDirectedGraph */
	public ShortestPathCollection() {       
	}

	public void constructGridGraph(){
		//get the linklist
		int row=5;
		int col = 5;
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

	public static int [][] pathBuilder(DirectedGraph<NmvNode, NmvLink> myGraphGrid,ArrayList<NmvNode> gridNodes, int s, int t){
//		System.out.println("Source "+gridNodes.get(s).getId());
//		System.out.println("Target "+gridNodes.get(t).getId());

		DijkstraShortestPath<NmvNode, NmvLink> gPath = new DijkstraShortestPath<NmvNode, NmvLink>(myGraphGrid);
		List <NmvLink> shortestPathGrid = gPath.getPath(gridNodes.get(s),gridNodes.get(t));
//		System.out.println("Path length"+shortestPathGrid.size());
		int numSteps = shortestPathGrid.size();
		int count = 0;
		int nonzeroSize=0;
		int [][] pathCollectInt = new int[100000][numSteps+1];
		pathCollectInt[0][0] = s;

		while (count<=numSteps){
			int [][] NEWpathCollect = new int[100000][numSteps+1];
			int countRows = 0;
			int cycle =0;
			while (pathCollectInt[cycle][0]==s){ //while there is still a row in pathCollect with entries

				int currentNode = pathCollectInt[cycle][count];

				Collection <NmvNode> success = new LinkedList<NmvNode>(myGraphGrid.getSuccessors(gridNodes.get(currentNode)));
				//need to prevent from going back

				for (NmvNode sucNode:success){
					boolean contin = false;
					List <NmvLink> path1 = gPath.getPath(gridNodes.get(s),sucNode);
					List <NmvLink> path2 = gPath.getPath(sucNode,gridNodes.get(t));

					//make sure it doesn't move further away
					//path2 must now be shorter than it was in the previous iteration

					List <NmvLink> path2Prev = gPath.getPath(gridNodes.get(currentNode),gridNodes.get(t));
					if (numSteps==(path1.size()+path2.size())&&(path2.size()<path2Prev.size())){
						contin = true;
					}

				
				
					if (contin){
						for (int k=0;k<=count;k++){
							NEWpathCollect[countRows][k]=pathCollectInt[cycle][k]; //populate row in NEW matrix with existing values
						}
						NEWpathCollect[countRows][count+1] = gridNodes.indexOf(sucNode); //add the successor node
						countRows ++;
						nonzeroSize=countRows;
					}

				}	
				cycle++;
			}


			//write out both matrices to check before overwriting

//			System.out.println("pathCollect");
//			int check1 = 0;
//			while (pathCollectInt[check1][0]==s){
//				for (int r = 0; r<=count;r++){
//					System.out.print(gridNodes.get(pathCollectInt[check1][r]).getId()+" ");
//				}
//				check1++;
//				System.out.println();
//			}
//
//			System.out.println("NEWpathCollect");
//			int check2 = 0;
//			while (NEWpathCollect[check2][0]==s){
//				for (int r = 0; r<=count+1;r++){
//					System.out.print(gridNodes.get(NEWpathCollect[check2][r]).getId()+" ");
//				}
//				check2++;
//				System.out.println();
//			}

			if (NEWpathCollect[0][0]!=0){
				pathCollectInt=NEWpathCollect;
			}
			count++;
			
//			System.out.println("count "+cycle);
//			System.out.println("countRows "+ countRows);
//			System.out.println("cycle "+cycle);
			
		}
//		System.out.println("pathCollectINT");
//		for (int t1 =0;t1<100;t1++){
//			for (int t2=0; t2<numSteps+1;t2++){
//				System.out.print(pathCollectInt[t1][t2]+" ");
////				System.out.print(gridNodes.get(pathCollectFin[t1][t2]).getId()+" ");
//			}
//			System.out.println();
//		}
//		System.out.println("NonZeroSize "+nonzeroSize);
		int [][] pathCollectFin = new int[nonzeroSize][numSteps+1];
		for (int t1 =0;t1<nonzeroSize;t1++){
			for (int t2=0; t2<numSteps+1;t2++){
				pathCollectFin[t1][t2]=pathCollectInt[t1][t2];
			}
		}
//		System.out.println("pathCollectFIN");
//		for (int t1 =0;t1<nonzeroSize;t1++){
//			for (int t2=0; t2<numSteps+1;t2++){
////				System.out.print(pathCollectFin[t1][t2]+" ");
//				System.out.print(gridNodes.get(pathCollectFin[t1][t2]).getId()+" ");
//			}
//			System.out.println();
//		}
	
		return pathCollectFin;

	}
	public static void main(String[] args) throws FileNotFoundException{

		final ShortestPathCollection myAppGrid = new ShortestPathCollection(); //this was made "final" by one of the visualization transformers
		myAppGrid.constructGridGraph(); 
		//		LinkedList<NmvLink> linkListGrid = new LinkedList<NmvLink>(myAppGrid.myGraphGrid.getEdges());
		ArrayList<NmvNode> nodeListGrid = new ArrayList<NmvNode>(myAppGrid.myGraphGrid.getVertices());
		int [][] setOfPaths = pathBuilder(myAppGrid.myGraphGrid,nodeListGrid,3, 11);


	}

}