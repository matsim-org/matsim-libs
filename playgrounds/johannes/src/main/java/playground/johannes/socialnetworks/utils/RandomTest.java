/* *********************************************************************** *
 * project: org.matsim.*
 * RandomTest.java
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
package playground.johannes.socialnetworks.utils;

import java.util.Random;

import org.apache.commons.math.random.BitsStreamGenerator;
import org.apache.commons.math.random.RandomGenerator;
import org.apache.commons.math.random.Well512a;

/**
 * @author illenberger
 *
 */
public class RandomTest {

	public static void main(String args[]) {
		int samples = 100000000;
	
		Random jdkRandom = new Random(4711);
		long time = System.currentTimeMillis();
		for(int i = 0; i < samples; i++) {
			jdkRandom.nextDouble();
		}
		time = System.currentTimeMillis() - time;
		System.out.println(String.format("JDK generator took %1$s msecs.", time));
		
		Random well = new XORShiftRandom(4711);
		well.setSeed(4711);
		time = System.currentTimeMillis();
		for(int i = 0; i < samples; i++) {
				well.nextDouble();
		}
		time = System.currentTimeMillis() - time;
		System.out.println(String.format("XORShift generator took %1$s msecs.", time));
	}
}
