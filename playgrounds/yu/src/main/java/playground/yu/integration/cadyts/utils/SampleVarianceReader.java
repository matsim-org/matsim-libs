/* *********************************************************************** *
 * project: org.matsim.*
 * SampleVarianceReader.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.yu.integration.cadyts.utils;

import java.util.Map;
import java.util.TreeMap;

import playground.yu.utils.io.SimpleReader;

public class SampleVarianceReader {
	public static void main(String[] args) {
		SampleVarianceReader svReader = new SampleVarianceReader(
				"test/input/2car1ptRoutes/prepareCounts/simpleVariance.log");
		svReader.read();
		Map<String, Map<Integer, Double>> countsSampleVariances = svReader
				.getCountsSampleVariances();
		System.out.println(countsSampleVariances.toString());
	}

	private final SimpleReader reader;

	private final Map<String/* countId */, Map<Integer/* timeStep */, Double/*
																		 * sample
																		 * variance
																		 */>> countsSampleVariances = new TreeMap<String, Map<Integer, Double>>();

	public SampleVarianceReader(String filename) {
		reader = new SimpleReader(filename);
	}

	public Map<String, Map<Integer, Double>> getCountsSampleVariances() {
		return countsSampleVariances;
	}

	public void read() {
		String line = reader.readLine();
		while (line != null) {
			line = reader.readLine();
			if (line == null) {
				break;
			}
			// ////////////////////////////////////////////
			String[] words = line.split("\t");
			if (words.length != 3) {
				throw new RuntimeException(
						"There should be 3 columns in this sample variance file!");
			}
			String countId = words[0];
			Integer timeStep = Integer.parseInt(words[1]);
			double spVar = Double.parseDouble(words[2]);
			Map<Integer, Double> timeSampleVariances = countsSampleVariances
					.get(countId);
			if (timeSampleVariances == null) {
				timeSampleVariances = new TreeMap<Integer, Double>();
				countsSampleVariances.put(countId, timeSampleVariances);
			}
			timeSampleVariances.put(Integer.parseInt(words[1])/* timeStep */,
					Double.parseDouble(words[2])/* sample variance */);
			// ////////////////////////////////////////////
		}
		reader.close();
	}
}
