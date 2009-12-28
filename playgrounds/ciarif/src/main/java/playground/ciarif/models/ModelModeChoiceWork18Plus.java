/* *********************************************************************** *
 * project: org.matsim.*
 * ModelModeChoiceWork18Plus.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.ciarif.models;

import org.matsim.core.gbl.Gbl;


public class ModelModeChoiceWork18Plus extends ModelModeChoice {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	static final double B1_CONST = +2.4788750e+000;		
	static final double B1_DIST_TOUR = -4.0651449e-001;
	static final double B1_DIST_W_E = -1.7330547e-002;		
	static final double B2_CONST = +1.1186166e+000;
	static final double B2_DIST_TOUR = -1.3743949e+000;		
	static final double B2_DIST_W_E = -6.5495332e-003;
	static final double B3_CONST = -3.4562753e-001;
	static final double B3_CAR_ALWAYS = +2.6192062e+000;
	static final double B3_DIST_TOUR = -6.6341053e-004;
	static final double B3_DIST_W_E = -1.4849077e-002;
	static final double B3_T2 = +3.9603572e-001;
	static final double B3_T3 = +4.5153284e-001;
	static final double B3_T4 = +5.9383257e-001;		
	static final double B3_T5 = +6.0151130e-001;
	static final double B4_AGE = -1.0564284e-004;
	static final double B4_SEASON = +2.3213232e+000;
	static final double B4_T2 = -4.7650791e-001;
	static final double B4_T3 = -2.2084505e-001;
	static final double B4_T4 = -5.3704029e-001;
	static final double B4_T5 = -1.1589370e+000;
	static final double B5_18_30 = +8.9705007e-001;
	static final double B5_70 = +1.7279727e+000;
	static final double B5_CONST = -1.4863033e+000;
	static final double B5_DIST_TOUR = -1.4060959e-003;
	static final double B5_DIST_W_E = -5.7056861e-002;
	static final double B5_MALE = -6.2407099e-001;
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public ModelModeChoiceWork18Plus() {
		super ();
	}

	//////////////////////////////////////////////////////////////////////
	// calc methods
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////

	@Override
	protected final double calcWalkUtil() {
		// Computes for this person the utility of choosing walk as transportation mode 
		// when the tour (plan) has work as main purpose
		// if (one)
		// B_Const_w* 1 + B_Dist_w * T_DIST
		double util = 0.0;
		util += B1_CONST * 1.0;
		util += B1_DIST_TOUR * dist_tour;
		util += B1_DIST_W_E * dist_w_e;
		return util;
	}

	@Override
	protected final double calcBikeUtil() {
		if (!bike) { return Double.NEGATIVE_INFINITY; }
		double util = 0.0;
		util += B2_CONST * 1.0;
		util += B2_DIST_TOUR * dist_tour;
		util += B2_DIST_W_E * dist_w_e;
		return util;
	}
	
	@Override
	protected final double calcCarUtil() {
		double util = 0.0;
		util += B3_CONST * 1.0;
		util += B3_DIST_TOUR * dist_tour;
		util += B3_DIST_W_E * dist_w_e;
		if (car == "always") { util += B3_CAR_ALWAYS * 1.0; }
		if (udeg == 1) { /* reference type */ }
		else if (udeg == 2) { util += B3_T2 * 1.0; }
		else if (udeg == 3) { util += B3_T3 * 1.0; }
		else if (udeg == 4) { util += B3_T4 * 1.0; }
		else if (udeg == 5) { util += B3_T5 * 1.0; }
		else { Gbl.errorMsg("This should never happen!"); }
		return util;
	}
	 
	@Override
	protected final double calcPublicUtil() {
		double util = 0.0;
		util += B4_SEASON * tickets ;
		util += B4_AGE * (age * age);
		if (udeg == 1) { /* reference type */ }
		else if (udeg == 2) { util += B4_T2 * 1.0; }
		else if (udeg == 3) { util += B4_T3 * 1.0; }
		else if (udeg == 4) { util += B4_T4 * 1.0; }
		else if (udeg == 5) { util += B4_T5 * 1.0; }
		else { Gbl.errorMsg("This should never happen!"); }
		return util;
	}

	@Override
	protected final double calcCarRideUtil() {
		double util = 0.0;
		util += B5_CONST * 1.0;
		util += B5_DIST_TOUR * dist_tour;
		util += B5_DIST_W_E * dist_w_e;
		util += B5_MALE * 1.0;
		if (age <= 30 & age > 18) { util += B5_18_30 * 1.0; }
		if (age >= 70 ) { util += B5_70 * 1.0; }
		return util;
	}
}

