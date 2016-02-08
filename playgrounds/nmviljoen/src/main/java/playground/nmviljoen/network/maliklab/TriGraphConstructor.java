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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.matsim.core.utils.io.IOUtils;

import playground.nmviljoen.network.JungCentrality;
import playground.nmviljoen.network.generator.GenDirectedNetwork;
import playground.nmviljoen.network.JungClusters;
import playground.nmviljoen.network.JungGraphDistance;
import playground.nmviljoen.network.NmvLink;
import playground.nmviljoen.network.salience.SampleNetworkBuilder;
import playground.nmviljoen.network.NmvNode;
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;

public class TriGraphConstructor {
	static DirectedGraph<NmvNode, NmvLink> myGraphGrid; 
	static DirectedGraph<NmvNode, NmvLink> myGraphMalik;
	static DirectedGraph<NmvNode, NmvLink> myGraphGhost;
	public TriGraphConstructor() {       
	}
	public static DirectedGraph<NmvNode, NmvLink> constructGridGraphSim(String path, int SimDim){
		//read in the linklist
		String filename = path+"GridLinkList.csv";
		BufferedReader br1 = null;
		String lineNode = "";
		int counter =0;
		int [][] linkList = new int[SimDim][3]; 
		try {
			br1 = new BufferedReader(new FileReader(filename));
						String heading=br1.readLine();
						System.out.println("Throwing out the heading "+heading);
			while ((lineNode = br1.readLine())!=null) {
				String[] pathData = lineNode.split(",");
				for (int i =1; i<pathData.length;i++){ //change this back to int i =1; i<pathData.length;i++
					linkList[counter][i-1] = Integer.parseInt(pathData[i]);			
				}
//				for (int i =0; i<pathData.length;i++){ //change this back to int i =1; i<pathData.length;i++
//					linkList[counter][i] = Integer.parseInt(pathData[i]);			
//				}
				counter++;
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br1 != null) {
				try {
					br1.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		double defaultWeight = 1;
		double defaultTransProb = 1;
		myGraphGrid = new DirectedSparseMultigraph<NmvNode, NmvLink>();
		//create the node list
		int row=10;
		int col = 10;
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
		return myGraphGrid;

	}

	public static DirectedGraph<NmvNode, NmvLink> constructGridGraph(String path){
		//get the linklist
		int row=10;
		int col = 10;
		String filename = path+"GridLinkList.csv";
		int [][] linkList = GridLinkList.linkList(row, col, filename);
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
		return myGraphGrid;

	}
	public static DirectedGraph<NmvNode, NmvLink> constructMalikGraph(String path){
		//get the linklist
		int core=2;
		int n = 5;
		String filename = path+"MalikLinkList.csv";
		int [][] linkList = MalikLinkList.linkList(core*n*2+core*(core-1), 3,filename);
		double defaultWeight = 1;
		double defaultTransProb = 1;
		myGraphMalik = new DirectedSparseMultigraph<NmvNode, NmvLink>();
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
			myGraphMalik.addEdge(new NmvLink(Integer.toString(w),defaultWeight,defaultTransProb),nodeList.get(indexFrom),nodeList.get(indexTo),EdgeType.DIRECTED);
		}
		return myGraphMalik;

	}

	public static DirectedGraph<NmvNode, NmvLink> constructGhostGraph(String path, int[][]assocList, int fullPathSize, int segSize, int segPathLength){
		ArrayList<NmvNode> nodeListGrid = new ArrayList<NmvNode>(TriGraphConstructor.myGraphGrid.getVertices());
		ArrayList<NmvLink> linkListMalik = new ArrayList<NmvLink>(TriGraphConstructor.myGraphMalik.getEdges());
		ArrayList<NmvNode> nodeListMalik = new ArrayList<NmvNode>(TriGraphConstructor.myGraphMalik.getVertices());

		//Create the shortest path sets for each link in the Malik Graph

		//The 0= FROM(MalikId), 1 = TO(MalikId), 2 = FROM(GridId), 3 = TO(GridId), 4 = # of shortest paths between the two, the rest is the shortest path Grid ids
		int [][] logicalPathCollect = new int[50000][segPathLength+5];
		for (int[] row: logicalPathCollect){
			Arrays.fill(row, 999);
		}
		int rowMarker=0;
		Collection<NmvLink> linkTrack =null;
		Iterator <NmvLink> LinkIterator = linkListMalik.iterator();
		Collection<NmvNode> nodeTrack =null;
		while (LinkIterator.hasNext()){
			nodeTrack=myGraphMalik.getIncidentVertices(LinkIterator.next()); //gets incident vertices to a link
			if(nodeTrack.size()!=2){
				System.out.println("Incident vertices !=2:  "+nodeTrack.size());
			}


			Iterator <NmvNode> NodeIterator = nodeTrack.iterator();

			logicalPathCollect[rowMarker][0]=Integer.parseInt(NodeIterator.next().getId());//place Malik id of incident vertices in array
			//			System.out.println("FROM= "+logicalPathCollect[rowMarker][0]);
			logicalPathCollect[rowMarker][1]=Integer.parseInt(NodeIterator.next().getId());//place Malik id of incident vertices in array
			//			System.out.println("TO= "+logicalPathCollect[rowMarker][1]);
			int source=0;
			int target=0;
			for (int s=0;s<assocList.length;s++){ //find the matching grid ids and put it in the array
				if (logicalPathCollect[rowMarker][0]==assocList[s][0]) {
					logicalPathCollect[rowMarker][2]=assocList[s][2];
					source=s;//position in the assocList for the source node
					break;
				}
			}
			for (int t=0;t<assocList.length;t++){
				if (logicalPathCollect[rowMarker][1]==assocList[t][0]) {
					logicalPathCollect[rowMarker][3]=assocList[t][2];
					target=t;//position in the assocList for the target node
					break;
				}
			}
			//						System.out.println(String.format("%d,%d,%d,%d\n", logicalPathCollect[rowMarker][0],logicalPathCollect[rowMarker][1],logicalPathCollect[rowMarker][2],logicalPathCollect[rowMarker][3]));

			//this call gives you all the shortest paths between two connected malik nodes
//			System.out.println("Source Grid ID: "+assocList[source][3]+" Target Grid ID: "+assocList[target][3]);
			int [][] interimPathCollect = pathBuilder(myGraphGrid, nodeListGrid,assocList[source][3], assocList[target][3]);
			
//			System.out.println(interimPathCollect.length);
			//now add it to the logicalPathCollect

			int mFId=logicalPathCollect[rowMarker][0];
			int mTId=logicalPathCollect[rowMarker][1];
			int gFId=logicalPathCollect[rowMarker][2];
			int gTId=logicalPathCollect[rowMarker][3];
			//						System.out.println(String.format("%d,%d,%d,%d\n", mFId,mTId,gFId,gTId));
			for(int i1=0;i1<interimPathCollect.length;i1++){
				logicalPathCollect[rowMarker][0]=mFId;
				logicalPathCollect[rowMarker][1]=mTId;
				logicalPathCollect[rowMarker][2]=gFId;
				logicalPathCollect[rowMarker][3]=gTId;
				logicalPathCollect[rowMarker][4]=interimPathCollect[0].length;
				for (int r=1;r<=interimPathCollect[0].length;r++){
					logicalPathCollect[rowMarker][4+r]=interimPathCollect[i1][r-1];
				}//add nodes to path
//				System.out.println(String.format("%d,%d,%d,%d,%d,%d,%d\n",logicalPathCollect[rowMarker][0],logicalPathCollect[rowMarker][1],logicalPathCollect[rowMarker][2],logicalPathCollect[rowMarker][3],logicalPathCollect[rowMarker][4],logicalPathCollect[rowMarker][5],logicalPathCollect[rowMarker][4+interimPathCollect[0].length]));
				rowMarker++;
			}//while there are still paths in interimPathCollect


		}//linklist iterator loop end



		//write to file
		String filenameL = path+"segmentPaths.csv";
		BufferedWriter bl = IOUtils.getBufferedWriter(filenameL);
		try{
			for (int j =0; j< rowMarker;j++){
				for (int r=0;r<logicalPathCollect[0].length;r++){
					if (logicalPathCollect[j][r]!=999){
						if(r<=4){

							bl.write(logicalPathCollect[j][r]+" ");
						}else bl.write(nodeListGrid.get(logicalPathCollect[j][r]).getId()+" ");
					}	
				}
				bl.newLine();	
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally{
			try {
				bl.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		System.out.println("Segment paths written to "+filenameL);

		//Now you have all the shortest paths for all the directly connected Malik links. Put them together to build the whole set.

		//		Create set of all shortest paths for Ghost Network
		int [][] fullPathCollect = new int[fullPathSize][55];
		for (int[] row: fullPathCollect){
			Arrays.fill(row, 999);
		}
		DijkstraShortestPath<NmvNode, NmvLink> mPath = new DijkstraShortestPath<NmvNode, NmvLink>(myGraphMalik);
		//		int marker =0;
		//		int test =0;
		//for each node pair in the assocList - remember that shortest path sets may eventually not be symmetric and therefore you have to do all


		int rows=0;
		for (int y = 0; y<assocList.length;y++){
			for (int x = 0; x<assocList.length;x++){

				//		for (int y = 0; y<3;y++){ //test
				//			for (int x = 1; x<8;x++){ //test
				if (x!=y){

					int[][] segment1 = new int[segSize][segPathLength];
					for (int[] row: segment1){
						Arrays.fill(row, 999);
					}
					int seg1C=0;
					int[][] segment2 = new int[segSize][segPathLength];
					for (int[] row: segment2){
						Arrays.fill(row, 999);
					}
					int seg2C=0;
					int[][] segment3 = new int[segSize][segPathLength];
					for (int[] row: segment3){
						Arrays.fill(row, 999);
					}
					int seg3C=0;

					//get the logical shortest path first
					List <NmvLink> shortestPathMalik = mPath.getPath(nodeListMalik.get(assocList[y][1]), nodeListMalik.get(assocList[x][1]));
					NmvNode FROM = null;
					NmvNode TO = null;
					//decide which loop depending on the logical path length
					if(shortestPathMalik.size()==1){
						Iterator<NmvLink> PathIterator = shortestPathMalik.iterator();
						nodeTrack=myGraphMalik.getIncidentVertices(PathIterator.next()); //gets incident vertices to a link
						Iterator <NmvNode> NodeIterator = nodeTrack.iterator();
						//if only one leg
						//populate the segment from the LogicalPathCollect
						FROM = NodeIterator.next();
						TO = NodeIterator.next();
						//first find the paths and populate segment1
						int w=0;
						for(int q=0;q<rowMarker;q++){
							if ((logicalPathCollect[q][0]==Integer.parseInt(FROM.getId()))&& (logicalPathCollect[q][1]==Integer.parseInt(TO.getId()))){
								//								System.out.println("In here 1");
								w=0;
								while (logicalPathCollect[q][5+w]!=999){
									segment1[seg1C][w]=logicalPathCollect[q][5+w];
									w++;

								}
								seg1C++;
							}
						}//now all the paths are transferred to segment 1

						//						for(int q=0;q<seg1C;q++){
						//							System.out.println(String.format("%s,%s,%s\n",nodeListGrid.get(segment1[q][0]).getId(),nodeListGrid.get(segment1[q][1]).getId(),nodeListGrid.get(segment1[q][2]).getId()));
						//						}
						//transfer to fullPathCollect
						int v=0;
						for (int u=0;u<seg1C;u++){
							v=0;
							while (segment1[u][v]!=999){
								fullPathCollect[rows+u][v]=segment1[u][v];
								v++;
							}
						}
						rows=rows+seg1C;
						//						System.out.println("Fullpath rows: "+rows);

						//						for (int q=0;q<seg1C;q++){
						//							System.out.print(String.format("%s,%s,%s\n",nodeListGrid.get(fullPathCollect[q][0]).getId(),nodeListGrid.get(fullPathCollect[q][1]).getId(),nodeListGrid.get(fullPathCollect[q][2]).getId()));
						//						}

					}else if(shortestPathMalik.size()==2){
						//if two legs
						Iterator<NmvLink> PathIterator = shortestPathMalik.iterator();
						//FIRST SEGMENT
						nodeTrack=myGraphMalik.getIncidentVertices(PathIterator.next());
						Iterator <NmvNode> NodeIterator = nodeTrack.iterator();
						//populate the segment from the LogicalPathCollect
						FROM = NodeIterator.next();
						TO = NodeIterator.next();
						//first find the paths and populate segment1
						int w=0;
						for(int q=0;q<rowMarker;q++){
							if ((logicalPathCollect[q][0]==Integer.parseInt(FROM.getId()))&& (logicalPathCollect[q][1]==Integer.parseInt(TO.getId()))){
								w=0;
								while (logicalPathCollect[q][5+w]!=999){
									segment1[seg1C][w]=logicalPathCollect[q][5+w];
									w++;

								}
								seg1C++;
							}
						}//now all the paths are transferred to segment 1
						//						System.out.println("Segment 1");
						//						for(int q=0;q<seg1C;q++){
						//							System.out.print(String.format("%s,%s,%s\n",nodeListGrid.get(segment1[q][0]).getId(),nodeListGrid.get(segment1[q][1]).getId(),nodeListGrid.get(segment1[q][2]).getId()));
						//						}

						//SECOND SEGMENT
						nodeTrack=myGraphMalik.getIncidentVertices(PathIterator.next());

						NodeIterator = nodeTrack.iterator();
						//populate the segment from the LogicalPathCollect
						FROM = NodeIterator.next();
						TO = NodeIterator.next();
						//first find the paths and populate segment2
						w=0;
						for(int q=0;q<rowMarker;q++){
							if ((logicalPathCollect[q][0]==Integer.parseInt(FROM.getId()))&& (logicalPathCollect[q][1]==Integer.parseInt(TO.getId()))){

								w=0;
								while (logicalPathCollect[q][5+w]!=999){
									segment2[seg2C][w]=logicalPathCollect[q][5+w];
									w++;

								}
								seg2C++;
							}
						}//now all the paths are transferred to segment 2
						//						System.out.println("Segment 2");
						//						for(int q=0;q<seg2C;q++){
						//							System.out.print(String.format("%s,%s,%s\n",nodeListGrid.get(segment2[q][0]).getId(),nodeListGrid.get(segment2[q][1]).getId(),nodeListGrid.get(segment2[q][2]).getId()));
						//						}


						//transfer to fullPathCollect
						int v=0;
						int v1=1;

						for (int u=0;u<seg1C;u++){
							for (int r=0;r<seg2C;r++){
								v=0;
								v1=1;
								while (segment1[u][v]!=999){ //add first segment
									fullPathCollect[rows][v]=segment1[u][v];
									v++;
								}
								while (segment2[r][v1]!=999){ //add second segment
									fullPathCollect[rows][v]=segment2[r][v1];
									v++;
									v1++;
								}
								rows++;
							}
						}
						//						System.out.println("Fullpath rows: "+rows);

					}else if(shortestPathMalik.size()==3){
						Iterator<NmvLink> PathIterator = shortestPathMalik.iterator();

						//FIRST SEGMENT
						nodeTrack=myGraphMalik.getIncidentVertices(PathIterator.next());
						Iterator <NmvNode> NodeIterator = nodeTrack.iterator();

						//populate the segment from the LogicalPathCollect
						FROM = NodeIterator.next();
						TO = NodeIterator.next();
						//first find the paths and populate segment1
						int w=0;
						for(int q=0;q<rowMarker;q++){
							if ((logicalPathCollect[q][0]==Integer.parseInt(FROM.getId()))&& (logicalPathCollect[q][1]==Integer.parseInt(TO.getId()))){
								w=0;
								while (logicalPathCollect[q][5+w]!=999){
									segment1[seg1C][w]=logicalPathCollect[q][5+w];
									w++;

								}
								seg1C++;
							}
						}//now all the paths are transferred to segment 1

						//SECOND SEGMENT
						nodeTrack=myGraphMalik.getIncidentVertices(PathIterator.next());
						NodeIterator = nodeTrack.iterator();
						//populate the segment from the LogicalPathCollect
						FROM = NodeIterator.next();
						TO = NodeIterator.next();
						//first find the paths and populate segment2
						w=0;
						for(int q=0;q<rowMarker;q++){
							if ((logicalPathCollect[q][0]==Integer.parseInt(FROM.getId()))&& (logicalPathCollect[q][1]==Integer.parseInt(TO.getId()))){
								w=0;
								while (logicalPathCollect[q][5+w]!=999){
									segment2[seg2C][w]=logicalPathCollect[q][5+w];
									w++;

								}
								seg2C++;
							}
						}//now all the paths are transferred to segment 2

						//THIRD SEGMENT
						nodeTrack=myGraphMalik.getIncidentVertices(PathIterator.next());
						NodeIterator = nodeTrack.iterator();
						//populate the segment from the LogicalPathCollect
						FROM = NodeIterator.next();
						TO = NodeIterator.next();
						//first find the paths and populate segment3
						w=0;
						for(int q=0;q<rowMarker;q++){
							if ((logicalPathCollect[q][0]==Integer.parseInt(FROM.getId()))&& (logicalPathCollect[q][1]==Integer.parseInt(TO.getId()))){
								w=0;
								while (logicalPathCollect[q][5+w]!=999){
									segment3[seg3C][w]=logicalPathCollect[q][5+w];
									w++;

								}
								seg3C++;
							}
						}//now all the paths are transferred to segment 3

						//transfer to fullPathCollect
						int v=0;
						int v1=1;
						int v2=1;

						for (int u=0;u<seg1C;u++){
							for (int r=0;r<seg2C;r++){
								for (int p=0; p<seg3C;p++){
									v=0;
									v1=1;
									v2=1;
									while (segment1[u][v]!=999){ //add first segment
										fullPathCollect[rows][v]=segment1[u][v];
										v++;
									}
									while (segment2[r][v1]!=999){ //add second segment
										fullPathCollect[rows][v]=segment2[r][v1];
										v++;
										v1++;
									}
									while (segment3[p][v2]!=999){ //add third segment
										fullPathCollect[rows][v]=segment3[p][v2];
										v++;
										v2++;
									}
									rows++;
								}
							}
						}


					}else System.out.println("YOUR LOGICAL PATH HAS MORE THAN THREE SEGMENTS");

					//				int [][] interimPathCollect = pathBuilder(myGraphGrid, nodeListGrid,assocList[y][3], assocList[x][3]);
					//				//					int [][] interimPathCollect = pathBuilder(myGraphGrid, nodeListGrid,assocList[1][3], assocList[2][3]);
					//				//insert into fullPathCollect
					//				for (int u = 0; u<interimPathCollect.length;u++){
					//					for (int g=0;g<interimPathCollect[0].length;g++){
					//						fullPathCollect[marker][g]=interimPathCollect[u][g];
					//					}	
					//					marker++;
					//
					//				}
					//				test=test+interimPathCollect.length;
				}
			}
		}
		//write out full path set (on screen)


		//write fullPathCollect out
		String filename = path+"shortestPathSet.csv";
		BufferedWriter bf = IOUtils.getBufferedWriter(filename);
		try{
			for (int j =0; j< rows;j++){
				for (int r=0;r<fullPathCollect[0].length;r++){
					if (fullPathCollect[j][r]!=999){
						bf.write(nodeListGrid.get(fullPathCollect[j][r]).getId()+" ");
					}	
				}
				bf.newLine();	
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
		System.out.println("Full path set written to file "+filename);

		//trim fullpathset
		int [][] fullPathTrim = new int[rows][55];
		for (int[] row: fullPathTrim){
			Arrays.fill(row, 999);
		}
		for (int j =0; j< rows;j++){
			for (int t=0;t<55;t++){
				if(fullPathCollect[j][t]!=999){
					fullPathTrim[j][t]=Integer.parseInt(nodeListGrid.get(fullPathCollect[j][t]).getId());
				}

			}
		}

		//				System.out.println("fullPathTrim.length = "+fullPathTrim.length);
		//				System.out.println();
		//				for (int y = 0;y<5;y++){
		//					System.out.println("# of entries"+fullPathTrim[y].length);
		//					for (int r=0;r<21;r++){
		//						System.out.print(fullPathTrim[y][r]+" ");
		//					}
		//					System.out.println();
		//
		//				}

		String filename2 = path+"shortestPathSetTRIM.csv";
		BufferedWriter bt = IOUtils.getBufferedWriter(filename2);
		try{
			for (int j =0; j< rows;j++){
				for (int r=0;r<fullPathTrim[0].length;r++){
					if (fullPathTrim[j][r]!=999){
						bt.write(fullPathTrim[j][r]+" ");
					}	
				}
				bt.newLine();	
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally{
			try {
				bt.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		System.out.println("Full path set TRIM written to file "+filename2);


		//get the linklist
		//		int[][] linkList= GhostLinkList.linkList(path, fullPathTrim); 
		int[][] linkList= GhostLinkList.linkList(path, rows); 

		double defaultTransProb = 1;

		myGraphGhost = new DirectedSparseMultigraph<NmvNode, NmvLink>();
		//create the node list
		int [] uniqueNodesTemp = new int[linkList.length*2];
		int count=0;
		boolean flag = true;
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
		for (int w = 0; w<linkList.length;w++){
			for (int q =0;q<uniqueNodes.length;q++){
				if(uniqueNodes[q]==linkList[w][0]){
					indexFrom=q;
					break;
				}
			}
			for (int p =0;p<uniqueNodes.length;p++){
				if(uniqueNodes[p]==linkList[w][1]){
					indexTo=p;
					break;
				}
			}
			myGraphGhost.addEdge(new NmvLink(Integer.toString(w),linkList[w][2],defaultTransProb),nodeList.get(indexFrom),nodeList.get(indexTo),EdgeType.DIRECTED);
		}
		return myGraphGhost;

	}


	public static int [][] pathBuilder(DirectedGraph<NmvNode, NmvLink> myGraphGrid,ArrayList<NmvNode> gridNodes, int s, int t){
		//				System.out.println("Source "+gridNodes.get(s).getId());
		//				System.out.println("Target "+gridNodes.get(t).getId());

		DijkstraShortestPath<NmvNode, NmvLink> gPath = new DijkstraShortestPath<NmvNode, NmvLink>(myGraphGrid);
		List <NmvLink> shortestPathGrid = gPath.getPath(gridNodes.get(s),gridNodes.get(t));
//				System.out.println("Path length"+shortestPathGrid.size());
		int numSteps = shortestPathGrid.size();
		int count = 0;
		int nonzeroSize=0;
		int [][] pathCollectInt = new int[500000][numSteps+1];
		for (int[] row: pathCollectInt){
			Arrays.fill(row, 999);
		}
		pathCollectInt[0][0] = s;

		while (count<=numSteps){
			int [][] NEWpathCollect = new int[50000][numSteps+1];
			for (int[] row: NEWpathCollect){
				Arrays.fill(row, 999);
			}

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
				//				System.out.println("NEWpathCollect");
				//				int check2 = 0;
				//				while (NEWpathCollect[check2][0]==s){
				//					for (int r = 0; r<=count+1;r++){
				//						System.out.print(gridNodes.get(NEWpathCollect[check2][r]).getId()+" ");
				//					}
				//					check2++;
				//					System.out.println();
				//				}
				//				System.out.println();
				cycle++;
				//				System.out.println("CountRows "+countRows);						

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


			if (NEWpathCollect[0][0]!=999){
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

	public static int [][] layerMalikSim(String path){
		//Because you are recreating the internal Grid and Malik graph, the id's will have different internal indices
		//Therefore you can read in the ids but have to search for the indices again
		String filename = path+"assocList.csv";
		BufferedReader br1 = null;
		String lineNode = "";
		int counter =0;
		int [][] assocList = new int[12][4];
		try {
			br1 = new BufferedReader(new FileReader(filename));
			String heading=br1.readLine();
			System.out.println("Throwing out the heading "+heading);
			while ((lineNode = br1.readLine())!=null) {
				String[] pathData = lineNode.split(",");
				assocList[counter][0] = Integer.parseInt(pathData[0]);		
				assocList[counter][2] = Integer.parseInt(pathData[2]);	
				counter++;
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br1 != null) {
				try {
					br1.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		//find the new Malik and Grid indices associated
		ArrayList<NmvNode> nodeListGrid = new ArrayList<NmvNode>(TriGraphConstructor.myGraphGrid.getVertices());
		ArrayList<NmvNode> nodeListMalik = new ArrayList<NmvNode>(TriGraphConstructor.myGraphMalik.getVertices());

		//Malik index
		for(int r=0;r<12;r++){
			for (int u =0;u< nodeListMalik.size();u++){
				if ((nodeListMalik.get(u).getId().equals(String.valueOf(assocList[r][0])))){
					assocList[r][1]=u;
					break;
				}
			}
		}

		//Grid index
		for(int r=0;r<12;r++){
			for (int u =0;u< nodeListGrid.size();u++){
				if ((nodeListGrid.get(u).getId().equals(String.valueOf(assocList[r][2])))){
					assocList[r][3]=u;
					break;
				}
			}
		}
		String assocFile = path+"assocListSim.csv";
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
		System.out.println("Association list written to file "+assocFile);
		return assocList;
	}
	public static int [][] layerMalik(String path,DirectedGraph<NmvNode, NmvLink> myGraphMalik,DirectedGraph<NmvNode, NmvLink> myGraphGrid){

		ArrayList<NmvNode> nodeListGrid = new ArrayList<NmvNode>(TriGraphConstructor.myGraphGrid.getVertices());
		ArrayList<NmvNode> nodeListMalik = new ArrayList<NmvNode>(TriGraphConstructor.myGraphMalik.getVertices());

		int [][] assocList = new int[nodeListMalik.size()][4];
		boolean flag = false;
		boolean use = true;
		boolean repeat = true;
		Random randomGen = new Random();
		int[] taken = new int[nodeListMalik.size()];
		int hub = 2;
		int spokes = 5;
		int gen = 0;

		DijkstraShortestPath<NmvNode, NmvLink> sPath = new DijkstraShortestPath<NmvNode, NmvLink>(myGraphGrid);

		//place the hub nodes

		for (int i =1; i<=hub;i++){
			assocList[i-1][0] = i;//Malik hub #
			flag = true;
			for (int u =0;u< nodeListMalik.size();u++){
				if ((nodeListMalik.get(u).getId().equals(String.valueOf(assocList[i-1][0])))&&(flag)){
					flag = false;
					assocList[i-1][1] = u;//Malik hub index
				}
			}
			//generate grid number and test for use
			repeat = true;
			while (repeat){
				use = true;
				gen = randomGen.nextInt(101);
				//test if it's been used
				for (int t = 0; t<taken.length;t++){
					if(taken[t]==gen) use = false;
				}
				//don't use 0
				if (gen == 0) use = false;
				if(use) repeat = false;
			}
			assocList[i-1][2]=gen; //Grid number
			taken[i-1] = gen;
			flag = true;
			for (int u =0;u< nodeListGrid.size();u++){
				if ((nodeListGrid.get(u).getId().equals(String.valueOf(assocList[i-1][2])))&&(flag)){
					flag = false;
					assocList[i-1][3] = u;//Grid index
				}
			}

		}

		//place spoke nodes
		int count = hub;
		int[][] hubDist = new int[hub][5];//hub-grid #, hub-grid index, spoke-grid #, spoke-grid index, distance
		int incumbent = 400;
		boolean beaten = false;
		//assign the grid numbers and indices to the hubDist array for the HUB nodes
		for (int y =0; y<hub;y++){
			hubDist[y][0] = assocList[y][2];
			hubDist[y][1] = assocList[y][3];
		}

		for (int i = 0;i<hub;i++){
			for (int k = hub+spokes*i;k<=hub+spokes*(i+1)-1;k++){
				// assign spoke number and spoke list index to assocList
				assocList[count][0] = k+1;//Malik hub #
				flag = true;
				for (int u =0;u< nodeListMalik.size();u++){
					if ((nodeListMalik.get(u).getId().equals(String.valueOf(assocList[count][0])))&&(flag)){
						flag = false;
						assocList[count][1] = u;//Malik hub index
					}
				}

				//generate grid number for spoke node and test for use

				repeat = true;
				while (repeat){
					use = true;
					gen = randomGen.nextInt(101);
					//test if taken
					for (int t = 0; t<taken.length;t++){
						if(taken[t]==gen)use = false;
					}
					//test if 0
					if (gen == 0) use = false;
					// calculate the distance from the generated node to each of the hubs
					for (int y =0; y<hub;y++){
						hubDist[y][2] = gen;
						flag = true;
						for (int u =0;u< nodeListGrid.size();u++){
							if ((nodeListGrid.get(u).getId().equals(String.valueOf(gen)))&&(flag)){
								flag = false;
								hubDist[y][3] = u;//List index
							}
						}
						List <NmvLink> shortestPath = sPath.getPath(nodeListGrid.get(hubDist[y][1]), nodeListGrid.get(hubDist[y][3]));
						hubDist[y][4]= shortestPath.size();
					}
					//test whether the distance to the current hub i is not longer than shortest (can be equal)
					incumbent = hubDist[i][4];
					beaten = false;
					for (int y =0; y<hub;y++){
						if (hubDist[y][4]<incumbent) beaten = true;
					}

					if (beaten) use = false;
					if (use) repeat = false;
				}
				//insert the tested gen into the assocList
				assocList[count][2]=gen; //Grid number
				taken[count] = gen;
				flag = true;
				for (int u =0;u< nodeListGrid.size();u++){
					if ((nodeListGrid.get(u).getId().equals(String.valueOf(assocList[count][2])))&&(flag)){
						flag = false;
						assocList[count][3] = u;//Grid index
					}
				}
				count++;
			}
		}

		String assocFile = path+"assocList.csv";
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
		System.out.println("Association list written to file "+assocFile);
		return assocList;
	}



}
