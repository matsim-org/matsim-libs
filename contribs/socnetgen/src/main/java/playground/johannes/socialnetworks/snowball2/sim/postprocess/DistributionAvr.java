/* *********************************************************************** *
 * project: org.matsim.*
 * DistributionAvr.java
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

import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TDoubleDoubleIterator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;

/**
 * @author illenberger
 *
 */
public class DistributionAvr {
	
	private static final Logger logger = Logger.getLogger(DistributionAvr.class);

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
		 * Create a table with empty distributions.
		 */
		logger.info("Loading data...");
		Map<String, TDoubleDoubleHashMap[]> distrTable = new HashMap<String, TDoubleDoubleHashMap[]>();
		for(int i = 0; i < dumpNames.length; i++) {
			TDoubleDoubleHashMap[] distr = new TDoubleDoubleHashMap[n_runs];
			distrTable.put(dumpNames[i], distr);
		}
		
		for(int i = 0; i < runDirs.length; i++) {
			
			for(File dumpDir : runDirs[i].listFiles(dumpFilter)) {
				String dumpName = dumpDir.getName().split("\\.")[1];
				TDoubleDoubleHashMap distr = distrTable.get(dumpName)[i];
				if(distr == null) {
					distr = new TDoubleDoubleHashMap();
					distrTable.get(dumpName)[i] = distr;
				}
				/*
				 * Open the stats.txt file.
				 */
				String path = String.format("%1$s/%2$s/%3$s.txt", dumpDir.getAbsolutePath(), analyzerKey, propertyKey);
				BufferedReader reader = new BufferedReader(new FileReader(new File(path)));
				String line = reader.readLine();
				while((line = reader.readLine()) != null) {
					String tokens[] = line.split("\t");
					double bin = Double.parseDouble(tokens[0]);
					double val = Double.parseDouble(tokens[1]);
					distr.put(bin, val);
				}
			}
		}
		/*
		 * Average distributions.
		 */
		logger.info("Averaging distributions...");
		SortedSet<Double> bins = new TreeSet<Double>();
		Map<String, TDoubleDoubleHashMap> avrTable = new HashMap<String, TDoubleDoubleHashMap>();
		for(String dumpKey : dumpNames) {
			TDoubleDoubleHashMap[] distributions = distrTable.get(dumpKey);
			TDoubleDoubleHashMap distrAvr = new TDoubleDoubleHashMap();
			int count = 0;
			for(int i = 0; i < distributions.length; i++) {
				TDoubleDoubleHashMap distr = distributions[i];
				if(distr == null) {
//					logger.warn(String.format("No distribution available for dump %1$s, run %2$s,", dumpKey, i));
				} else{
					TDoubleDoubleIterator it = distr.iterator();
					for (int k = 0; k < distr.size(); k++) {
						it.advance();
						distrAvr.adjustOrPutValue(it.key(), it.value(), it.value());
						bins.add(it.key());
					}
					count++;
				}
			}
			
			TDoubleDoubleIterator it = distrAvr.iterator();
			for(int i = 0; i < distrAvr.size(); i++) {
				it.advance();
				distrAvr.put(it.key(), it.value()/(double)count);
			}
			
			avrTable.put(dumpKey, distrAvr);
		}
		/*
		 * Write.
		 */
		logger.info("Writing table...");
		List<String> dumpKeys = new ArrayList<String>(distrTable.keySet());
		Collections.sort(dumpKeys, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return Integer.parseInt(o1) - Integer.parseInt(o2);
				
			}
		});
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(outputTable));
		for(String dumpKey : dumpKeys) {
			writer.write(dumpKey);
			writer.write("\t");
		}
		writer.newLine();
		
		for(Double bin : bins) {
			writer.write(bin.toString());
			writer.write("\t");
			for(String dumpKey : dumpKeys) {
				TDoubleDoubleHashMap distr = avrTable.get(dumpKey);
				writer.write(String.valueOf(distr.get(bin)));
				writer.write("\t");
			}
			writer.newLine();
		}
		writer.close();
		
		logger.info("Done.");
	}

}
