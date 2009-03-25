/* *********************************************************************** *
 * project: org.matsim.*
 * ModelModeChoiceOther18Minus.java
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

public class ModelModeChoiceOther18Minus extends ModelModeChoice {
	
	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	static final double B1_CONST = +3.1101510e+000;		
	static final double B1_T2 = +2.8586344e-001;
	static final double B1_T3 = -1.8126991e-001;
	static final double B1_T4 = -1.7790913e-001;		
	static final double B1_T5 = -2.4615782e-001;		
	static final double B2_CONST = +2.1548612e+000;
	static final double B2_T2 = +5.2616644e-002;
	static final double B2_T3 = +8.1434652e-003;
	static final double B2_T4 = -8.2933460e-001;		
	static final double B2_T5 = -7.1255186e-003;
	static final double B4_AGE = +7.6872568e-003;
	static final double B4_SEASON = +8.8220087e-001;
	static final double B5_6_12 = +5.7192709e-001;
	static final double B5_CONST = +2.4527356e+000;
	

	/////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public ModelModeChoiceOther18Minus() {
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
		if (udeg == 1) { /* reference type */ }
		else if (udeg == 2) { util += B1_T2 * 1.0; }
		else if (udeg == 3) { util += B1_T3 * 1.0; }
		else if (udeg == 4) { util += B1_T4 * 1.0; }
		else if (udeg == 5) { util += B1_T5 * 1.0; }
		else { Gbl.errorMsg("This should never happen!"); }
		return util;
	}

	@Override
	protected final double calcBikeUtil() {
		if (!bike) { return Double.NEGATIVE_INFINITY; }
		double util = 0.0;
		util += B2_CONST * 1.0;
		if (udeg == 1) { /* reference type */ }
		else if (udeg == 2) { util += B2_T2 * 1.0; }
		else if (udeg == 3) { util += B2_T3 * 1.0; }
		else if (udeg == 4) { util += B2_T4 * 1.0; }
		else if (udeg == 5) { util += B2_T5 * 1.0; }
		else { Gbl.errorMsg("This should never happen!"); }
		return util;
	}
	
	@Override
	protected final double calcCarUtil () {
		double util = Double.NEGATIVE_INFINITY;
		return util;
	}
	
	@Override
	protected final double calcPublicUtil() {
		double util = 0.0;
		util += B4_SEASON * tickets ;
		util += B4_AGE * (age * age);
		return util;
	}

	@Override
	protected final double calcCarRideUtil() {
		double util = 0.0;
		util += B5_CONST * 1.0;
		if (age <= 12 & age >= 6 ) { util += B5_6_12 * 1.0; }
		return util;
	}
}
