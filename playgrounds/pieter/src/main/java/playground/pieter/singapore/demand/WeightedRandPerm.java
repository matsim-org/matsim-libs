/*
 * Copyright (c) 2003-2005 The BISON Project
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License version 2 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */
		
package playground.pieter.singapore.demand;

import java.util.NoSuchElementException;
import java.util.Random;

/**
* This class provides a weighted random permutation of indexes.
* Useful for weighted random sampling without replacement.
* The next sample is taken according to the weights given as a parameter
* to {@link #reset(int)}.
* The weights work as follows.
* The first sample is drawn according to the probability distribution
* defined by the (normalized) weights.
* After this the remaining k-1 elements and the associated k-1
* (re-normalized) weights
* define a new probability distribution, according to which the 2nd element
* is drawn, and so on.
*/
public class WeightedRandPerm{


// ======================= private fields ============================
// ===================================================================

/** Holds the weights that are used to initialize the permutation */
private final double[] w;

/** Holds the sum of the weights until the given index, inclusive. */
private final double[] wsum;

private int[] buffer = null;

/** Working array for calculating the permutation */ 
private double[] weights = null;

private int len = 0;

private int pointer = 0;

private double sum = 0.0;

private final Random r;


// ======================= initialization ============================
// ===================================================================


/** Set the source of randomness to use and the weights. You need to call
* {@link #reset} to fully initialize the object.
* @param r source of randomness
* @param weights The array that holds the weights for the calculation of the
* permutation. The length of the array will be an upper bound on the
* parameter {@link #reset} accepts. If {@link #reset} is called with a
* parameter less than the length of weights, the prefix of the same length
* is used.
* The vector elements must be positive, that is, zero is not accepted either.
*/
public WeightedRandPerm( Random r, double[] weights ) {

	this.r=r;
	w = weights.clone();
	wsum = weights.clone();
    this.weights = new double[w.length];
	buffer = new int[w.length];
	
	for(int i=0; i<w.length; ++i)
	{
		if( w[i] <= 0.0 ) throw new IllegalArgumentException(
			"weights should be positive: w["+i+"]="+w[i]);
	}
	
	for(int i=1; i<w.length; ++i) wsum[i]+=wsum[i-1];
}


// ======================= public methods ============================
// ===================================================================


/**
* It initiates a random weighted permutation of the integeres from 0 to k-1.
* It does not actually calculate the permutation.
* The permutation can be read using method {@link #next}.
* If the previous permutation was of the same length, it is more efficient.
* The weights set at construction time work as follows.
* The first sample is drawn according to the probability distribution
* defined by the (normalized) weights.
* After this the remaining k-1 elements and the associated k-1
* (re-normalized) weights
* define a new probability distribution, according to which the 2nd element
* is drawn, and so on.
* @param k the set is defined as 0,...,k-1
*/
public void reset(int k) {
	
	if( k<0 || k>w.length )
		throw new IllegalArgumentException(
			"k should be non-negative and <= "+w.length);
	
	pointer = k;
	sum = wsum[k-1];
	
	if( k != len )
	{
		// we need to initialize weights and buffer
		for(int i=0; i<k; ++i)
		{
			weights[i]=w[i];
			buffer[i]=i;
		}
		len=k;
	}
}

// -------------------------------------------------------------------

/**
* The first sample is drawn according to the probability distribution
* defined by the (normalized) weights.
* After this the remaining k-1 elements and the associated k-1
* (re-normalized) weights
* define a new probability distribution, according to which the 2nd element
* is drawn, and so on.
* @see #reset
*/
public int next() {
	
	if( pointer < 1 ) throw new NoSuchElementException();
	
	double d = sum*r.nextDouble();
	int i = pointer;
	double tmp = weights[i-1];
	while( tmp < d && i>1 ) tmp += weights[--i-1];
	
	// now i-1 is the selected element, we shift it to next position
	int a = buffer[i-1];
	double b = weights[i-1];
	buffer[i-1] = buffer[pointer-1];
	weights[i-1] = weights[pointer-1];
	buffer[pointer-1] = a;
	weights[pointer-1] = b;
	sum -= b;
	
	return buffer[--pointer];
}

// -------------------------------------------------------------------

boolean hasNext() { return pointer > 0; }

// -------------------------------------------------------------------


public static void main( String pars[] ) throws Exception {
	

	int k = pars.length;
	double w[] = new double[k];
	for(int i=0; i<k; ++i) w[i] = Double.parseDouble(pars[i]);
	
	WeightedRandPerm rp = new WeightedRandPerm(new Random(),w);
	rp.reset(k);
	for(int i=0; i<1000; ++i)
	{
		if(i%2==0) rp.reset(k);
		if(i%2==1) rp.reset(k-1);
		while(rp.hasNext()) System.out.print(rp.next()+" ");
		System.out.println();
	}
	
	System.out.println();
}

}

