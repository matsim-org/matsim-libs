/* *********************************************************************** *
 * project: org.matsim.*
 * MyPermutation.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.southafrica.utilities;

import org.matsim.core.gbl.MatsimRandom;

/**
 * Class to generate a random permutation of integers.
 *
 * @author jwjoubert
 */
public class RandomPermutation {

	protected RandomPermutation() {
	}
	
	/**
	 * Implementation to get a random permutation of integers <code>1..n</code>.
	 *  
	 * @param n the largest integer in the permutation.
	 * @return
	 */
	public static int[] getRandomPermutation(int n){
		/* Add the sequential integers to an array. */
		int[] a = new int[n];
		for(int i = 1; i <= n; i++){
			a[i-1] = i;
		}

		/* Shuffle each position in the array with a random other position. */
		int[] b = (int[])a.clone();
		for(int c = b.length-1; c >= 0; c--){
			int d = (int)Math.floor(MatsimRandom.getRandom().nextDouble() * (c+1));
			int tmp = b[d];
			b[d] = b[c];
			b[c] = tmp;
		}
		return b;
	}


}