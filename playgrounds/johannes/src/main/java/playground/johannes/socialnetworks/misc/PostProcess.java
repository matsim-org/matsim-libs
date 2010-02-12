/* *********************************************************************** *
 * project: org.matsim.*
 * PostProcess.java
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
package playground.johannes.socialnetworks.misc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author illenberger
 *
 */
public class PostProcess {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		String dir = "/Volumes/ils-raid/socialnets/snowball/output/proposalsim";
		
		List<int[][]> tables = new ArrayList<int[][]>(10);
		for(int i = 10; i < 101; i += 10) {
			tables.add(readTable(String.format("%1$s/%2$s.stats.txt", dir, i)));
		}
	
		double binsize = 10;
		int bins = (int)Math.ceil(63600/binsize)+1;
		float[][] matrix = new float[bins][tables.size()];
		
		for(int i = 0; i < tables.size(); i++) {
			int[][] table = tables.get(i);
			int maxIt = 0;
			for(int x = 0; x < table.length; x++) {
				int nodes = table[x][1];
				int bin = (int)Math.ceil(nodes/binsize);
				int val = table[x][0];
				int n = (i+1)*10;
				matrix[bin][i] =val;//(float)(connect/((double)(n*(n-1)/2)) * 100);
				maxIt = Math.max(maxIt, val);
			}
			for(int x = table.length; x < bins; x++) {
				int n = (i+1)*10;
				matrix[x][i] = maxIt;//(n*(n-1)/2);
			}
		}
		
		String output = "/Volumes/ils-raid/socialnets/snowball/output/proposalsim/table.txt";
		BufferedWriter writer = new BufferedWriter(new FileWriter(output));
		for(int i = 0; i < matrix.length; i++) {
//			writer.write(String.valueOf((int)((i+1)*binsize)));
//			writer.write("\t");
			for(int j = 0; j < matrix[i].length; j++) {
				writer.write(String.valueOf(matrix[i][j]));
				writer.write("\t");
			}
			writer.newLine();
		}
		writer.close();
	}

	private static int[][] readTable(String file) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line = reader.readLine();
			ArrayList<String> lines = new ArrayList<String>();
			while((line = reader.readLine()) != null) {
				lines.add(line);
			}
			
			int[][] table = new int[lines.size()][4];
			for(int i = 0; i < lines.size(); i++) {
				String[] tokens = lines.get(i).split("\t");
				for(int j = 0; j < tokens.length; j++) {
					table[i][j] = Integer.parseInt(tokens[j]);
				}
			}
			
			return table;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
//	private static int getMax() {
//		return 0;
//	}
}
