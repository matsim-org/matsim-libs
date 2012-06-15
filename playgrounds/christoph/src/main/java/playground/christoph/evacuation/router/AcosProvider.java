/* *********************************************************************** *
 * project: org.matsim.*
 * AcosProvider.java
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

package playground.christoph.evacuation.router;

import java.util.Random;

import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;

public class AcosProvider {

	/*
	 * Using a lookup table instead of using Math.acos is approximately 
	 * ten times faster. 
	 */
	private static boolean useLookup = true;
	
	private final int acosResoluation = 1000000;
	private final double[] acosArray = new double[acosResoluation + 1]; 
	
	public AcosProvider() {
		if (useLookup) {
			for (int i = 0; i < acosResoluation; i++) {
				acosArray[i] = Math.acos(Double.valueOf(i) / acosResoluation);
			}
			acosArray[acosArray.length - 1] = 0.0;			
		}
	}
	
	public double getAcos(double d) {
		if (useLookup) {
			double abs = Math.abs(d);
			
			int arrayPosition = (int) Math.round(abs * acosResoluation);
			
			if (d < 0.0) return Math.PI - acosArray[arrayPosition];
			else return acosArray[arrayPosition];
		} else return Math.acos(d);
	}
	
	public static void main(String[] args) {
		double[] values = new double[1000000];
		
		Random random = MatsimRandom.getLocalInstance();
		for (int i = 0; i < values.length; i++) values[i] = 2 * random.nextDouble() - 1;
		
		Gbl.startMeasurement();
		for (double d : values) Math.acos(d);
		Gbl.printElapsedTime();
		
		AcosProvider acosProvider = new AcosProvider();
		Gbl.startMeasurement();
		for (double d : values) acosProvider.getAcos(d);
		Gbl.printElapsedTime();
		
		for (double d : values) {
			double v1 = Math.acos(d);
			double v2 = acosProvider.getAcos(d);
			double error = Math.abs(v1 - v2)/(2 * Math.PI);
			if (error > 0.0001) {
				System.out.println("Error > 0.01% of range (-PI .. PI): " + error);
			}
		}
	}
}
