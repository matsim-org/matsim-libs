/* *********************************************************************** *
 * project: org.matsim.*
 * Postprocess.java
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
package playground.johannes.studies.mcmc;

import gnu.trove.TDoubleDoubleHashMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.matsim.contrib.sna.util.TXTWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;

/**
 * @author illenberger
 *
 */
public class Postprocess {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String rootDir = "/Volumes/cluster.math.tu-berlin.de/net/ils/jillenberger/socialnets/mcmc/runs/run311/";
		String statsPath = "20000000000/social/statistics.txt";
//		String key = "r_age";
		String key = "r_gender";
//		String thetaKey = "theta_age";
		String thetaKey = "theta_gender";
		
		TDoubleDoubleHashMap values = new TDoubleDoubleHashMap();
		
		Config config = new Config();
		MatsimConfigReader configReader = new MatsimConfigReader(config);
		
		File root = new File(rootDir + "/output/");
		for(String runDir : root.list()) {
			String statsFileName = String.format("%1$s/output/%2$s/%3$s", rootDir, runDir, statsPath);
			File statsFile = new File(statsFileName);
			if(statsFile.exists()) {
				/*
				 * read value
				 */
				BufferedReader reader = new BufferedReader(new FileReader(statsFile));
				double val = Double.NaN;
				String line = null;
				while((line = reader.readLine()) != null) {
					String tokens[] = line.split("\t");
					if(tokens[0].equals(key)) {
						val = Double.parseDouble(tokens[1]);
					}
				}
				/*
				 * get theta param
				 */		
				configReader.readFile(String.format("%1$s/output/%2$s/config.xml", rootDir, runDir));
				double theta = Double.parseDouble(config.getParam("ergm", thetaKey));
				
				values.put(theta, val);
			} else {
				System.err.println(String.format("No stats file: %1$s", statsFileName));
			}
		}
		
		TXTWriter.writeMap(values, "theta", "value", String.format("%1$s/analysis/%2$s.txt", rootDir, key));
	}

}
