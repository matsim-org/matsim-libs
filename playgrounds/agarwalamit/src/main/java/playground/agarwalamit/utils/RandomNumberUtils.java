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
 * other distributions. 
 * 
 * This will use random from MatsimRandom.
 * 
 * A good overview of some other possibilities -- http://introcs.cs.princeton.edu/java/stdlib/StdRandom.java.html
 * 
 * @author amit
 */

public class RandomNumberUtils {

	private static final Random rnd = MatsimRandom.getRandom();
	private static final Logger LOG = Logger.getLogger(RandomNumberUtils.class);

	private RandomNumberUtils(){}
	
	public static void main(String[] args) {
		int totalRequiredNumber = 100;
		
		// example - 1
		List<Integer> numbers = new ArrayList<>(); 
		for (int i=0; i< totalRequiredNumber ; i++) {
			numbers.add(RandomNumberUtils.getRNFromInverseTransformation(10));
		}

		for (int i=0; i<= 10 ; i++) {
			System.out.println(i+" is "+Collections.frequency(numbers, i));
		}
		
		//example - 2
		SortedMap<String, Double> inMap = new TreeMap<>();
		inMap.put("car", 0.1);
		inMap.put("bike", 0.4);
		inMap.put("pt", 0.2);
		inMap.put("walk", 0.3);
		RandomNumberUtils.getRandomStringsFromDiscreteDistribution(inMap, 10);
		
		// example - 3
		List<String> modesTesting = new ArrayList<>();
		for(int ii = 0; ii<10; ii++){
			modesTesting.add(RandomNumberUtils.getRandomStringFromDiscreteDistribution(inMap));
		}
		
		// compare example - 2 and example - 3
		for (String s : inMap.keySet()){
			LOG.info("Share of "+s+" in input and output -- "+ inMap.get(s)+" and "+ Collections.frequency(modesTesting, s)/10.);
		}
	}
	
	public static Random getRandom() {
		//this will allow to use the other default calls within Random
		return rnd;
	}
	
	/**
	 * @param lowerBound inclusive
	 * @param upperBound inclusive
	 * @return
	 */
	public static int getUniformlyRandomNumber(int lowerBound, int upperBound){
		if (upperBound < lowerBound) throw new IllegalArgumentException("Upper bound is smaller than lower bound. Aborting...");
		return lowerBound + rnd.nextInt(upperBound - lowerBound + 1);
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
		if (sum <= 0. ) throw new IllegalArgumentException("Sum of the values for given distribution is "+sum+". Aborting ...");
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
		if (sum <= 0. ) throw new IllegalArgumentException("Sum of the values for given distribution is "+sum+". Aborting ...");

		// process
		int idx = 0;
		for(String str : discreteDistribution.keySet()) { // fill the array
			double requiredNrs = Math.round( ( discreteDistribution.get(str)/sum) * requiredRandomNumbers ); 
 			for(int i = 0; i <  requiredNrs && idx < requiredRandomNumbers ; i++) {
				strs[idx] = str;
				idx++;
			}
		}
		//fixing null elements due to rounding errors
		while ( idx < requiredRandomNumbers  ) {
			List<String > strings = new ArrayList<>(discreteDistribution.keySet());
			strs[idx] = strings.get( rnd.nextInt( strings.size() ));
			idx++;
		}
		
		//shuffle uniformly 
		List<String> outArray = Arrays.asList( RandomNumberUtils.shuffleUniformly(strs) ); 

		//cross check and print
		for (String s : discreteDistribution.keySet()){
			LOG.info("Share of "+s+" in input and output -- "+ discreteDistribution.get(s)/sum+" and "+ (double) Collections.frequency(outArray, s)/outArray.size());
		}
		
		//Check if due to rounding any element is null
		if (outArray.contains(null)) {
			throw new RuntimeException("Some element in the list is null. Aborting ...");
		}
		
		return outArray;
	}
	
	/**
	 * One could use shuffle call in Collections however, following is useful to get reproducible results.
	 */
	public static String [] shuffleUniformly(final String [] inArray){
		int length = inArray.length;
		String [] shuffledArray = inArray;
		
        for (int i = 0; i < length; i++) {
            int r = i + rnd.nextInt(length-i);  // a random number between i and length-1
            String tempVal = shuffledArray[i];
            shuffledArray[i] = shuffledArray[r];
            shuffledArray[r] = tempVal;
        }
		return shuffledArray;
	}
}