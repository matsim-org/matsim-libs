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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.matsim.core.utils.io.IOUtils;

import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.DirectedGraph;
import playground.nmviljoen.network.NmvLink;
import playground.nmviljoen.network.NmvNode;

public class ShortestPath {
	public static void collectShortest(DirectedGraph<NmvNode, NmvLink> myGraphGrid,DirectedGraph<NmvNode, NmvLink> myGraphHub, LinkedList<NmvLink> gridLinks, LinkedList<NmvLink> hubLinks,ArrayList<NmvNode> gridNodes,ArrayList<NmvNode> hubNodes, int [][] assocList, String shortListFile){

		int [][] shortList = new int [hubNodes.size()*(hubNodes.size()-1)][3];
		DijkstraShortestPath<NmvNode, NmvLink> hPath = new DijkstraShortestPath<NmvNode, NmvLink>(myGraphHub);
		DijkstraShortestPath<NmvNode, NmvLink> gPath = new DijkstraShortestPath<NmvNode, NmvLink>(myGraphGrid);
		int count = 0;
		//		System.out.println(assocList[1][3]);
		//		System.out.println(assocList[2][3]);

		for (int k = 0; k<assocList.length;k++){
			for(int p = 0; p<assocList.length;p++){
				if (p != k){

					//determine the logical shortest path
					//set the start and end points
					List <NmvLink> shortestPathHub = hPath.getPath(hubNodes.get(assocList[k][1]), hubNodes.get(assocList[p][1]));

					int [][] logicalList = new int [shortestPathHub.size()+1][4];


					//I want to extract the grid associated indices for the path. So need to identify logical node
					//in the shortest path and aasociate with their grid indices based on assocList

					//System.out.println("SHORTEST PATH - Logical");
					Iterator<NmvLink> PathIterator = shortestPathHub.iterator();
					int flush=0;
					Collection<NmvNode> nodeTrack =null;
					if(shortestPathHub.size()==1){
						while (PathIterator.hasNext()){
							nodeTrack = myGraphHub.getIncidentVertices(PathIterator.next());
							Iterator <NmvNode> NodeIterator = nodeTrack.iterator();
							while (NodeIterator.hasNext()){
								//System.out.print(NodeIterator.next().getId()+" ");
								logicalList[flush][0] = Integer.parseInt(NodeIterator.next().getId()); //put the Malik ids in the logical list
								flush++;
							}
							//System.out.println();
						}
					}else{
						logicalList[flush][0] = assocList[k][0]; //put the Malik ids in the logical list
						flush++;
						while (PathIterator.hasNext()){
							nodeTrack = myGraphHub.getIncidentVertices(PathIterator.next());
							Iterator <NmvNode> NodeIterator = nodeTrack.iterator();

							while (NodeIterator.hasNext()){
								NodeIterator.next();
								//System.out.print(NodeIterator.next().getId()+" ");
								if(NodeIterator.hasNext()){
									logicalList[flush][0] = Integer.parseInt(NodeIterator.next().getId()); //put the Malik ids in the logical list
									flush++;
								}
							}
						}
					}


					//System.out.println("LOGICAL LIST");
					for (int y = 0; y<logicalList.length;y++){
						for (int r = 0; r<assocList.length;r++){
							if (logicalList[y][0] == assocList[r][0]){
								logicalList[y][1] = assocList[r][1];
								logicalList[y][2] = assocList[r][2];
								logicalList[y][3] = assocList[r][3];
								//System.out.println(String.format("%d,%d,%d,%d",logicalList[y][0],logicalList[y][1],logicalList[y][2],logicalList[y][3]));
							}
						}
					}




					//get the grid shortest path
					int cumShortPath = 0;

					for (int y = 0; y<logicalList.length-1;y++){
						List <NmvLink> shortestPath = gPath.getPath(gridNodes.get(logicalList[y][3]), gridNodes.get(logicalList[y+1][3]));
						cumShortPath = cumShortPath +shortestPath.size();
						//System.out.println(cumShortPath);
					}


					//					Iterator<NmvLink> PathIterator = shortestPath.iterator();
					//					Collection<NmvNode> nodeTrack =null;
					//					System.out.println("SHORTEST PATH");
					//					while (PathIterator.hasNext()){
					//						nodeTrack = myGraphGrid.getIncidentVertices(PathIterator.next());
					//						System.out.println(nodeTrack.toString());
					//					}
					shortList[count][0] = assocList[k][0];
					shortList[count][1] = assocList[p][0];
					shortList[count][2] = cumShortPath;
					count++;
				}
			}

		}
		BufferedWriter bw = IOUtils.getBufferedWriter(shortListFile);
		try{
			bw.write("Hub(From),Hub(To),ShortestPathLength");
			bw.newLine();
			for (int j =0; j< shortList.length;j++){
				bw.write(String.format("%d,%d,%d\n",shortList[j][0],shortList[j][1],shortList[j][2]));
			}
		} catch (IOException e) {
			e.printStackTrace();
			//			LOG.error("Oops, couldn't write to file.");
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				//				LOG.error("Oops, couldn't close");
			}
		}
		System.out.println("ShortList written to file");



		//		while (PathIterator.hasNext()){
		//			nodeTrack = myGraphGrid.getIncidentVertices(PathIterator.next());
		//			System.out.println(nodeTrack.toString());
		//		}
	}
}
