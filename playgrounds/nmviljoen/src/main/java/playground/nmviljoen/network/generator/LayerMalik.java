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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.DirectedGraph;
import playground.nmviljoen.network.NmvLink;
import playground.nmviljoen.network.NmvNode;

public class LayerMalik {
	public static int [][] assocList(DirectedGraph<NmvNode, NmvLink> myGraphGrid, LinkedList<NmvLink> gridLinks, LinkedList<NmvLink> hubLinks,ArrayList<NmvNode> gridNodes,ArrayList<NmvNode> hubNodes){
		int [][] assocList = new int[hubNodes.size()][4];
		Iterator<NmvNode> HubIterator = hubNodes.iterator();
		Iterator<NmvNode> GridIterator = gridNodes.iterator();
		boolean flag = false;
		boolean use = true;
		boolean repeat = true;
		Random randomGen = new Random();
		int[] taken = new int[hubNodes.size()];
		int hub = 2;
		int spokes = 5;
		int gen = 0;

		DijkstraShortestPath<NmvNode, NmvLink> sPath = new DijkstraShortestPath<NmvNode, NmvLink>(myGraphGrid);

		//place the hub nodes

		for (int i =1; i<=hub;i++){
			assocList[i-1][0] = i;//Malik hub #
			flag = true;
			for (int u =0;u< hubNodes.size();u++){
				if ((hubNodes.get(u).getId().equals(String.valueOf(assocList[i-1][0])))&&(flag)){
					flag = false;
					assocList[i-1][1] = u;//Malik hub index
				}
			}
			//generate grid number and test for use
			repeat = true;
			while (repeat){
				use = true;
				gen = randomGen.nextInt(2501);
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
			for (int u =0;u< gridNodes.size();u++){
				if ((gridNodes.get(u).getId().equals(String.valueOf(assocList[i-1][2])))&&(flag)){
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
				for (int u =0;u< hubNodes.size();u++){
					if ((hubNodes.get(u).getId().equals(String.valueOf(assocList[count][0])))&&(flag)){
						flag = false;
						assocList[count][1] = u;//Malik hub index
					}
				}

				//generate grid number for spoke node and test for use
				
				repeat = true;
				while (repeat){
					use = true;
					gen = randomGen.nextInt(2501);
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
						for (int u =0;u< gridNodes.size();u++){
							if ((gridNodes.get(u).getId().equals(String.valueOf(gen)))&&(flag)){
								flag = false;
								hubDist[y][3] = u;//List index
							}
						}
						List <NmvLink> shortestPath = sPath.getPath(gridNodes.get(hubDist[y][1]), gridNodes.get(hubDist[y][3]));
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
				for (int u =0;u< gridNodes.size();u++){
					if ((gridNodes.get(u).getId().equals(String.valueOf(assocList[count][2])))&&(flag)){
						flag = false;
						assocList[count][3] = u;//Grid index
					}
				}
				count++;
			}
		}

		return assocList;
	}

}
