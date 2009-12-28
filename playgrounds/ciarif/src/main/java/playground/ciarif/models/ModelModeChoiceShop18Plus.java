/* *********************************************************************** *
 * project: org.matsim.*
 * ModelModeChoiceShop18Plus.java
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

public class ModelModeChoiceShop18Plus extends ModelModeChoice {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	static final double B1_CONST = +2.8347054e+000;		
	static final double B1_DIST_TOUR = -2.3377491e-001;
	static final double B2_CONST = +1.1253337e+000;
	static final double B2_DIST_TOUR = -1.547889e-001;		
	static final double B3_CONST = -1.6728674e-001;
	static final double B3_CAR_ALWAYS = +2.3961644e+000;
	static final double B3_DIST_TOUR = -1.8733613e-003;
	static final double B3_T2 = +4.3815363e-001;
	static final double B3_T3 = +6.14612904e-001;
	static final double B3_T4 = +9.3565597e-001;		
	static final double B3_T5 = +8.6805574e-001;
	static final double B4_AGE = +1.0058544e-004;
	static final double B4_Car_Never = +4.5670870e-001;
	static final double B4_DIST_TOUR = -1.9791424e-003;
	static final double B4_SEASON = +1.5016966e+000;
	static final double B4_T2 = -9.8770902e-001;
	static final double B4_T3 = -9.1599407e-001;
	static final double B4_T4 = -8.9163726e-001;
	static final double B4_T5 = -1.8205975e+000;
	static final double B5_70 = +8.3096335e-001;
	static final double B5_CONST = -4.1213902e-001;
	static final double B5_MALE = -1.3400346e+000;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public ModelModeChoiceShop18Plus() {
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
		return util;
	}

	@Override
	protected final double calcBikeUtil() {
		if (!bike) { return Double.NEGATIVE_INFINITY; }
		double util = 0.0;
		util += B2_CONST * 1.0;
		util += B2_DIST_TOUR * dist_tour;
		return util;
	}
	
	@Override
	protected final double calcCarUtil() {
		double util = 0.0;
		util += B3_CONST * 1.0;
		util += B3_DIST_TOUR * dist_tour;
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
		util += B5_MALE * 1.0;
		if (age >= 70 ) { util += B5_70 * 1.0; }
		return util;
	}
}


