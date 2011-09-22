/* *********************************************************************** *
 * project: org.matsim.*
 * SweepMerge.java
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
import java.util.Arrays;
import java.util.Comparator;

import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;

/**
 * @author illenberger
 * 
 */
public class SweepMerge {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		File root = new File("/Volumes/cluster.math.tu-berlin.de/net/ils2/jillenberger/leisure/runs/run20/");
		String property = "d_trip_visit";
		int valIdx = 5;
		String parameterKey = "beta_join";

		File analysis = new File(String.format("%1$s/analysis/", root.getAbsolutePath()));
		analysis.mkdirs();
		BufferedWriter writer = new BufferedWriter(new FileWriter(String.format("%1$s/%2$s.%3$s.txt",
				analysis.getAbsolutePath(), property, parameterKey)));
		writer.write(parameterKey);
		writer.write("\t");
		writer.write(property);
		writer.newLine();
		
		for (File file : root.listFiles()) {
			if (file.isDirectory()) {
				if (!file.getName().equals("analysis")) {
					File output = new File(String.format("%1$s/output/", file.getAbsolutePath()));
					String[] dirs = output.list();

					Arrays.sort(dirs, new Comparator<String>() {
						@Override
						public int compare(String o1, String o2) {
							return Double.compare(Double.parseDouble(o1), Double.parseDouble(o2));
						}
					});

					File statsFile = new File(String.format("%1$s/%2$s/statistics.txt", output.getAbsolutePath(),
							dirs[dirs.length - 1]));
					if (statsFile.exists()) {
						/*
						 * get parameter value
						 */
						Config config = new Config();
						config.addCoreModules();
						MatsimConfigReader creader = new MatsimConfigReader(config);
						creader.readFile(String.format("%1$s/config.xml", file.getAbsolutePath()));
						String paramValue = config.getParam("socialnets", parameterKey);
						/*
						 * get property value
						 */
						BufferedReader reader = new BufferedReader(new FileReader(statsFile));
						String line = reader.readLine();

						while ((line = reader.readLine()) != null) {
							String[] tokens = line.split("\t");
							String key = tokens[0];
							String val = tokens[valIdx];

							if (key.equals(property)) {
								writer.write(paramValue);
								writer.write("\t");
								writer.write(val);
								writer.newLine();
								break;
							}
						}
					}
				}
			}
		}
		writer.close();
	}

}
