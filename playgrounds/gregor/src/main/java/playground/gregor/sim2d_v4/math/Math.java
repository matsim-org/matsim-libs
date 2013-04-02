/* *********************************************************************** *
 * project: org.matsim.*
 * Math.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.gregor.sim2d_v4.math;



/**
 * this class provides fast implementation math algorithms/functions and/or fast approximations respective
 * @author laemmel
 *
 */
public abstract class Math {

	/**
	 * Fast approximation of the exponential function 
	 * see http://www.javamex.com/tutorials/math/exp.shtml
	 * @param x
	 * @return exp(x)
	 */
	public static double exp(double x) {
		x = 1d + x / 256d;
		x *= x; x *= x; x *= x; x *= x;
		x *= x; x *= x; x *= x; x *= x;
		return x;
	}

	/**
	 * Fast approximation of the exponential function 
	 * see http://www.javamex.com/tutorials/math/exp.shtml
	 * @param x
	 * @return exp(x)
	 */
	public static float exp(float x) {
		x = 1f + x / 256f;
		x *= x; x *= x; x *= x; x *= x;
		x *= x; x *= x; x *= x; x *= x;
		return x;
	}



	//	public static void main(String [] args) {
	//		int MAX = 1000000;
	//		List<Double> rnds = new ArrayList<Double>();
	//		List<Float> frnds = new ArrayList<Float>();
	//		for (int i = 0; i < MAX; i++) {
	//			rnds.add((MatsimRandom.getRandom().nextDouble()-.5)*20);
	//			double dbl = rnds.get(i);
	//			frnds.add((double)dbl);
	//		}
	//		for (int i = 0; i < MAX; i++) {
	//			double [] dbl = new double[MAX];
	//			for (int j = 0; j < MAX; j++) {
	//				dbl[j] = 0;
	//			}
	////			long start = System.nanoTime();
	////			for (int j = 0; j < MAX; j++) {
	////				double rnd = rnds.get(j);
	////				double exp = java.lang.Math.exp(rnd);
	////				dbl[j] += exp;
	////			}
	////			long end = System.nanoTime();
	////			System.out.println("Math.exp() took:\t" + (end-start)/1000);
	////			
	////			start = System.nanoTime();
	////			for (int j = 0; j < MAX; j++) {
	////				double rnd = rnds.get(j);
	////				double exp = exp(rnd);
	////				dbl[j] -= exp;
	////			}
	////			end = System.nanoTime();
	////			System.out.println("exp() took:\t\t" + (end-start)/1000);
	//			
	//			long start = System.nanoTime();
	//			for (int j = 0; j < MAX; j++) {
	//				double rnd = frnds.get(j);
	//				double exp = (double) java.lang.Math.exp(rnd);
	//				dbl[j] -= exp;
	//			}
	//			long end = System.nanoTime();
	//			System.out.println("Math.exp() took:\t" + (end-start)/1000);
	//			
	//			start = System.nanoTime();
	//			for (int j = 0; j < MAX; j++) {
	//				double rnd = frnds.get(j);
	//				double exp = exp(rnd);
	//				dbl[j] += exp;
	//			}
	//			end = System.nanoTime();
	//			System.out.println("exp() took:\t\t" + (end-start)/1000);
	//			
	//			Arrays.sort(dbl);
	//			System.out.println("median:" + dbl[dbl.length/2]);
	//		}
	//		
	//		
	//	}
}
