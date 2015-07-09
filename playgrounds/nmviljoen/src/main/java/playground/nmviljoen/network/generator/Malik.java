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
import java.io.FileWriter;
import java.io.IOException;

public class Malik {
	public static int[][] linkList(int row, int col){
		//setting structure
		// A Malik network has two bi-directionally connected hubs with n nodes connected bi-directedly to the hubs
		int core = 2;
		int n = 5;
		//calculating size of the link list and boundary values
		int links =core*n*2+core*(core-1);
		int [][] linkList = new int[links][3];
		
		int count=0;
		//populate hub network
		for (int i = 0;i<core;i++){
			for (int k = 0;k<core;k++)
				if(i!=k){
					linkList[count][0] = i+1;
					linkList[count][1] = k+1;
					linkList[count][2] = 1;
				}
			count++;
		}
		
		//populate spokes
		for (int i = 0;i<core;i++){
			System.out.println();
			for (int k = core+n*i;k<=core+n*(i+1)-1;k++){
				System.out.println(k);
				linkList[count][0] = i+1;
				linkList[count][1] = k+1;
				linkList[count][2] = 1;
				count++;
				linkList[count][0] = k+1;
				linkList[count][1] = i+1;
				linkList[count][2] = 1;
				count++;
			}
		}
		System.out.println(links);
		
			try {
				File file = new File("/Users/nadiaviljoen/Documents/workspace/ArticleRegister/GridNetworkFiles/baseline50x50/linkListMalik.csv");
				FileWriter fw = new FileWriter(file.getAbsoluteFile());
				BufferedWriter bw = new BufferedWriter(fw);
				for (int r =0;r<links;r++) {
					bw.write(String.format("%d,%d,%d\n",linkList[r][0],linkList[r][1],linkList[r][2]));
				}
				bw.close();
				System.out.println("LinkList written");

			} catch (IOException e) {
				e.printStackTrace();
			}
			

			return linkList;
	}

}
