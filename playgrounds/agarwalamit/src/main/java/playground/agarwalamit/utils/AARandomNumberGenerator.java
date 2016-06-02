/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.MatsimRandom;

/**
 * A class to generate pseudo random numbers for given discrete probability distribution and 
 * random d
 * @author amit
 */

public class AARandomNumberGenerator {

	// group and their probabilities of occurances
	private static SortedMap<String, Double> groupProbs ;
	private static SortedMap<String, Double> cummulativeGroupProbabilities ;
	private final Random rnd;

	private static final Logger LOG = Logger.getLogger(AARandomNumberGenerator.class);

	public AARandomNumberGenerator(final SortedMap<String, Double> groupProbabilities){
		groupProbs = groupProbabilities;
		rnd = MatsimRandom.getRandom();
		getCummulativeDistribution();
	}

	public static void main(String[] args) {
		// a small example
		SortedMap<String, Double> xs = new TreeMap<>();
		xs.put("HBW", 0.45);
		xs.put("HBE", 0.34);
		xs.put("HBS", 0.04);
		xs.put("HBO", 0.17);

		int lowerBound = 27;
		int upperBound = 42;
		int totalRequiredNumber = 10;

		SortedMap<String, List<Integer> > ns = new AARandomNumberGenerator(xs).getRandomNumbers(lowerBound, upperBound, totalRequiredNumber);

		for (String s : ns.keySet()){
			System.out.println("For string " +s+ " the zones are "+ ns.get(s).toString());
		}
	}

	public List<String>  getRandomDistribution(final int requiredRandomNumbers){
		List<String> rndDistri = new ArrayList<>();
		for (int i = 0 ; i < requiredRandomNumbers; i++){
			double rndPortion =  this.rnd.nextDouble();

			for( Entry<String, Double> e: cummulativeGroupProbabilities.entrySet() ) {
				if (rndPortion <= e.getValue()) { // found interval
					rndDistri.add(e.getKey());
					break;
				}
			}
		}
		return rndDistri;
	}

	/**
	 * @param lowerBound inclusive
	 * @param upperBound inclusive
	 * @param requiredRandomNumbers higher is number ==> better is the distribution
	 * @return
	 */
	public SortedMap<String, List<Integer> > getRandomNumbers(final int lowerBound, final int upperBound, final int requiredRandomNumbers){
		SortedMap<String, List<Integer> > group2RandomNumbers = new TreeMap<>(); 

		for (int i = 0 ; i < requiredRandomNumbers; i++){
			// this number lies between the lower, upper bounds
			int rndNrInRange = lowerBound  + this.rnd.nextInt( upperBound- lowerBound + 1);

			// based on above number, find where it lies on a line with lower bound = 0 and upper bounds = 1 i.e. converting betwen 0,1
			double rndPortion = Double.valueOf( rndNrInRange - lowerBound ) / Double.valueOf( upperBound - lowerBound);

			for( Entry<String, Double> e: cummulativeGroupProbabilities.entrySet() ) {

				if (rndPortion <= e.getValue()) { // found interval

					if (group2RandomNumbers.get(e.getKey())!=null) {// already exists
						group2RandomNumbers.get(e.getKey()).add(rndNrInRange);
					} else { // first element in the list
						List<Integer> l  = new ArrayList<>();
						l.add(rndNrInRange);
						group2RandomNumbers.put(e.getKey(), l);
					}
					break;
				}
			}
		}
		return group2RandomNumbers;
	}

	private static void getCummulativeDistribution(){
		cummulativeGroupProbabilities =  new TreeMap<>() ;
		double sum = 0.; 
		
		for( String str : groupProbs.keySet() ){
			sum = sum+groupProbs.get(str);
			cummulativeGroupProbabilities.put(str, sum);
		}

		if (sum != 1) {
			LOG.warn("Sum of input probabilities is not equal to 1. Assuming that these are number, converting them to shares.");

			double sumOfNumbers = MapUtils.doubleValueSum(groupProbs);
			sum = 0.;
			for( String str : groupProbs.keySet() ){
				sum = sum + groupProbs.get(str) / sumOfNumbers;
				cummulativeGroupProbabilities.put(str, sum);
			}
		}
	}
}