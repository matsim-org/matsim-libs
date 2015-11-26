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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

import org.matsim.core.utils.io.IOUtils;


public class GhostLinkList {
	public static int[][] linkList(String path, int marker){
		//read in and assign nodes
		
		String csvFile1 = path+"shortestPathSetTRIM.csv";
		BufferedReader br1 = null;
		String lineNode = "";
		int count =0;
		int [][] fullpathset = new int[marker][63];
		for (int[] row: fullpathset){
			Arrays.fill(row, 999);
		}
		try {
			br1 = new BufferedReader(new FileReader(csvFile1));
			while ((lineNode = br1.readLine())!=null) {
				String[] pathData = lineNode.split(" ");
//				for (int i =0; i<pathData.length;i++){
//				System.out.print(pathData[i]+" ");		
//				}
//				System.out.println();
				for (int i =0; i<pathData.length;i++){
//					System.out.println(i);
					fullpathset[count][i] = Integer.parseInt(pathData[i]);			
				}
				count++;
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

		int counter =0;
		int [][] edges = new int[fullpathset.length*55][2];
		for (int[] row: edges){
			Arrays.fill(row, 999);
		}

		for (int k=0;k<fullpathset.length;k++){

			//		for (int k=0;k<5;k++){
			int i=0;
			while (i<63){
				if (fullpathset[k][i+1]!=999){

					edges[counter][0] = fullpathset[k][i];
					edges[counter][1] = fullpathset[k][i+1];
					counter++;
					i++;
				}else break;
			}
		}
		
		int [][] linkList = new int[counter][2];
		for (int t1=0; t1<counter;t1++){
			linkList[t1][0] = edges[t1][0];
			linkList[t1][1] = edges[t1][1];
		}

		//write out full link list for testing
		String filename = path+"detailGhostLinkList.csv";
		BufferedWriter bd = IOUtils.getBufferedWriter(filename);
		try{
			bd.write("From,To");
			bd.newLine();
			for (int j =0; j< linkList.length;j++){
				bd.write(String.format("%d,%d\n",linkList[j][0],linkList[j][1]));
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally{
			try {
				bd.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		System.out.println("Ghost LinkList (detailed) written to file "+filename);
		//now create a weighted linkList
		int[][] weightedLinkListTemp = new int[linkList.length][3];
		int count1=1;
		weightedLinkListTemp[0][0] = linkList[0][0];
		weightedLinkListTemp[0][1] = linkList[0][1];
		weightedLinkListTemp[0][2] = 1;
		boolean flag = false; //it is a new link
		for (int p =1; p<linkList.length;p++){
			flag = false;
			for (int s=0; s<count1;s++){
				if((linkList[p][0]==weightedLinkListTemp[s][0])&&(linkList[p][1]==weightedLinkListTemp[s][1])){
					weightedLinkListTemp[s][2]=weightedLinkListTemp[s][2]+1;//increment the weight
					flag = true;
					break;
				}
			}
			if (!flag){ //then add a new link
				weightedLinkListTemp[count1][0] = linkList[p][0];
				weightedLinkListTemp[count1][1] = linkList[p][1];
				weightedLinkListTemp[count1][2] = 1;
				count1++;
			}
		}

		int[][] weightedLinkListFin = new int[count1][3];
		for (int f=0;f<count1;f++){
			weightedLinkListFin[f][0]=weightedLinkListTemp[f][0];
			weightedLinkListFin[f][1]=weightedLinkListTemp[f][1];
			weightedLinkListFin[f][2]=weightedLinkListTemp[f][2];
		}

		//write weighted edgelist to file - tested it, it creates the list correctly
		filename = path+"aggregatedGhostLinkList.csv";
		BufferedWriter be = IOUtils.getBufferedWriter(filename);
		try{
			be.write("From,To,Weight");
			be.newLine();
			for (int j =0; j< weightedLinkListFin.length;j++){
				be.write(String.format("%d,%d,%d\n",weightedLinkListFin[j][0],weightedLinkListFin[j][1],weightedLinkListFin[j][2]));
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally{
			try {
				be.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		System.out.println("Ghost LinkList (aggregated) written to file "+filename);
		return weightedLinkListFin;

	}
	public static void main(String[] args) throws FileNotFoundException{
		String csvFile1 = "/Users/nadiaviljoen/Documents/PhD_gridNetwork/MalikLab/Run_104/shortestPathSetTRIM.csv";
		BufferedReader br1 = null;
		String lineNode = "";
		int counter =0;
		int [][] fullpathset = new int[11778][21];
		try {
			br1 = new BufferedReader(new FileReader(csvFile1));
			while ((lineNode = br1.readLine())!=null) {
				String[] pathData = lineNode.split(" ");
				for (int i =0; i<pathData.length;i++){
					fullpathset[counter][i] = Integer.parseInt(pathData[i]);			
				}
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
		
		String filename2 = "/Users/nadiaviljoen/Documents/PhD_gridNetwork/MalikLab/Run_6 troubleshoot/shortestPathSetTRIMtest.csv";
		BufferedWriter bt = IOUtils.getBufferedWriter(filename2);
		try{
			for (int j =0; j< 11778;j++){
				for (int r=0;r<fullpathset[0].length;r++){
					if (fullpathset[j][r]!=0){
						bt.write(fullpathset[j][r]+",");
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
//		int [][] Links = linkList("/Users/nadiaviljoen/Documents/PhD_gridNetwork/MalikLab/Run_6 troubleshoot/",fullpathset);
	}

}
