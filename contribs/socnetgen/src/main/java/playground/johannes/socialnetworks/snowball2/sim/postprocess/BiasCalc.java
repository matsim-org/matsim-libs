/* *********************************************************************** *
 * project: org.matsim.*
 * BiasCalc.java
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

import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntIntHashMap;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * @author illenberger
 *
 */
public class BiasCalc {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		final int MAX_ITERATION = 20;
		String basedir = "";
		String nSamplesFile = "";
		/*
		 * read n_samples file
		 */
		TIntIntHashMap n_samples = new TIntIntHashMap();
		BufferedReader reader = new BufferedReader(new FileReader(nSamplesFile));
		
		String line = reader.readLine();
		while((line = reader.readLine()) != null) {
			String[] tokens = line.split("\t");
			int it = Integer.parseInt(tokens[0]);
			int n = Integer.parseInt(tokens[1]);
			n_samples.put(it, n);
		}
		/*
		 * read p_obs files
		 */
		TIntDoubleHashMap bias = new TIntDoubleHashMap();
		for(int i = 0; i < MAX_ITERATION; i++) {
			reader = new BufferedReader(new FileReader(String.format("%1$s/%2$s.pobs.txt", basedir, i)));
			line = reader.readLine();
			while((line = reader.readLine()) != null) {
				
			}
		}
	}

}
