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

import java.util.Collection;


/**
 *
 * Contains general equations that are relevant to compute noise emission and immission levels, based on the German RLS-90 approach 'Lange gerade Straßen'.
 *
 * @author lkroeger, ikaddoura
 *
 */
final class NoiseEquations {

	private NoiseEquations() {};

	private enum DayTime {NIGHT, DAY, EVENING}


    public static double calculateDamageCosts(double noiseImmission, double affectedAgentUnits, double timeInterval, double annualCostRate, double timeBinSize) {

		DayTime daytimeType = DayTime.NIGHT;

		if (timeInterval > 6 * 3600 && timeInterval <= 18 * 3600) {
			daytimeType = DayTime.DAY;
		} else if (timeInterval > 18 * 3600 && timeInterval <= 22 * 3600) {
			daytimeType = DayTime.EVENING;
		}

		double lautheitsgewicht = 0;

		switch (daytimeType) {
			case DAY:
				if (noiseImmission >= 50) {
					lautheitsgewicht = Math.pow(2.0 , 0.1 * (noiseImmission - 50));
				}
				break;
			case EVENING:
				if (noiseImmission >= 45) {
					lautheitsgewicht = Math.pow(2.0 , 0.1 * (noiseImmission - 45));
				}
				break;
			case NIGHT:
				if (noiseImmission >= 40) {
					lautheitsgewicht = Math.pow(2.0 , 0.1 * (noiseImmission - 40));
				}
				break;
			default:
		}


		double laermEinwohnerGleichwert = lautheitsgewicht * affectedAgentUnits;
		double damageCosts = ( annualCostRate * laermEinwohnerGleichwert / 365. ) * ( timeBinSize / (24.0 * 3600) );

		return damageCosts;
	}

	public static double calculateShareOfResultingNoiseImmission (double noiseImmission , double resultingNoiseImmission){
		double shareOfResultingNoiseImmission = Math.pow(((Math.pow(10, (0.05 * noiseImmission))) / (Math.pow(10, (0.05 * resultingNoiseImmission)))), 2);
		return shareOfResultingNoiseImmission;
	}

	public static double calculateShare(int nVehicleType1, double lVehicleType1, int nVehicleType2, double lVehicleType2) {
		double share = ((nVehicleType1 * Math.pow(10, 0.1 * lVehicleType1)) / ((nVehicleType1 * Math.pow(10, 0.1 * lVehicleType1)) + (nVehicleType2 * Math.pow(10, 0.1 * lVehicleType2))));
		return share;
	}

	public static double calculateResultingNoiseImmissionPlusOneVehicle(double finalImmission, double immissionIsolatedLink, double immissionIsolatedLinkPlusOneVehicle) {
		double noiseImmissionPlusOneVehicle = Double.NEGATIVE_INFINITY;
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
