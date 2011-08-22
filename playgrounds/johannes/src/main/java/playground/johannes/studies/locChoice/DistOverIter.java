/* *********************************************************************** *
 * project: org.matsim.*
 * DistOverIter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.studies.locChoice;

import gnu.trove.TDoubleArrayList;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author illenberger
 *
 */
public class DistOverIter {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		File rootDir = new File("/Volumes/cluster.math.tu-berlin.de/net/ils2/jillenberger/leisure/runs/run5/output/analysis");
		
		TDoubleArrayList values = new TDoubleArrayList();
		
		for(File dir : rootDir.listFiles()) {
			if(dir.isDirectory()) {
			String statsFile = String.format("%1$s/statistics.txt", dir.getAbsolutePath());
			
			BufferedReader reader = new BufferedReader(new FileReader(statsFile));
			String line = reader.readLine();
			while((line = reader.readLine()) != null) {
				String[] tokens = line.split("\t");
				if(tokens[0].equalsIgnoreCase("d_act_leisure")) {
//				if(tokens[0].equalsIgnoreCase("scores_social")) {
					double val = Double.parseDouble(tokens[1]);
					values.add(val);
				}
			}
			}
		}

		BufferedWriter writer = new BufferedWriter(new FileWriter(rootDir.getAbsolutePath() + "/d_act_leisure.txt"));
//		BufferedWriter writer = new BufferedWriter(new FileWriter(rootDir.getAbsolutePath() + "/scores_social.txt"));
		writer.write("it\tdist");
		writer.newLine();
		for(int i = 0; i < values.size(); i++) {
			writer.write(String.valueOf(i*100));
			writer.write("\t");
			writer.write(String.valueOf(values.get(i)));
			writer.newLine();
		}
		writer.close();
	}

}
