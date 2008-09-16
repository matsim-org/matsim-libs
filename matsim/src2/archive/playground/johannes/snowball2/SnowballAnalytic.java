/* *********************************************************************** *
 * project: org.matsim.*
 * SnowballAnalytic.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.johannes.snowball2;

/**
 * @author illenberger
 *
 */
public class SnowballAnalytic {

	private static double z = 8;
	
	private static double c = 0.4733;
	
	private static double M = 0.6475;
	
	private static double N = 70000;
	
	private static double n_0 = 10;
	
	private static double n_total;
	
	public static void main(String args[]) {
		System.out.println("0 : " + n_0);
		n_total = n_0;
		double n = calcNumVertex(1, 0);
		n_total += n;
		int i = 1;
		System.out.println(i + " : " + n_total);
		while(n_total < N) {
			i++;
			n = calcNumVertex(i, n_total);
			n_total += n;
			System.out.println(i + " : " + n_total);
		}
	}
	
	private static double calcNumVertex(int i, double n) {
		if(i == 1) {
			return n_0 * (z-1);
//		} else if(i == 2) {
//			return Math.ceil((n * (z-1) * (1-c)));// * Math.pow((1 - n_total/(double)N),1)));
		} else {
			return Math.ceil((n * M * (z-1) * (1-c) *  Math.pow((1 - n_total/(double)N),1)));
//			return Math.ceil((n * M * (z-1-(z*c)) *  Math.pow((1 - n_total/(double)N),1)));
//			return Math.ceil((M * Math.pow((z-1),i-1) * z * (1-c) *  Math.pow((1 - n_total/(double)N),1)));
		}
			
	}
}
