/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityTimeAnalyzer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package matrix;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class BetwHist {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		BetwHist test = new BetwHist();
		test.createHist();

	}
	
	public void createHist() throws IOException {
		File input = new File("/home/sfuerbas/workspace/Schweiz/BetweennessSchweiz");
		File output = new File("/home/sfuerbas/workspace/Schweiz/BetweennessSchweiz_");
		BufferedWriter bw = new BufferedWriter(new FileWriter(output));
		int num[]=new int[31];
		Scanner scanner = new Scanner(input);
		while (scanner.hasNext()) {
			scanner.useDelimiter("\n");
			scanner.next();
			scanner.useDelimiter("\n");
			String betweenness = scanner.next();
			Integer betw = Integer.parseInt(betweenness);
			bw.write("123\t"+betweenness);
			bw.newLine();
//			if (betw>0){
//				if (betw<=1) num[0]++;
//				else if (betw<=2) num[1]++;
//				else if (betw<=5) num[2]++;
//				else if (betw<=10) num[3]++;
//				else if (betw<=15) num[4]++;
//				else if (betw<=20) num[5]++;
//				else if (betw<=30) num[6]++;
//				else if (betw<=40) num[7]++;
//				else if (betw<=50) num[8]++;
//				else if (betw<=60) num[9]++;
//				else if (betw<=70) num[10]++;
//				else if (betw<=80) num[11]++;
//				else if (betw<=90) num[12]++;
//				else if (betw<=100) num[13]++;
//				else if (betw<=125) num[14]++;
//				else if (betw<=150) num[15]++;
//				else if (betw<=200) num[16]++;
//				else if (betw<=250) num[17]++;
//				else if (betw<=300) num[18]++;
//				else if (betw<=350) num[19]++;
//				else if (betw<=400) num[20]++;
//				else if (betw<=450) num[21]++;
//				else if (betw<=500) num[22]++;
//				else if (betw<=600) num[23]++;
//				else if (betw<=700) num[24]++;				
//				else if (betw<=800) num[25]++;
//				else if (betw<=900) num[26]++;
//				else if (betw<=1000) num[27]++;
//				else if (betw<=1500) num[28]++;
//				else if (betw<=2000) num[29]++;
//				else if (betw>2000) num[30]++;
//			} 		
		}
		bw.close();
		
//		System.out.println("(betw<=1) "+num[0]);
//		System.out.println("(betw<=2) "+num[1]);
//		System.out.println("(betw<=5) "+num[2]);
//		System.out.println("(betw<=10) "+num[3]);
//		System.out.println("(betw<=15) "+num[4]);
//		System.out.println("(betw<=20) "+num[5]);
//		System.out.println("(betw<=30) "+num[6]);
//		System.out.println("(betw<=40) "+num[7]);
//		System.out.println("(betw<=50) "+num[8]);
//		System.out.println("(betw<=60) "+num[9]);
//		System.out.println("(betw<=70) "+num[10]);
//		System.out.println("(betw<=80) "+num[11]);
//		System.out.println("(betw<=90) "+num[12]);
//		System.out.println("(betw<=100) "+num[13]);
//		System.out.println("(betw<=125) "+num[14]);
//		System.out.println("(betw<=150) "+num[15]);
//		System.out.println("(betw<=200) "+num[16]);
//		System.out.println("(betw<=250) "+num[17]);
//		System.out.println("(betw<=300) "+num[18]);
//		System.out.println("(betw<=350) "+num[19]);
//		System.out.println("(betw<=400) "+num[20]);
//		System.out.println("(betw<=450) "+num[21]);
//		System.out.println("(betw<=500) "+num[22]);
//		System.out.println("(betw<=600) "+num[23]);
//		System.out.println("(betw<=700) "+num[24]);
//		System.out.println("(betw<=800) "+num[25]);
//		System.out.println("(betw<=900) "+num[26]);
//		System.out.println("(betw<=1000) "+num[27]);
//		System.out.println("(betw<=1500) "+num[28]);
//		System.out.println("(betw<=2000) "+num[29]);
//		System.out.println("(betw>2000) "+num[30]);
		
	}

}
