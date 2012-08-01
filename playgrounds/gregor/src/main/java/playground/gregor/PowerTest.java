/* *********************************************************************** *
 * project: org.matsim.*
 * PowerTest.java
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

package playground.gregor;

import org.matsim.core.gbl.MatsimRandom;

public class PowerTest {

	private final static int SIZE = 100000;

	public static void main (String...args) {
		long than = System.nanoTime();
		for (int j = 0; j < SIZE/100; j ++) {
			double [] rnds = new double[SIZE];
			double [] res = new double[SIZE];
			//fill array
			for (int i = 0; i < SIZE; i++) {
				//rnd num betw. -100 & 100
				double rand = MatsimRandom.getRandom().nextDouble();
				rnds[i] = (rand-0.5) * 200;
			}


			for (int i = 0; i < SIZE; i++) {
//				res[i] = Math.pow(rnds[i], 2);
				res[i] = rnds[i] * rnds[i];
			}


		}
		long now = System.nanoTime();
		System.out.println(now - than);
		
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		than = System.nanoTime();
		for (int j = 0; j < SIZE/100; j ++) {
			float [] rnds = new float[SIZE];
			float [] res = new float[SIZE];
			//fill array
			for (int i = 0; i < SIZE; i++) {
				//rnd num betw. -100 & 100
				float rand = MatsimRandom.getRandom().nextFloat();
				rnds[i] = (rand-0.5f) * 200f;
			}


			for (int i = 0; i < SIZE; i++) {
//				res[i] = Math.pow(rnds[i], 2);
				res[i] = rnds[i] * rnds[i];
			}


		}
		now = System.nanoTime();
		System.out.println(now - than);
	}
}
