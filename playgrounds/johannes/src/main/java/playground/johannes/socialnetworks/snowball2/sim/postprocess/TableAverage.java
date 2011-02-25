/* *********************************************************************** *
 * project: org.matsim.*
 * TableAverage.java
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
package playground.johannes.socialnetworks.snowball2.sim.postprocess;

import gnu.trove.TDoubleIntHashMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

/**
 * @author illenberger
 *
 */
public class TableAverage {
	
	private static final Logger logger = Logger.getLogger(TableAverage.class);

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		File rootDir = new File(args[0]);
		String propertyKey = args[1];
		String outputTable = args[2];
		
		logger.info(String.format("Root dir = %1$s", rootDir));
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
		int n_runs = rootDir.listFiles(dirFilter).length;
//		File[] runDirs = rootDir.listFiles(dirFilter);
//		for(File runDir : runDirs) {
//			n_runs++;
//		
//		}
		/*
		 * Load data
		 */
		logger.info("Loading data...");
		Map<Integer, Table> tables = new HashMap<Integer, Table>();
		
		for(int i = 1; i <= n_runs; i++) {
			String file = String.format("%1$s/%2$s/%3$s", rootDir, i, propertyKey);
			try{
			tables.put(i, new Table(file));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		/*
		 * get row/column names
		 */
		Set<Double> rowNamesSet = new TreeSet<Double>();
		Set<Double> colNamesSet = new TreeSet<Double>();
		for(Table table : tables.values()) {
			for(double key : table.rowNames.keys())
				rowNamesSet.add(key);
			
			for(double key : table.colNames.keys())
				colNamesSet.add(key);
		}
		List<Double> rowNames = new ArrayList<Double>(rowNamesSet);
		List<Double> colNames = new ArrayList<Double>(colNamesSet);
		/*
		 * average
		 */
		Table avrTable = new Table(rowNames.size(), colNames.size());
		for (int avrRowIdx = 0; avrRowIdx < rowNames.size(); avrRowIdx++) {
			Double row = rowNames.get(avrRowIdx);
			
			for (int avrColIdx = 0; avrColIdx < colNames.size(); avrColIdx++) {
				Double col = colNames.get(avrColIdx);
				
				double sum = 0;
				int cnt = 0;
				for (Table table : tables.values()) {
					int rowIdx = -1;
					if (table.rowNames.containsKey(row))
						rowIdx = table.rowNames.get(row);
					int colIdx = -1;
					if (table.colNames.containsKey(col))
						colIdx = table.colNames.get(col);

					if (rowIdx > -1 && colIdx > -1) {
						double val = table.values[rowIdx][colIdx]; 
						if(!Double.isNaN(val)) {
							sum += val;
							cnt++;
						}
					}
				}
//				if(avrRowIdx == 0 && avrColIdx == 0)
//					System.out.println();
				
				avrTable.values[avrRowIdx][avrColIdx] = sum/(double)cnt;
			}
		}
		/*
		 * write table
		 */
		BufferedWriter writer = new BufferedWriter(new FileWriter(outputTable));
		
		for(Double col : colNames) {
			writer.write("\t");
			writer.write(col.toString());
		}
		writer.newLine();
		
		for(int rowIdx = 0; rowIdx < rowNames.size(); rowIdx++) {
			writer.write(rowNames.get(rowIdx).toString());
			for(int colIdx = 0; colIdx < colNames.size(); colIdx++) {
				writer.write("\t");
				double val = avrTable.values[rowIdx][colIdx];
				if(Double.isNaN(val))
					writer.write("NA");
				else
					writer.write(String.valueOf(val));
			}
			writer.newLine();
		}
		
		writer.close();
	}

	private static class Table {
		
		private double[][] values;
		
		private TDoubleIntHashMap rowNames = new TDoubleIntHashMap();
		
		private TDoubleIntHashMap colNames = new TDoubleIntHashMap();
		
		public Table(int rows, int cols) {
			values = new double[rows][cols];
		}
		
		public Table(String file) throws IOException {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String header = reader.readLine();
			if(header != null) {
					
			String[] tokens = header.split("\t");
			for(int i = 1; i < tokens.length; i++) {
				colNames.put(Double.parseDouble(tokens[i]), i-1);
			}
			
			String row = null;
			int rowIdx = 0;
			while((row = reader.readLine()) != null) {
				tokens = row.split("\t");
				rowNames.put(Double.parseDouble(tokens[0]), rowIdx);
				rowIdx++;
			}
			
			values = new double[rowNames.size()][colNames.size()];
			/*
			 * open file again and read values
			 */
			reader = new BufferedReader(new FileReader(file));
			row = reader.readLine();
			rowIdx = 0;
			while((row = reader.readLine()) != null) {
				tokens = row.split("\t");
				for(int col = 1; col < tokens.length; col++) {
					if("NA".equalsIgnoreCase(tokens[col]))
						values[rowIdx][col-1] = Double.NaN;
					else
						values[rowIdx][col-1] = Double.parseDouble(tokens[col]);
				}
				rowIdx++;
			}
			
			}
		}
	}
}
