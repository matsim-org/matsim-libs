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

import gnu.trove.TDoubleDoubleHashMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigReader;

import playground.johannes.sna.util.TXTWriter;

/**
 * @author illenberger
 * 
 */
public class SweepMerge1D {

	private static final Logger logger = Logger.getLogger(SweepMerge1D.class);
	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		File root = new File("/Volumes/cluster.math.tu-berlin.de/net/ils2/jillenberger/leisure/runs/run272/");
//		String property = "score_join_culture";
		String property = "d_trip_culture";
		int valIdx = 1;
//		String parameterKey = "beta_join";
//		String parameterKey = "performing";
		String parameterKey = "alterProba_culture";
		
		int dumps = 1;

		File analysis = new File(String.format("%1$s/analysis/", root.getAbsolutePath()));
		analysis.mkdirs();
		
		TDoubleDoubleHashMap values = new TDoubleDoubleHashMap();

		File tasks = new File(String.format("%1$s/tasks/", root.getAbsolutePath()));
		for (File file : tasks.listFiles()) {
			if (file.isDirectory()) {
				if (!file.getName().equals("analysis")) {
					File output = new File(String.format("%1$s/output/", file.getAbsolutePath()));
					String[] dirs = output.list();
					if(dirs.length > 0) {

					Arrays.sort(dirs, new Comparator<String>() {
						@Override
						public int compare(String o1, String o2) {
							return Double.compare(Double.parseDouble(o1), Double.parseDouble(o2));
						}
					});
					/*
					 * get parameter value
					 */
					Config config = new Config();
					ConfigReader creader = new ConfigReader(config);
					creader.readFile(String.format("%1$s/config.xml", file.getAbsolutePath()));
					double paramValue = Double.parseDouble(config.findParam("socialnets", parameterKey));
//					double paramValue = Double.parseDouble(config.findParam("planCalcScore", parameterKey));
					
					
					int start = dirs.length - dumps;
					start = Math.max(0, start);
					if(dirs.length < dumps) {
						logger.warn(String.format("Less than %1$s samples.", dumps));
					}
					DescriptiveStatistics stat = new DescriptiveStatistics();
					for(int i = start; i < dirs.length; i++) {
//					for(int i = 12; i < 13; i++) {
						File statsFile = new File(String.format("%1$s/%2$s/statistics.txt", output.getAbsolutePath(), dirs[i]));
						if (statsFile.exists()) {
							/*
							 * get property value
							 */
							BufferedReader reader = new BufferedReader(new FileReader(statsFile));
							String line = reader.readLine();

							while ((line = reader.readLine()) != null) {
								String[] tokens = line.split("\t");
								String key = tokens[0];
								double val = Double.parseDouble(tokens[valIdx]);

								if (key.equals(property)) {
									stat.addValue(val);
									break;
								}
							}
						}

					}
					values.put(paramValue, stat.getMean());
					} else {
						logger.warn("No samples.");
					}
				}
			}
		}
		
		TXTWriter.writeMap(values, parameterKey, property, String.format("%1$s/%2$s.txt", analysis.getAbsolutePath(), property));
	}

}
