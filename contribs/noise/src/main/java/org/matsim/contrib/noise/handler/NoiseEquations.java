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
package org.matsim.contrib.noise.handler;

import java.util.Collection;


/**
 *
 * Contains general equations that are relevant to compute noise emission and immission levels, based on the German RLS-90 approach 'Lange gerade Straßen'.
 *
 * @author lkroeger, ikaddoura
 *
 */
public final class NoiseEquations {

	private NoiseEquations() {};

	private enum DayTime {NIGHT, DAY, EVENING}

	public static double calculateMittelungspegelLm(int n, double p) {

		//	Der Beurteilungspegel L_r ist bei Straßenverkehrsgeraeuschen gleich dem Mittelungspegel L_m.
		//  L_r = L_m = 10 * lg(( 1 / T_r ) * (Integral)_T_r(10^(0,1*1(t))dt))
		//	L_m,e ist der Mittelungspegel im Abstand von 25m von der Achse der Schallausbreitung

		// 	M ... traffic volume
		// 	p ... share of hdv in %

		if (p > 1) {
			throw new RuntimeException("p has to be <= 1. For an HGV share of 1%, p should be 0.01. Aborting...");
		}

		double pInPercentagePoints = p * 100.;
		double mittelungspegel = 37.3 + 10* Math.log10(n * (1 + (0.082 * pInPercentagePoints)));

		return mittelungspegel;
	}

	public static double calculateGeschwindigkeitskorrekturDv (double vCar , double vHdv , double p) {

		//  v ... speed in kilometers per hour
		// 	p ... share of hdv, in percentage points

		if (p > 1) {
			throw new RuntimeException("p has to be <= 1. For an HGV share of 1%, p should be 0.01. Aborting...");
		}

		double pInPercentagePoints = p * 100.;

		double lCar = calculateLCar(vCar);
		double lHdv = calculateLHdv(vHdv);

		double d = lHdv - lCar;
		double geschwindigkeitskorrekturDv = lCar - 37.3 + 10* Math.log10( (100.0 + (Math.pow(10.0, (0.1 * d)) - 1) * pInPercentagePoints ) / (100 + 8.23 * pInPercentagePoints));

		return geschwindigkeitskorrekturDv;
	}

	/**
	 * Pegeländerung D_B durch bauliche Maßnahmen (und topografische Gegebenheiten)
	 * @param s distance between emission source and immission receiver point
	 * @param a distance between emission source and (first) edge of diffraction
	 * @param b distance between (last) edge of diffraction and immission receiver point
	 * @param c sum of distances between edges of diffraction
	 * @return shielding correction term for the given parameters
	 */
	public static double calculateShieldingCorrection(double s, double a, double b, double c) {

	    //Shielding value (Schirmwert) z: the difference between length of the way from the emission source via the
        // obstacle to the immission receiver point and direct distance between emission source and immission
        // receiver point ~ i.e. the additional length that sound has to propagate because of shielding
		double z = a + b + c - s;

		//Weather correction for diffracted rays
		double k = Math.exp(- 1. / 2000. * Math.sqrt( (a*b*s) / (2 * z)) );

		double temp = 5 + ((70 + 0.25 * s) / (1+ 0.2 * z)) * z * Math.pow(k,2);
		double correction = 7 * Math.log10(temp);
		return correction;
	}

	public static double calculateResultingNoiseImmission (Collection<Double> collection){

		double resultingNoiseImmission = 0.;

		if (collection.size() > 0) {
			double sumTmp = 0.;
			for (double noiseImmission : collection) {
				if (noiseImmission > 0.) {
					sumTmp = sumTmp + (Math.pow(10, (0.1 * noiseImmission)));
				}
			}
			resultingNoiseImmission = 10 * Math.log10(sumTmp);
			if (resultingNoiseImmission < 0) {
				resultingNoiseImmission = 0.;
			}
		}
		return resultingNoiseImmission;
	}

	public static double calculateLCar(double vCar) {

		double lCar = 27.7 + (10 * Math.log10(1.0 + Math.pow(0.02 * vCar, 3.0)));
		return lCar;
	}

	public static double calculateLHdv(double vHdv) {

		double lHdv = 23.1 + (12.5 * Math.log10(vHdv));
		return lHdv;
	}

	public static double calculateDistanceCorrection(double distance) {
		double correctionTermDs = 15.8 - (10 * Math.log10(distance)) - (0.0142 * (Math.pow(distance, 0.9)));
		return correctionTermDs;
	}

	public static double calculateAngleCorrection(double angle) {
		double angleCorrection = 10 * Math.log10((angle) / (180));
		return angleCorrection;
	}

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
