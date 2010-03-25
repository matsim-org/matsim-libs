/* *********************************************************************** *
 * project: org.matsim.*
 * RunLocator.java
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
import gnu.trove.TDoubleIntHashMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
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
public abstract class RunLocator<T> {
	
	private static final Logger logger = Logger.getLogger(RunLocator.class);

	public Map<String, T[]> locate(File rootDir, final String dumpPattern, String analyzerKey) {
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
		Map<String, T[]> objectTable = new HashMap<String, T[]>();
		for(int i = 0; i < dumpNames.length; i++) {
			T[] object = createObjectArray(n_runs);
			objectTable.put(dumpNames[i], object);
		}
		
		for(int i = 0; i < runDirs.length; i++) {
			
			for(File dumpDir : runDirs[i].listFiles(dumpFilter)) {
				String dumpName = dumpDir.getName().split("\\.")[1];
				String path = String.format("%1$s/%2$s/", dumpDir.getAbsolutePath(), analyzerKey);
				inDirectory(path, dumpName, i, objectTable.get(dumpName));
			}
		}
		
		return objectTable;
	}
	
	protected abstract T[] createObjectArray(int n_runs);
	
	protected abstract void inDirectory(String path, String dumpKey, int runIdx, T[] objectArray);
	
	public Map<String, Double> averageValues(Map<String, Double[]> data) {
		Map<String, Double> averages = new HashMap<String, Double>();
		for (Entry<String, Double[]> entry : data.entrySet()) {
			double sum = 0;
			int count = 0;
			for (Double d : entry.getValue()) {
				if (d != null) {
					sum += d;
					count++;
				}
			}
			double mean = sum / (double) count;
			averages.put(entry.getKey(), mean);
		}

		return averages;
	}
	
	protected Map<String, TDoubleDoubleHashMap> averageDistritburions(Map<String, TDoubleDoubleHashMap[]> dataTable) {
		/*
		 * Average distributions.
		 */
		logger.info("Averaging distributions...");
//		SortedSet<Double> bins = new TreeSet<Double>();
		int notFound = 0;
		Map<String, TDoubleDoubleHashMap> avrTable = new HashMap<String, TDoubleDoubleHashMap>();
		for(String dumpKey : dataTable.keySet()) {
			TDoubleDoubleHashMap[] distributions = dataTable.get(dumpKey);
			TDoubleDoubleHashMap distrAvr = new TDoubleDoubleHashMap();
			TDoubleIntHashMap counts = new TDoubleIntHashMap();
//			int count = 0;
			for(int i = 0; i < distributions.length; i++) {
				TDoubleDoubleHashMap distr = distributions[i];
				if(distr == null) {
					notFound++;
				} else{
					TDoubleDoubleIterator it = distr.iterator();
					for (int k = 0; k < distr.size(); k++) {
						it.advance();
						distrAvr.adjustOrPutValue(it.key(), it.value(), it.value());
						counts.adjustOrPutValue(it.key(), 1, 1);
//						bins.add(it.key());
					}
//					count++;
				}
			}
			
			TDoubleDoubleIterator it = distrAvr.iterator();
			for(int i = 0; i < distrAvr.size(); i++) {
				it.advance();
				int cnt = counts.get(it.key());
				distrAvr.put(it.key(), it.value()/(double)cnt);
			}
			
			avrTable.put(dumpKey, distrAvr);
		}
		
		if(notFound > 0)
			logger.warn(String.format("%1$s distributions missing.", notFound));
		
		return avrTable;
	}

	protected TDoubleDoubleHashMap loadDistribution(String file) {
		try {
		TDoubleDoubleHashMap distr = new TDoubleDoubleHashMap();
		BufferedReader reader = new BufferedReader(new FileReader(new File(file)));
		String line = reader.readLine();
		while((line = reader.readLine()) != null) {
			String tokens[] = line.split("\t");
			double bin = Double.parseDouble(tokens[0]);
			double val = Double.parseDouble(tokens[1]);
			distr.put(bin, val);
		}
		
		return distr;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	protected List<String> sortDumpKeys(Map<String, ?> dataTable) {
		List<String> dumpKeys = new ArrayList<String>(dataTable.keySet());
		Collections.sort(dumpKeys, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return Integer.parseInt(o1) - Integer.parseInt(o2);
				
			}
		});
		
		return dumpKeys;
	}
}
