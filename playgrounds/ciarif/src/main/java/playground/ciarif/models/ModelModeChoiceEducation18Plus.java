/* *********************************************************************** *
 * project: org.matsim.*
 * ModelModeChoiceEducation18Plus.java
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

public class ModelModeChoiceEducation18Plus extends ModelModeChoice {
	
//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	static final double B1_CONST = +3.6365697e+000;		
	static final double B1_DIST_TOUR = -1.0017446e+000;
	static final double B2_CONST = +2.4500648e+000;
	static final double B2_DIST_TOUR = -3.6108609e-001;
	static final double B3_CONST = -7.5207522e-001;
	static final double B3_CAR_ALWAYS = +2.91847952e+000;
	static final double B3_DIST_TOUR = -6.6341053e-003;
	static final double B3_T2 = -1.0267447e+000;
	static final double B3_T3 = -1.2836031e+000;
	static final double B3_T4 = -8.3509503e-001;		
	static final double B3_T5 = -2.0478721e-001;
	static final double B4_AGE = -4.3255177e-004;
	static final double B4_SEASON = +2.1225061e+000;
	static final double B5_AGE = -5.6501291e-003;
	static final double B5_CONST = +2.3929285e+000;
	static final double B5_DIST_TOUR = -1.4080310e-001;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public ModelModeChoiceEducation18Plus() {
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
		return util;
	}

	@Override
	protected final double calcCarRideUtil() {
		double util = 0.0;
		util += B5_CONST * 1.0;
		util += B5_DIST_TOUR * dist_tour;
		util += B5_AGE * (age * age);
		return util;
	}

}
