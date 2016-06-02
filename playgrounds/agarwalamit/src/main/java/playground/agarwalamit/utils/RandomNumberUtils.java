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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.MatsimRandom;

/**
 * A class to generate pseudo random numbers for given discrete probability distribution and 
 * random 
 * 
 * 
 * http://introcs.cs.princeton.edu/java/stdlib/StdRandom.java.html
 * 
 * @author amit
 */

public class RandomNumberUtils {

	private static final Random rnd = MatsimRandom.getRandom();
	private static final Logger LOG = Logger.getLogger(RandomNumberUtils.class);

	public static void main(String[] args) {
		// a small example
		SortedMap<String, Double> xs = new TreeMap<>();
		xs.put("HBW", 0.45);
		xs.put("HBE", 0.34);
		xs.put("HBS", 0.04);
		xs.put("HBO", 0.17);

		int lowerBound = 27;
		int upperBound = 42;
		int totalRequiredNumber = 100;

		//		SortedMap<String, List<Integer> > ns = new AARandomNumberGenerator(xs).getRandomNumbers(lowerBound, upperBound, totalRequiredNumber);
		//
		//		for (String s : ns.keySet()){
		//			System.out.println("For string " +s+ " the zones are "+ ns.get(s).toString());
		//		}

		List<Integer> numbers = new ArrayList<>(); 


		for (int i=0; i< totalRequiredNumber ; i++) {
			numbers.add(getRNFromInverseTransformation(10));
		}

		for (int i=0; i<= 10 ; i++) {
			System.out.println(i+" is "+Collections.frequency(numbers, i));
		}
	}

	/**
	 * Taken from http://stackoverflow.com/questions/5969447/java-random-integer-with-non-uniform-distribution
	 */
	public static int getRNFromInverseTransformation(int upperBound){

		int randomMultiplier = upperBound * (upperBound + 1) / 2; // all possible values inside the triangle

		int randomInt = rnd.nextInt(randomMultiplier);

		int linearRandomNumber = 0;
		for(int i = upperBound; randomInt >= 0; i--){
			randomInt -= i;
			linearRandomNumber++;
		}

		return linearRandomNumber;
	}

	public static double getRNNormallyDistributed(final double mean, final double standardDeviation){

		return rnd.nextGaussian() * standardDeviation + mean ;
	}


	/**
	 * @param discreteDistribution
	 * @return string from the keys randomly based on the given distribution.
	 */
	public static String getRandomStringFromDiscreteDistribution(final SortedMap<String, Double> discreteDistribution) {
		//Checks
		double sum = MapUtils.doubleValueSum(discreteDistribution);
		SortedMap<String, Double> probs = new TreeMap<>();
		if (sum <= 0. ) throw new RuntimeException("Sum of the values for given distribution is "+sum+". Aborting ...");
		else if (sum != 1 ) {
			LOG.warn("Sum of the values for given distribution is not equal to one. Converting them ...");
			probs.putAll( MapUtils.getDoublePercentShare( discreteDistribution ) );
		} else probs.putAll( discreteDistribution );

		// process
		do {
			double d = rnd.nextDouble();
			double s = 0;
			for(Entry<String, Double> e : probs.entrySet()) {
				s += e.getValue();
				if(s >= d) return e.getKey();
			}
		} while(true);
	}

	/**
	 * This will give a distribution close to given discrete distribution by first generating required Strings based on the share of discreteDistribution
	 * and then shuffle them uniformly.
	 */
	public static List<String>  getRandomStringsFromDiscreteDistribution(final SortedMap<String, Double> discreteDistribution, final int requiredRandomNumbers){
		String [] strs = new String [requiredRandomNumbers];
		
		//Checks
		double sum = MapUtils.doubleValueSum(discreteDistribution);
		SortedMap<String, Double> probs = new TreeMap<>();
		if (sum <= 0. ) throw new RuntimeException("Sum of the values for given distribution is "+sum+". Aborting ...");
		else if (sum != 1 ) {
			LOG.warn("Sum of the values for given distribution is not equal to one. Converting them ...");
			probs.putAll( MapUtils.getDoublePercentShare( discreteDistribution ) );
		} else probs.putAll( discreteDistribution );

		// process
		int idx = 0;
		for(String str : probs.keySet()) { // fill the array
			double requiredNrs = Math.round( probs.get(str) * requiredRandomNumbers );
			for(int i = 0; i < requiredNrs ; i++) {
				strs[idx] = str;
				idx++;
			}
		}
		
		// shuffle uniformly and return 
		return Arrays.asList( RandomNumberUtils.shuffleUniformly(strs) );
	}
	
	/**
	 * One could use shuffle call in Collections however, following is useful to get reproducible results.
	 */
	public static String [] shuffleUniformly(final String [] inArray){
		int length = inArray.length;
		String [] shuffledArray = new String [length];
		
        for (int i = 0; i < length; i++) {
            int r = i + rnd.nextInt(length-i);  // a random number between i and length-1
            shuffledArray[i] = inArray[r];
        }
		return shuffledArray;
	}


//	/**
//	 * @param lowerBound inclusive
//	 * @param upperBound inclusive
//	 * @param requiredRandomNumbers higher is number ==> better is the distribution
//	 * @return
//	 */
//	public SortedMap<String, List<Integer> > getRandomNumbers(final int lowerBound, final int upperBound, final int requiredRandomNumbers){
//		SortedMap<String, List<Integer> > group2RandomNumbers = new TreeMap<>(); 
//
//		for (int i = 0 ; i < requiredRandomNumbers; i++){
//			// this number lies between the lower, upper bounds
//			int rndNrInRange = lowerBound  + this.rnd.nextInt( upperBound- lowerBound + 1);
//
//			// based on above number, find where it lies on a line with lower bound = 0 and upper bounds = 1 i.e. converting betwen 0,1
//			double rndPortion = Double.valueOf( rndNrInRange - lowerBound ) / Double.valueOf( upperBound - lowerBound);
//
//			for( Entry<String, Double> e: cummulativeGroupProbabilities.entrySet() ) {
//
//				if (rndPortion <= e.getValue()) { // found interval
//
//					if (group2RandomNumbers.get(e.getKey())!=null) {// already exists
//						group2RandomNumbers.get(e.getKey()).add(rndNrInRange);
//					} else { // first element in the list
//						List<Integer> l  = new ArrayList<>();
//						l.add(rndNrInRange);
//						group2RandomNumbers.put(e.getKey(), l);
//					}
//					break;
//				}
//			}
//		}
//		return group2RandomNumbers;
//	}
}