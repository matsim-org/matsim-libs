/* *********************************************************************** *
 * project: org.matsim.*
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

/**
 *
 */
package org.matsim.contrib.noise;

import org.matsim.core.gbl.Gbl;


/**
 *
 * Contains general equations that are relevant to compute noise emission and immission levels, based on the German RLS-90 approach 'Lange gerade Straßen'.
 *
 * @author lkroeger, ikaddoura
 *
 */
final class NoiseEquations {

	private NoiseEquations() {};

	static double calculateShareOfResultingNoiseImmission(double noiseImmission, double resultingNoiseImmission){
		double shareOfResultingNoiseImmission = Math.pow(((Math.pow(10, (0.05 * noiseImmission))) / (Math.pow(10, (0.05 * resultingNoiseImmission)))), 2);
		return shareOfResultingNoiseImmission;
	}

	static double[] calculateShare(int[] counts, double[] levels) {
		Gbl.assertNotNull(counts);
		Gbl.assertNotNull(levels);
		if(counts.length != levels.length) {
			throw new IllegalArgumentException("Numbers of counts and levels must match!");
		}

		double sum = 0;
		double shares[] = new double[counts.length];
		for(int i = 0; i< counts.length; i++) {
			final double value = counts[i] * Math.pow(10, 0.1 * levels[i]);
			sum += value;
			shares[i] = value;
		}
		for(int i = 0; i< counts.length; i++) {
			shares[i] = shares[i] / sum;
		}
		return shares;
	}

	static double calculateResultingNoiseImmissionPlusOneVehicle(double finalImmission, double immissionIsolatedLink, double immissionIsolatedLinkPlusOneVehicle) {
		double noiseImmissionPlusOneVehicle;
		if (finalImmission != 0.) {
			if (immissionIsolatedLink == 0.) {
				noiseImmissionPlusOneVehicle = 10 * Math.log10( Math.pow(10, (0.1 * immissionIsolatedLinkPlusOneVehicle)) + Math.pow(10, (0.1 * finalImmission)) );
			} else {
				noiseImmissionPlusOneVehicle = 10 * Math.log10( Math.pow(10, (0.1 * immissionIsolatedLinkPlusOneVehicle)) - Math.pow(10, (0.1 * immissionIsolatedLink)) + Math.pow(10, (0.1 * finalImmission)) );
			}
		} else {
			noiseImmissionPlusOneVehicle = immissionIsolatedLinkPlusOneVehicle;
		}
		return noiseImmissionPlusOneVehicle;
	}
}
