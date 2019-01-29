/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package org.matsim.contrib.ev;

public class EvUnits {
	public static final double W_PER_kW = 1_000;// W per kW
	public static final double J_PER_kWh = 3_600_000;// J per kWh
	public static final double J_m_PER_Wh_km = 3.6;// J/m per Wh/km
	public static final double J_m_PER_kWh_100km = 36;// J/m per kWh/100km

	public static double kWh_to_J(double value) {
		return value * J_PER_kWh;
	}

	public static double J_to_kWh(double value) {
		return value / J_PER_kWh;
	}

	public static double kW_to_W(double value) {
		return value * W_PER_kW;
	}

	public static double W_to_kW(double value) {
		return value / W_PER_kW;
	}

	public static double Wh_km_to_J_m(double value) {
		return value * J_m_PER_Wh_km;
	}

	public static double J_m_to_Wh_km(double value) {
		return value / J_m_PER_Wh_km;
	}

	public static double kWh_100km_to_J_m(double value) {
		return value * J_m_PER_kWh_100km;
	}

	public static double J_m_to_kWh_100km(double value) {
		return value / J_m_PER_kWh_100km;
	}
}
