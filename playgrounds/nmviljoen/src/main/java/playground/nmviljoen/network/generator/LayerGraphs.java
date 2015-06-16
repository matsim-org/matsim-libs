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

import playground.nmviljoen.network.NmvLink;
import playground.nmviljoen.network.NmvNode;

public class LayerGraphs {
	public static int [][] assocList(LinkedList<NmvLink> gridLinks, LinkedList<NmvLink> hubLinks,ArrayList<NmvNode> gridNodes,ArrayList<NmvNode> hubNodes){
		int [][] assocList = new int[hubNodes.size()][4];
		Iterator<NmvNode> HubIterator = hubNodes.iterator();
		Iterator<NmvNode> GridIterator = gridNodes.iterator();
		boolean flag = true;

		//manual association Malik5 on 10x10 grid

		//node 1
		assocList[0][0]=1; //Malik5
		flag = true;
		for (int u =0;u< hubNodes.size();u++){
			if ((hubNodes.get(u).getId().equals(String.valueOf(assocList[0][0])))&&(flag)){
				flag = false;
				assocList[0][1] = u;//Malik5 List index
			}
		}
		assocList[0][2]=34; //Grid
		flag = true;
		for (int u =0;u< gridNodes.size();u++){
			if ((gridNodes.get(u).getId().equals(String.valueOf(assocList[0][2])))&&(flag)){
				flag = false;
				assocList[0][3] = u;//Grid List index
			}
		}

		//node 2

		assocList[1][0]=2; //Malik5
		flag = true;
		for (int u =0;u< hubNodes.size();u++){
			if ((hubNodes.get(u).getId().equals(String.valueOf(assocList[1][0])))&&(flag)){
				flag = false;
				assocList[1][1] = u;//Malik5 List index
			}
		}

		assocList[1][2]=67; //Grid
		flag = true;
		for (int u =0;u< gridNodes.size();u++){
			if ((gridNodes.get(u).getId().equals(String.valueOf(assocList[1][2])))&&(flag)){
				flag = false;
				assocList[1][3] = u;//Grid List index
			}
		}

		//node 3
		assocList[2][0]=3; //Malik5
		flag = true;
		for (int u =0;u< hubNodes.size();u++){
			if ((hubNodes.get(u).getId().equals(String.valueOf(assocList[2][0])))&&(flag)){
				flag = false;
				assocList[2][1] = u;//Malik5 List index
			}
		}
		assocList[2][2]=25; //Grid
		flag = true;
		for (int u =0;u< gridNodes.size();u++){
			if ((gridNodes.get(u).getId().equals(String.valueOf(assocList[2][2])))&&(flag)){
				flag = false;
				assocList[2][3] = u;//Grid List index
			}
		}

		// node 4
		assocList[3][0]=4; //Malik5
		flag = true;
		for (int u =0;u< hubNodes.size();u++){
			if ((hubNodes.get(u).getId().equals(String.valueOf(assocList[3][0])))&&(flag)){
				flag = false;
				assocList[3][1] = u;//Malik5 List index
			}
		}

		assocList[3][2]=14; //Grid
		flag = true;
		for (int u =0;u< gridNodes.size();u++){
			if ((gridNodes.get(u).getId().equals(String.valueOf(assocList[3][2])))&&(flag)){
				flag = false;
				assocList[3][3] = u;//Grid List index
			}
		}

		// node 5
		assocList[4][0]=5; //Malik5
		flag = true;
		for (int u =0;u< hubNodes.size();u++){
			if ((hubNodes.get(u).getId().equals(String.valueOf(assocList[4][0])))&&(flag)){
				flag = false;
				assocList[4][1] = u;//Malik5 List index
			}
		}

		assocList[4][2]=23; //Grid
		flag = true;
		for (int u =0;u< gridNodes.size();u++){
			if ((gridNodes.get(u).getId().equals(String.valueOf(assocList[4][2])))&&(flag)){
				flag = false;
				assocList[4][3] = u;//Grid List index
			}
		}

		// node 6
		assocList[5][0]=6; //Malik5
		flag = true;
		for (int u =0;u< hubNodes.size();u++){
			if ((hubNodes.get(u).getId().equals(String.valueOf(assocList[5][0])))&&(flag)){
				flag = false;
				assocList[5][1] = u;//Malik5 List index
			}
		}

		assocList[5][2]=32; //Grid
		flag = true;
		for (int u =0;u< gridNodes.size();u++){
			if ((gridNodes.get(u).getId().equals(String.valueOf(assocList[5][2])))&&(flag)){
				flag = false;
				assocList[5][3] = u;//Grid List index
			}
		}

		// node 7
		assocList[6][0]=7; //Malik5
		flag = true;
		for (int u =0;u< hubNodes.size();u++){
			if ((hubNodes.get(u).getId().equals(String.valueOf(assocList[6][0])))&&(flag)){
				flag = false;
				assocList[6][1] = u;//Malik5 List index
			}
		}

		assocList[6][2]=43; //Grid
		flag = true;
		for (int u =0;u< gridNodes.size();u++){
			if ((gridNodes.get(u).getId().equals(String.valueOf(assocList[6][2])))&&(flag)){
				flag = false;
				assocList[6][3] = u;//Grid List index
			}
		}

		// node 8
		assocList[7][0]=8; //Malik5
		flag = true;
		for (int u =0;u< hubNodes.size();u++){
			if ((hubNodes.get(u).getId().equals(String.valueOf(assocList[7][0])))&&(flag)){
				flag = false;
				assocList[7][1] = u;//Malik5 List index
			}
		}

		assocList[7][2]=58; //Grid
		flag = true;
		for (int u =0;u< gridNodes.size();u++){
			if ((gridNodes.get(u).getId().equals(String.valueOf(assocList[7][2])))&&(flag)){
				flag = false;
				assocList[7][3] = u;//Grid List index
			}
		}

		// node 9
		assocList[8][0]=9; //Malik5
		flag = true;
		for (int u =0;u< hubNodes.size();u++){
			if ((hubNodes.get(u).getId().equals(String.valueOf(assocList[8][0])))&&(flag)){
				flag = false;
				assocList[8][1] = u;//Malik5 List index
			}
		}

		assocList[8][2]=69; //Grid
		flag = true;
		for (int u =0;u< gridNodes.size();u++){
			if ((gridNodes.get(u).getId().equals(String.valueOf(assocList[8][2])))&&(flag)){
				flag = false;
				assocList[8][3] = u;//Grid List index
			}
		}

		// node 10
		assocList[9][0]=10; //Malik5
		flag = true;
		for (int u =0;u< hubNodes.size();u++){
			if ((hubNodes.get(u).getId().equals(String.valueOf(assocList[9][0])))&&(flag)){
				flag = false;
				assocList[9][1] = u;//Malik5 List index
			}
		}

		assocList[9][2]=78; //Grid
		flag = true;
		for (int u =0;u< gridNodes.size();u++){
			if ((gridNodes.get(u).getId().equals(String.valueOf(assocList[9][2])))&&(flag)){
				flag = false;
				assocList[9][3] = u;//Grid List index
			}
		}

		// node 11
		assocList[10][0]=11; //Malik5
		flag = true;
		for (int u =0;u< hubNodes.size();u++){
			if ((hubNodes.get(u).getId().equals(String.valueOf(assocList[10][0])))&&(flag)){
				flag = false;
				assocList[10][1] = u;//Malik5 List index
			}
		}

		assocList[10][2]=87; //Grid
		flag = true;
		for (int u =0;u< gridNodes.size();u++){
			if ((gridNodes.get(u).getId().equals(String.valueOf(assocList[10][2])))&&(flag)){
				flag = false;
				assocList[10][3] = u;//Grid List index
			}
		}

		// node 12
		assocList[11][0]=12; //Malik5
		flag = true;
		for (int u =0;u< hubNodes.size();u++){
			if ((hubNodes.get(u).getId().equals(String.valueOf(assocList[11][0])))&&(flag)){
				flag = false;
				assocList[11][1] = u;//Malik5 List index
			}
		}

		assocList[11][2]=76; //Grid
		flag = true;
		for (int u =0;u< gridNodes.size();u++){
			if ((gridNodes.get(u).getId().equals(String.valueOf(assocList[11][2])))&&(flag)){
				flag = false;
				assocList[11][3] = u;//Grid List index
			}
		}


		return assocList;
	}

}
