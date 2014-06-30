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
package noiseInternalization.version01;

//Der Beurteilungspegel L_r ist bei Straßenverkehrsgeräuschen dem Mittelungspegel L_m gleich.
//L_r = L_m = 10 * lg(( 1 / T_r ) * (Integral)_T_r(10^(0,1*1(t))dt))
//
//L_m,e ist der Mittelungspegel im Abstand von 25m von der Achse der Schallausbreitung

public class NoiseEmissionCalculator {
	
public static double calculateEmissionspegel(int M , double p , double vCar , double vLorry) {
		
		// M ... Verkehrsstärke
		// p ... Lkw-Anteil
		
		double Emissionspegel = 0.0;
		
		double Mittelungspegel = 37.3 + 10* Math.log10(M * (1 + (0.082 * p)));
		
		// TODO: Hier müssten evtl. weitere Korrekturfaktoren eingehen, so zum Abstand der Bebauung und zur Reflexion
		Emissionspegel = Mittelungspegel + calculateGeschwindigkeitskorrekturDv(vCar, vLorry, p);
		
		return Emissionspegel;
		
	}
	
	public static double calculateGeschwindigkeitskorrekturDv (double vCar , double vLorry , double p) {
		// von 100 km/h abweichende Geschwindigkeiten werden berücksichtigt

		// p ... Lkw-Anteil
		
		double GeschwindigkeitskorrekturDv = 0.0;
		
		double LCar = 27.7 + (10 * Math.log10(1.0 + Math.pow(0.02 * vCar, 3.0)));
		
		double LLorry = 23.1 + (12.5 * Math.log10(vLorry));
		
		double D = LCar - LLorry; 
		
		GeschwindigkeitskorrekturDv = LCar - 37.3 + 10* Math.log10((100.0 + (Math.pow(10.0, (0.1 * D)) - 1) * p ) / (100 + 8.23 * p));
		
		return GeschwindigkeitskorrekturDv;
		
	}

}
