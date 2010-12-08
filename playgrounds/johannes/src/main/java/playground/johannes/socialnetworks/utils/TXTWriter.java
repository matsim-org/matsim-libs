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
package playground.johannes.socialnetworks.utils;

import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TDoubleObjectHashMap;
import gnu.trove.TDoubleObjectIterator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

/**
 * @author illenberger
 *
 */
public class TXTWriter {

	private static final String TAB = "\t";
	
	private static final String NA = "NA";
	
	public static void writeMap(TDoubleDoubleHashMap map, String keyCol, String valCol, String file) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		
		writer.write(keyCol);
		writer.write(TAB);
		writer.write(valCol);
		writer.newLine();
		
		double[] keys = map.keys();
		Arrays.sort(keys);
		
		for(double key : keys) {
			writer.write(String.valueOf(key));
			writer.write(TAB);
			writer.write(String.valueOf(map.get(key)));
			writer.newLine();
		}
		
		writer.close();
	}
	
	public static void writeStatsTable(TDoubleObjectHashMap<DescriptiveStatistics> table, String file) throws IOException {
		TDoubleObjectIterator<DescriptiveStatistics> it = table.iterator();
		TDoubleObjectHashMap<double[]> newTable = new TDoubleObjectHashMap<double[]>();
		for(int i = 0; i < table.size(); i++) {
			it.advance();
			newTable.put(it.key(), it.value().getValues());
		}
		
		writeDoubleTable(newTable, file);
		
	}
	public static void writeDoubleTable(TDoubleObjectHashMap<double[]> table, String file) throws IOException {
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
}
