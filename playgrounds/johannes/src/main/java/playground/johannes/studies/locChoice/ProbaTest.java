/* *********************************************************************** *
 * project: org.matsim.*
 * ProbaTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.studies.locChoice;

import java.util.Random;

/**
 * @author illenberger
 *
 */
public class ProbaTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Random random = new Random();
		
		double t0 = Double.MAX_VALUE;
		for(int i = 0; i < 100000; i++) {
			double t1 = Double.POSITIVE_INFINITY;
			
			boolean accept = false;
			while(!accept) {
				double x = random.nextDouble() + 1;
				double y = Math.pow(x, -1.5);
				
				if(y > random.nextDouble()) {
					accept = true;
					t1 = x;
				}
			}
			
			if(t1 < t0) {
				t0 = t1;
				System.out.println(String.format("[%1$s] %2$s", i, t1));
			}
		}

	}

}
