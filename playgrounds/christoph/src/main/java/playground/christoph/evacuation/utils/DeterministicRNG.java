/* *********************************************************************** *
 * project: org.matsim.*
 * DeterministicRNG.java
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

package playground.christoph.evacuation.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;

public class DeterministicRNG {

	private static final Logger log = Logger.getLogger(DeterministicRNG.class);
	private static List<Long> usedModFactors = new ArrayList<Long>();
	
	private final long hashCodeModFactor;
	
	/**
	 * Use values > 10^6. The value is used to calculate a modulo
	 */
	public DeterministicRNG(long hashCodeModFactor) {
		this.hashCodeModFactor = hashCodeModFactor;
		
		synchronized(usedModFactors) {
			if (usedModFactors.contains(hashCodeModFactor)) {
				log.warn("hashCodeModFactor is re-used: " + hashCodeModFactor + ". Ensure that this behavior is desired.");
			}
			usedModFactors.add(hashCodeModFactor);
		}
	}
	
	/**
	 * Creates a random double value between 0.0 and 1.0 based
	 * on an Id.
	 */
	public double idToRandomDouble(Id id) {
		return this.hashCodeToRandomDouble(id.hashCode());
	}
	
	/**
	 * Creates a random double value between 0.0 and 1.0 based
	 * on an integer hash value.
	 */
	public double hashCodeToRandomDouble(int hashCode) {
		
		/*
		 *  Small numbers represented as a String return small hash values.
		 *  By doing this operation, we create higher values that result
		 *  in larger differences between two input String (e.g. "1" and "2").
		 */
		hashCode ^= (hashCode << 13);
		hashCode ^= (hashCode >>> 17);
		hashCode ^= (hashCode << 5);

		Long modValue = hashCode % (hashCodeModFactor);
		double value = modValue.doubleValue() / (hashCodeModFactor);
		return Math.abs(value);
	}
	
	public static void main(String[] args) {
		DeterministicRNG rng = new DeterministicRNG(122456);
		Random random = MatsimRandom.getLocalInstance();
				
		Gbl.startMeasurement();
		double sum = 0.0;
		int iters = 100000000;
		for (int i = 0; i < iters; i++) {
			sum += rng.hashCodeToRandomDouble(i);
		}
		System.out.println(sum/iters);
		Gbl.printElapsedTime();
		
		Gbl.startMeasurement();
		sum = 0.0;
		for (int i = 0; i < iters; i++) {
			sum += random.nextDouble();
		}
		System.out.println(sum/iters);
		Gbl.printElapsedTime();
	}
}
