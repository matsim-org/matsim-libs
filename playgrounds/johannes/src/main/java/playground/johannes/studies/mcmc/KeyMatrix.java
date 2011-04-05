/* *********************************************************************** *
 * project: org.matsim.*
 * k.java
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

import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author illenberger
 *
 */
public class KeyMatrix<T> {
	
	private Map<T, TObjectDoubleHashMap<T>> rows = new HashMap<T, TObjectDoubleHashMap<T>>();

	public void putValue(double value, T rowKey, T colKey) {
		TObjectDoubleHashMap<T> row = rows.get(rowKey);
		if(row == null) {
			row = new TObjectDoubleHashMap<T>();
			rows.put(rowKey, row);
		}
		
		row.put(colKey, value);
	}
	
	public double getValue(T rowKey, T colKey) {
		TObjectDoubleHashMap<T> row = rows.get(rowKey);
		if(row == null)
			return Double.NaN;
		else
			return row.get(colKey);
	}
	
	public void write(String filename) throws IOException {
		Set<T> rowKeys = new TreeSet<T>();
		Set<T> colKeys = new TreeSet<T>();
		
		for(Entry<T, TObjectDoubleHashMap<T>> entry : rows.entrySet()) {
			rowKeys.add(entry.getKey());
			TObjectDoubleHashMap<T> row = entry.getValue();
			TObjectDoubleIterator<T> it = row.iterator();
			for(int i = 0; i < row.size(); i++) {
				it.advance();
				colKeys.add(it.key());
			}
		}
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
		for(T colKey : colKeys) {
			writer.write("\t");
			writer.write(String.valueOf(colKey));
		}
		writer.newLine();
		
		for(T rowKey : rowKeys) {
			writer.write(String.valueOf(rowKey));
			TObjectDoubleHashMap<T> row = rows.get(rowKey);
			for(T colKey : colKeys) {
				writer.write("\t");
				double val = row.get(colKey);
				if(!Double.isNaN(val))
					writer.write(String.valueOf(val));
			}
			writer.newLine();
		}
		
		writer.close();
	}
	
}
