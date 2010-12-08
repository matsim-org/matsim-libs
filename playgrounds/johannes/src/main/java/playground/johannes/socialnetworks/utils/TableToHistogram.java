/* *********************************************************************** *
 * project: org.matsim.*
 * TableToHistogram.java
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

import gnu.trove.TDoubleArrayList;
import gnu.trove.TDoubleDoubleHashMap;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.matsim.contrib.sna.math.Discretizer;
import org.matsim.contrib.sna.math.FixedSampleSizeDiscretizer;

import playground.johannes.socialnetworks.statistics.Correlations;

/**
 * @author illenberger
 *
 */
public class TableToHistogram {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader("/Users/jillenberger/Work/shared-svn/projects/socialnets/papers/drafts/epj-vwproceedings/fig/data/freqModeData.txt"));
		
		int distIdx = 5;
		int modeIdx = 6;
		
		TDoubleArrayList values1 = new TDoubleArrayList();
		TDoubleArrayList values2 = new TDoubleArrayList();
		
		String line = reader.readLine();
		while((line = reader.readLine()) != null) {
			String tokens[] = line.split("\t");
			if(!tokens[distIdx].equalsIgnoreCase("NA") && !tokens[modeIdx].equalsIgnoreCase("NA")) {
				double d = Double.parseDouble(tokens[distIdx]);
				d = Math.ceil(d)*1000;
//				d = d *1000;
				if(d > 0) {
					values1.add(d);
					values2.add(Double.parseDouble(tokens[modeIdx]));
				}
			}
		}
		
		Discretizer disc = FixedSampleSizeDiscretizer.create(values1.toNativeArray(), 100);
		TDoubleDoubleHashMap map = Correlations.mean(values1.toNativeArray(), values2.toNativeArray(), disc);
		Correlations.writeToFile(map, "/Users/jillenberger/Work/shared-svn/projects/socialnets/papers/drafts/epj-vwproceedings/fig/data/freq_phys.txt", "distance", "frequency");
	}

}
