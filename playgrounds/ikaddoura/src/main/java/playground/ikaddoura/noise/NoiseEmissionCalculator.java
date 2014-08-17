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
package playground.ikaddoura.noise;

//import org.apache.log4j.Logger;

//Der Beurteilungspegel L_r ist bei Straßenverkehrsgeraeuschen gleich dem Mittelungspegel L_m.
//L_r = L_m = 10 * lg(( 1 / T_r ) * (Integral)_T_r(10^(0,1*1(t))dt))
//
//L_m,e ist der Mittelungspegel im Abstand von 25m von der Achse der Schallausbreitung

/**
 * @author lkroeger
 *
 */

public class NoiseEmissionCalculator {
	
//	private static final Logger log = Logger.getLogger(NoiseEmissionCalculator.class);	
	
	public static double calculateEmissionspegel(int M , double p , double vCar , double vHdv) {
		
		// M ... traffic volume
		// p ... share of hdv
		
		// p in percentage points
		double pInPercentagePoints = p*100.;
	
//		log.info(pInPercentagePoints);
		
		double Emissionspegel = 0.0;
		
		double Mittelungspegel = 37.3 + 10* Math.log10(M * (1 + (0.082 * pInPercentagePoints)));
		
		Emissionspegel = Mittelungspegel + calculateGeschwindigkeitskorrekturDv(vCar, vHdv, pInPercentagePoints);
		// The other correction factors are calculated for the immission later on.
		
		return Emissionspegel;
		
	}
	
	public static double calculateGeschwindigkeitskorrekturDv (double vCar , double vHdv , double pInPercentagePoints) {
		// basically the speed is 100 km/h
		// p ... share of hdv, in percentage points (see above)
		
		double GeschwindigkeitskorrekturDv = 0.0;
		
		double LCar = 27.7 + (10.0 * Math.log10(1.0 + Math.pow(0.02 * vCar, 3.0)));
		
		double LHdv = 23.1 + (12.5 * Math.log10(vHdv));
		
		double D = LHdv - LCar; 
		
		GeschwindigkeitskorrekturDv = LCar - 37.3 + 10* Math.log10((100.0 + (Math.pow(10.0, (0.1 * D)) - 1) * pInPercentagePoints ) / (100 + 8.23 * pInPercentagePoints));
		
		return GeschwindigkeitskorrekturDv;
		
	}

}
