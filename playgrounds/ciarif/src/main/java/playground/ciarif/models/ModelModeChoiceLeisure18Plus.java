/* *********************************************************************** *
 * project: org.matsim.*
 * ModelModeChoiceLeisure18Plus.java
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

public class ModelModeChoiceLeisure18Plus  extends ModelModeChoice {
	
//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	static final double B1_CONST = +4.3948182e+000;		
	static final double B1_DIST_TOUR = -8.0750733e-001;
	static final double B2_CONST = +1.2650584e+000;
	static final double B2_DIST_TOUR = -1.5385983e-001;		
	static final double B3_CONST = +5.1757033e-001;
	static final double B3_CAR_ALWAYS = +2.0663115e+000;
	static final double B3_DIST_TOUR = -2.3085511e-003;
	static final double B3_T2 = +3.2437481e-001;
	static final double B3_T3 = +1.5080347e-001;
	static final double B3_T4 = +3.2894202e-001;		
	static final double B3_T5 = +4.4949286e-001;
	static final double B4_AGE = +1.2270216e-004;
	static final double B4_CAR_NEVER = +7.2009050e-001;
	static final double B4_SEASON = +1.4862080e+000;
	static final double B4_T2 = -8.3248311e-001;
	static final double B4_T3 = -6.4843826e-001;
	static final double B4_T4 = -1.2727468e+000;
	static final double B4_T5 = -1.4736010e+000;
	static final double B5_18_30 = +5.0732109e-001;
	static final double B5_70 = +1.4776279e+000;
	static final double B5_CONST = -6.5312469e-001;
	static final double B5_DIST_TOUR = -2.9312571e-003;
	static final double B5_MALE = -1.0336419e+000;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public ModelModeChoiceLeisure18Plus() {
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
		if (car == "never") {util += B4_CAR_NEVER * 1.0;}
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
		util += B5_MALE * 1.0;
		if (age <= 30 ) { util += B5_18_30 * 1.0; }
		if (age >= 70 ) { util += B5_70 * 1.0; }
		return util;
	}

}
