/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.michalm.ev.discharging;

import org.matsim.api.core.v01.network.Link;

/**
 * Parametrised for the Nissan Leaf. All values in SI units.
 * <p>
 * </p>
 * See: </br>
 * Ohde, B., Ślaski, G., & Maciejewski, M. (2016). Statistical analysis of real-world urban driving cycles for modelling
 * energy consumption of electric vehicles. Journal of Mechanical and Transport Engineering, 68.
 * 
 * http://fwmt.put.poznan.pl/imgWYSIWYG/agill/File/2_68_2016/jmte_2016_68_2_03_ohde_b_slaski_g.pdf
 * <p>
 * </p>
 * TODO Add (dis-)charging efficiency relative to SOC, temperature, etc...
 */
public class OhdeSlaskiDriveEnergyConsumption implements DriveEnergyConsumption {
	private static final double g = 9.81; // g [m/s^2]
	private static final double m = 1525; // vehicle mass [kg]
	private static final double m_s = m + 100; // vehicle mass + extra mass [kg]
	private static final double spr = .935; // drive train efficiency [-]
	private static final double ft = .01; // rolling drag coefficient[-]
	private static final double fa = 0.4193; // aerodynamic drag coefficient [kg/m]
	private static final double cb = .191; // inertia resistance coefficient [-]

	// acceleration approximation in: a1 * ln(v / 1 [m/s]) + a2
	private static final double a1 = -0.267;// [m/s^2]
	private static final double a2 = 0.99819;// [m/s^2]

	// precomputed values
	private static final int maxAvgSpeed = 40;
	private static final int speedStepsPerUnit = 10;
	private static final double zeroSpeed = 0.01;
	private static final double[] power;

	static {
		power = new double[maxAvgSpeed * speedStepsPerUnit];
		power[0] = calcPower(zeroSpeed);
		for (int i = 1; i < power.length; i++) {
			power[i] = calcPower((double)i / speedStepsPerUnit);
			// System.out.println(((double)i / speedStepsPerUnit) + " -> " + power [i]);
		}
	}

	// v - avg speed [m/s]
	// power - avg power [W]
	private static double calcPower(double v) {
		return v * (ft * m_s * g + fa * v * v + cb * (a1 * Math.log(v) + a2) * m_s) / spr;
	}

	@Override
	public double calcEnergyConsumption(Link link, double travelTime) {
		if (travelTime == 0) {
			return 0;
		}

		double avgSpeed = link.getLength() / travelTime;
		int idx = (int)Math.round(avgSpeed * speedStepsPerUnit);
		return power[idx] * travelTime;
	}
}
