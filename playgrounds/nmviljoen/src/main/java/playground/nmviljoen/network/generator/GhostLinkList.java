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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import playground.nmviljoen.network.NmvNode;

public class GhostLinkList {
	@SuppressWarnings("null")
	public static int[][] linkList(){
		//read in and assign nodes
		String csvFile1 = "/Users/nadiaviljoen/Documents/PhD_gridNetwork/GhostProtocol/TestingFullPath/fullPathSet_TEST1.csv";
		BufferedReader br1 = null;
		String lineNode = "";
		int counter =0;
		int [][] edges = new int[100000][2];
		try {
			br1 = new BufferedReader(new FileReader(csvFile1));

			while ((lineNode = br1.readLine()) != null) {
//				System.out.println(lineNode);
				String[] pathData = lineNode.split(" ");
//				System.out.println(pathData[0]);
				for (int i =0; i<pathData.length-1;i++){
					edges[counter][0] = Integer.parseInt(pathData[i]);
					edges[counter][1] = Integer.parseInt(pathData[i+1]);
					counter++;
				}
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

		int [][] linkList = new int[counter][2];
		for (int t1=0; t1<counter;t1++){
			linkList[t1][0] = edges[t1][0];
			linkList[t1][1] = edges[t1][1];
		}
		return linkList;
		
	}
	public static void main(String[] args) throws FileNotFoundException{
		int[][] edges= linkList();
		System.out.println(edges.length);
		
		
	}

}
