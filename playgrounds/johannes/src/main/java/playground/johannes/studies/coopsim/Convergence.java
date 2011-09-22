/* *********************************************************************** *
 * project: org.matsim.*
 * Convergence.java
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
package playground.johannes.studies.coopsim;

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
public class Convergence {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String root = "/Volumes/cluster.math.tu-berlin.de/net/ils2/jillenberger/leisure/runs/run33/";
		File outputDir = new File(root + "/output");
		String property = "d_trip_visit";
		
		File analysisDir = new File(root + "/analyis");
		analysisDir.mkdirs();
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(analysisDir.getAbsolutePath() + "/" + property + ".txt"));
		writer.write("it\t");
		writer.write(property);
		writer.newLine();
		
		for(File file : outputDir.listFiles()) {
			if(file.isDirectory()) {
				File statsFile = new File(String.format("%1$s/statistics.txt", file.getAbsolutePath()));
				
				if(statsFile.exists()) {
					String iter = file.getName();
					
					BufferedReader reader = new BufferedReader(new FileReader(statsFile));
					String line = reader.readLine();
					
					while((line = reader.readLine()) != null) {
						String[] tokens = line.split("\t");
						String key = tokens[0];
						String val = tokens[1];
						
						if(key.equals(property)) {
							writer.write(iter);
							writer.write("\t");
							writer.write(val);
							writer.newLine();
						}
					}
				}
			}
		}

		writer.close();
	}

}
