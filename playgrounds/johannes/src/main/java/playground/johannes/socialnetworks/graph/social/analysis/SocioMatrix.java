/* *********************************************************************** *
 * project: org.matsim.*
 * SocioMatrix2.java
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
package playground.johannes.socialnetworks.graph.social.analysis;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * @author illenberger
 *
 */
public class SocioMatrix<K> {

	private double[][] m;
	
	private List<K> keys;
	
	public SocioMatrix(List<K> keys) {
		m = new double[keys.size()][keys.size()];
		this.keys = keys;
	}
	
	public List<K> getKeys() {
		return keys;
	}
	
	public void setValue(double value, K key1, K key2) {
		int i = keys.indexOf(key1);
		int j = keys.indexOf(key2);
		
		m[i][j] = value;
	}
	
	public double getValue(K key1, K key2) {
		int i = keys.indexOf(key1);
		int j = keys.indexOf(key2);
		
		return m[i][j];
	}
	
	public void adjustValue(K key1, K key2, double adjustValue) {
		int i = keys.indexOf(key1);
		int j = keys.indexOf(key2);
		
		m[i][j] += adjustValue;
	}
	
	double[][] getMatrix() {
		return m;
	}
	
	public void toFile(String file) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		
		for(K value : keys) {
			writer.write("\t");
			writer.write(value.toString());
		}
		writer.newLine();
		
		for(int i = 0; i < m.length; i++) {
			writer.write(keys.get(i).toString());
			
			for(int j = 0; j < m.length; j++) {
				writer.write("\t");
				writer.write(String.format(Locale.US, "%1$.2f", m[i][j]));
				
			}
			writer.newLine();
		}
		
		writer.close();
	}
}
