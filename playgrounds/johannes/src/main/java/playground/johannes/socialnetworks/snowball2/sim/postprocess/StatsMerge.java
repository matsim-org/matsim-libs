/* *********************************************************************** *
 * project: org.matsim.*
 * Boxplot.java
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
package playground.johannes.socialnetworks.snowball2.sim.postprocess;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

/**
 * @author illenberger
 *
 */
public class StatsMerge {

	private static final Logger logger = Logger.getLogger(StatsMerge.class);
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		File rootDir = new File(args[0]);
		final String dumpPattern = args[1];
		String analyzerKey = args[2];
		String propertyKey = args[3];
		String outputTable = args[4];
		String outputAvr = args[5];
		
		logger.info(String.format("Root dir = %1$s", rootDir));
		/*
		 * Create a filename filter to filter only the dumps of interest.
		 */
		FilenameFilter dumpFilter = new FilenameFilter() {	
			@Override
			public boolean accept(File dir, String name) {
				if(name.contains(dumpPattern))
					return true;
				else
					return false;
			}
		};
		/*
		 * Create a file filter to obtain only directories.
		 */
		FileFilter dirFilter = new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.isDirectory();
			}
		};
		/*
		 * Get the number of ensemble runs and dumps. The latter can vary
		 * between the ensemble runs!
		 */
		logger.info("Getting number of runs and dumps...");
		int n_runs = 0;
		String[] dumpNames = new String[0];
		File[] runDirs = rootDir.listFiles(dirFilter);
		for(File runDir : runDirs) {
			n_runs++;
			
			File[] dumpDirs = runDir.listFiles(dumpFilter);
			if(dumpDirs.length > dumpNames.length) {
				/*
				 * There is a run with more dumps...
				 */
				dumpNames = new String[dumpDirs.length];
				for(int i = 0; i < dumpDirs.length; i++) {
					/*
					 * Extract the counter of the dump (iteration, vertices, ...).
					 */
					dumpNames[i] = (dumpDirs[i].getName().split("\\."))[1];
				}
			}
		}
		/*
		 * Create a table filled with NaNs.
		 */
		logger.info("Loading data...");
		Map<String, double[]> dataTable = new HashMap<String, double[]>();
		for(int i = 0; i < dumpNames.length; i++) {
			double[] values = new double[n_runs];
			Arrays.fill(values, Double.NaN);
			dataTable.put(dumpNames[i], values);
		}
		/*
		 * Fill the table with values.
		 */
		for(int i = 0; i < runDirs.length; i++) {
			for(File dumpDir : runDirs[i].listFiles(dumpFilter)) {
				/*
				 * Open the stats.txt file.
				 */
				String path = String.format("%1$s/%2$s/stats.txt", dumpDir.getAbsolutePath(), analyzerKey);
				File file = new File(path);
				if (file.exists()) {
					BufferedReader reader = new BufferedReader(new FileReader(file));
					String line;
					while ((line = reader.readLine()) != null) {
						String[] tokens = line.split("\t");
						if (tokens[0].equalsIgnoreCase(propertyKey)) {
							/*
							 * Retrieve the value for the property key and then
							 * break.
							 */
							double value = Double.parseDouble(tokens[1]);
							String dumpName = dumpDir.getName().split("\\.")[1];
							double[] values = dataTable.get(dumpName);
							if (values == null)
								logger.warn(String.format("Null object for run %1$s. This should not happen!", i));
							else
								values[i] = value;
							break;
						}
					}
				} else {
					logger.warn(String.format("No stats file found: %1$s.", path));
				}
			}
		}
		/*
		 * Calculate averages for each dump.
		 */
		Map<String, Double> averages = new HashMap<String, Double>();
		for(Entry<String, double[]> entry : dataTable.entrySet()) {
			double sum = 0;
			int count = 0;
			for(double d : entry.getValue()) {
				if(!Double.isNaN(d)) {
					sum += d;
					count++;
				}
			}
			double mean = sum/(double)count; 
			averages.put(entry.getKey(), mean);
		}
		/*
		 * Sort the dump names.
		 */
		logger.info("Writing data...");
		List<String> keys = new ArrayList<String>(dataTable.keySet());
		Collections.sort(keys, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return Integer.parseInt(o1) - Integer.parseInt(o2);
			}
		});
		/*
		 * Write the data table.
		 */
		BufferedWriter writer = new BufferedWriter(new FileWriter(outputTable));
		for(String key : keys) {
			writer.write(key);
			writer.write("\t");
		}
		writer.newLine();
		
		for(int i = 0; i < n_runs; i++) {
			for(String key : keys) {
				writer.write(String.valueOf(dataTable.get(key)[i]));
				writer.write("\t");
			}
			writer.newLine();
		}
		writer.close();
		/*
		 * Write averages.
		 */
		writer = new BufferedWriter(new FileWriter(outputAvr));
		for(String key : keys) {
			writer.write(key);
			writer.write("\t");
			writer.write(String.valueOf(averages.get(key)));
			writer.newLine();
		}
		writer.close();
		
		logger.info("Done.");
	}
}
