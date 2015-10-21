/*
 * Copyright 2015 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.floetteroed@abe.kth.se
 *
 */
package floetteroed.utilities;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class Units {

	// --------------------PRIVATE CONSTRUCTION --------------------

	private Units() {
	}

	// -------------------- CONVERSION OF TIME UNITS --------------------

	public static final double S_PER_H = 3600.0;

	public static final double H_PER_S = 1.0 / S_PER_H;

	public static final double H_PER_D = 24.0;

	public static final double D_PER_H = 1.0 / H_PER_D;

	public static final double S_PER_D = S_PER_H * H_PER_D;

	public static final double D_PER_S = 1 / S_PER_D;

	// TODO NEW
	public static final double S_PER_MIN = 60.0;

	// TODO NEW
	public static final double MIN_PER_S = 1.0 / S_PER_MIN;

	// TODO NEW
	public static final double MIN_PER_H = 60.0;

	// TODO NEW
	public static final double H_PER_MIN = 1.0 / MIN_PER_H;

	// -------------------- CONVERSION OF LENGTH UNITS --------------------

	public static final double M_PER_KM = 1000.0;

	public static final double KM_PER_M = 1 / M_PER_KM;

	// -------------------- CONVERSION OF FLOW UNITS --------------------

	public static final double VEH_H_PER_VEH_S = 1.0 / H_PER_S;

	public static final double VEH_S_PER_VEH_H = 1.0 / S_PER_H;

	// -------------------- CONVERSION OF DENSITY UNITS --------------------

	public static final double VEH_KM_PER_VEH_M = 1.0 / KM_PER_M;

	public static final double VEH_M_PER_VEH_KM = 1.0 / M_PER_KM;

	// -------------------- CONVERSION OF VELOCITY UNITS --------------------

	public static final double M_S_PER_KM_H = M_PER_KM / S_PER_H;

	public static final double KM_H_PER_M_S = KM_PER_M / H_PER_S;

}
