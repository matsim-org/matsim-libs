/* *********************************************************************** *
 * project: org.matsim.*
 * PRatioDensityPlot.java
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
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntObjectIterator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.contrib.sna.math.Distribution;

/**
 * @author illenberger
 *
 */
public class PRatioDensityPlot {

	public static void main(String args[]) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader("/Users/jillenberger/Work/shared-svn/projects/socialnets/papers/drafts/snowball2010/fig/data/wRatio.condmat100.txt"));
		
		String line = reader.readLine();
		int maxIter = 0;
		TIntObjectHashMap<Distribution> matrix = new TIntObjectHashMap<Distribution>();
		while((line = reader.readLine()) != null) {
			String tokens[] = line.split("\t");
			maxIter = Math.max(maxIter, tokens.length-1);
			for(int i = 0; i < tokens.length; i++) {
				Distribution values = matrix.get(i);
				if(values == null) {
					values = new Distribution();
					matrix.put(i, values);
				}
				
				values.add(Double.parseDouble(tokens[i]));
			}
		}
		
		TIntObjectHashMap<TDoubleDoubleHashMap> hists = new TIntObjectHashMap<TDoubleDoubleHashMap>();
		TIntObjectIterator<Distribution> it = matrix.iterator();
		Set<Double> keySet = new TreeSet<Double>();
		for(int i = 0; i< matrix.size(); i++) {
			it.advance();
			TDoubleDoubleHashMap hist = it.value().normalizedDistribution(0.2);
			hists.put(it.key(), hist);
			
			for(double key : hist.keys())
				keySet.add(key);
		}
		
		BufferedWriter writer = new BufferedWriter(new FileWriter("/Users/jillenberger/Work/shared-svn/projects/socialnets/papers/drafts/snowball2010/fig/data/wRatioDensity.condmat100.txt"));
		for(int i = 0; i <= maxIter; i++) {
			writer.write("\t");
			writer.write(String.valueOf(i));
		}
		writer.newLine();
		for(Double key : keySet) {
			writer.write(String.valueOf(key));
			for(int i = 0; i <= maxIter; i++) {
				writer.write("\t");
				writer.write(String.valueOf(hists.get(i).get(key)));
			}
			writer.newLine();
		}
		writer.close();
	}
}
