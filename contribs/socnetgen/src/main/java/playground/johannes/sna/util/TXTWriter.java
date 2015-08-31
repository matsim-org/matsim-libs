/* *********************************************************************** *
 * project: org.matsim.*
 * TXTWriter.java
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
package playground.johannes.sna.util;

import gnu.trove.TDoubleArrayList;
import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TDoubleObjectHashMap;
import gnu.trove.TDoubleObjectIterator;
import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.core.utils.collections.Tuple;

/**
 * @author illenberger
 *
 */
public class TXTWriter {

	private static final String TAB = "\t";
	
	private static final String NA = "NA";
	
	public static void writeMap(TDoubleDoubleHashMap map, String keyCol, String valCol, String file) throws IOException {
		writeMap(map, keyCol, valCol, file, false);
	}
	
	public static void writeMap(TDoubleDoubleHashMap map, String keyCol, String valCol, String file, boolean descending) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		
		writer.write(keyCol);
		writer.write(TAB);
		writer.write(valCol);
		writer.newLine();
		
		double[] keys = map.keys();
		Arrays.sort(keys);
		if(descending)
			ArrayUtils.reverse(keys);
		
		for(double key : keys) {
			writer.write(String.valueOf(key));
			writer.write(TAB);
			writer.write(String.valueOf(map.get(key)));
			writer.newLine();
		}
		
		writer.close();
	}
	
	public static void writeMap(TObjectDoubleHashMap<String> map, String keyCol, String valCol, String file, boolean sortValues) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		
		writer.write(keyCol);
		writer.write(TAB);
		writer.write(valCol);
		writer.newLine();
		
		String[] keys = map.keys(new String[map.size()]);
		if(sortValues) {
			List<Tuple<String, Double>> list = new LinkedList<Tuple<String,Double>>();
			TObjectDoubleIterator<String> it = map.iterator();
			for(int i = 0; i < map.size(); i++) {
				it.advance();
				list.add(new Tuple<String, Double>(it.key(), it.value()));
			}
			
			Collections.sort(list, new Comparator<Tuple<String, Double>>() {

				@Override
				public int compare(Tuple<String, Double> o1, Tuple<String, Double> o2) {
					double result = o1.getSecond() - o2.getSecond();
					if(result == 0) {
						if(o1.getFirst().equals(o2.getFirst()))
							return 0;
						else
							return o1.hashCode() - o2.hashCode();
					} else if(result < 0)
						return 1;
					else
						return -1;
				}
			});
			
			for(int i = 0; i < list.size(); i++) {
				keys[i] = list.get(i).getFirst();
			}
		} else {
			Arrays.sort(keys);
		}
		
		for(String key : keys) {
			writer.write(key);
			writer.write(TAB);
			writer.write(String.valueOf(map.get(key)));
			writer.newLine();
		}
		
		writer.close();
	}
	
	public static void writeMap(TObjectDoubleHashMap<String> map, String keyCol, String valCol, String file) throws IOException {
		writeMap(map, keyCol, valCol, file, false);
	}
	
	public static void writeBoxplotStats(TDoubleObjectHashMap<DescriptiveStatistics> table, String file) throws IOException {
		TDoubleObjectIterator<DescriptiveStatistics> it = table.iterator();
		TDoubleObjectHashMap<double[]> newTable = new TDoubleObjectHashMap<double[]>();
		for(int i = 0; i < table.size(); i++) {
			it.advance();
			newTable.put(it.key(), it.value().getValues());
		}
		
		writeBoxplot(newTable, file);
		
	}
	
	public static void writeBoxplot(TDoubleObjectHashMap<double[]> table, String file) throws IOException {
		int maxSize = 0;
		TDoubleObjectIterator<double[]> it = table.iterator();
		for(int i = 0; i < table.size(); i++) {
			it.advance();
			maxSize = Math.max(maxSize, it.value().length);
		}
		
		double keys[] = table.keys();
		Arrays.sort(keys);
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		for(int k = 0; k < keys.length; k++) {
			writer.write(String.valueOf(keys[k]));
			if(k + 1 < keys.length)
				writer.write("\t");
		}
		writer.newLine();
		
		for(int i = 0; i < maxSize; i++) {
			for(int k = 0; k < keys.length; k++) {
				double[] list = table.get(keys[k]);
				if(i < list.length) {
					writer.write(String.valueOf(list[i]));
				} else {
					writer.write(NA);
				}
				if(k + 1 < keys.length)
					writer.write(TAB);
			}
			writer.newLine();
		}
		writer.close();
	}
	
	public static void writeScatterPlot(TDoubleObjectHashMap<DescriptiveStatistics> table, String file) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		
		TDoubleObjectIterator<DescriptiveStatistics> it = table.iterator();
		for(int i = 0; i < table.size(); i++) {
			it.advance();
			double[] vals = it.value().getValues();
			for(int j = 0; j < vals.length; j++) {
				writer.write(String.valueOf(it.key()));
				writer.write(TAB);
				writer.write(String.valueOf(vals[j]));
				writer.newLine();
			}
		}
		writer.close();
	}
	
	public static void writeStatistics(TDoubleObjectHashMap<DescriptiveStatistics> statsMap, String xLab, String file) throws IOException {
		double[] keys = statsMap.keys();
		Arrays.sort(keys);
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		
		writer.write(xLab);
		writer.write(TAB);
		writer.write("mean");
		writer.write(TAB);
		writer.write("median");
		writer.write(TAB);
		writer.write("min");
		writer.write(TAB);
		writer.write("max");
		writer.write(TAB);
		writer.write("n");
		writer.newLine();
		
		for(double key : keys) {
			DescriptiveStatistics stats = statsMap.get(key);
			
			writer.write(String.valueOf(key));
			writer.write(TAB);
			writer.write(String.valueOf(stats.getMean()));
			writer.write(TAB);
			writer.write(String.valueOf(stats.getPercentile(50)));
			writer.write(TAB);
			writer.write(String.valueOf(stats.getMin()));
			writer.write(TAB);
			writer.write(String.valueOf(stats.getMax()));
			writer.write(TAB);
			writer.write(String.valueOf(stats.getN()));
			writer.newLine();
		}
		
		writer.close();
	}
	
	public static void writeStatistics(Map<String, DescriptiveStatistics> statsMap, String filename) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
		
		writer.write("property");
		writer.write(TAB);
		writer.write("mean");
		writer.write(TAB);
		writer.write("median");
		writer.write(TAB);
		writer.write("min");
		writer.write(TAB);
		writer.write("max");
		writer.write(TAB);
		writer.write("n");
		writer.newLine();
		writer.newLine();
		
		SortedMap<String, DescriptiveStatistics> sortedMap = new TreeMap<String, DescriptiveStatistics>(statsMap);
		for(Entry<String, DescriptiveStatistics> entry : sortedMap.entrySet()) {
			writer.write(entry.getKey());
			writer.write("\t");
			writer.write(String.valueOf(entry.getValue().getMean()));
			writer.write("\t");
			writer.write(String.valueOf(entry.getValue().getPercentile(50)));
			writer.write("\t");
			writer.write(String.valueOf(entry.getValue().getMin()));
			writer.write("\t");
			writer.write(String.valueOf(entry.getValue().getMax()));
			writer.write("\t");
			writer.write(String.valueOf(entry.getValue().getN()));
			writer.write("\t");
			writer.write(String.valueOf(entry.getValue().getVariance()));
			writer.newLine();
		}
		
		writer.close();
	}
	
	public static void writeScatterPlot(TDoubleArrayList col1, TDoubleArrayList col2, String name1, String name2, String filename) throws IOException {
		if(col1.size() != col2.size()) {
			throw new RuntimeException(String.format("Unequal numer of rows (col1:%s, col2:%s)", col1.size(), col2.size()));
		}
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
		
		writer.write(name1);
		writer.write("\t");
		writer.write(name2);
		writer.newLine();
		
		for(int i = 0; i < col1.size(); i++) {
			writer.write(String.valueOf(col1.get(i)));
			writer.write("\t");
			writer.write(String.valueOf(col2.get(i)));
			writer.newLine();
		}
		
		writer.close();
	}
}
