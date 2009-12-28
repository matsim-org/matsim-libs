/* *********************************************************************** *
 * project: org.matsim.*
 * ModelModeChoiceShopLeisure.java
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

package playground.balmermi.census2000.models;

import org.matsim.core.gbl.Gbl;

public class ModelModeChoiceShopLeisure extends ModelModeChoice {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	static final double B_Dist_w_Shop = -4.1026300e-001;		
	static final double B_Const_w_Shop = +3.4582248e+000;
	static final double B_Dist_b_Shop = -1.22347514e-001;		
	static final double B_Const_b_Shop = +1.1708104e+000;
	static final double B_Dist_c_Shop = +1.7435190e-002;		
	static final double B_Const_c_Shop= +7.7357251e-001;
	static final double B_Lic_c_Shop = +3.0736545e-001;
	static final double B_T2_c_Shop = +3.2010590e-001;
	static final double B_T3_c_Shop = +3.0765084e-001;
	static final double B_T4_c_Shop = +4.0390676e-001;
	static final double B_T5_c_Shop = +3.6820671e-001;
	static final double B_Car_always_Shop = +1.0021152e+000;
	static final double B_HH_Dim_Shop = +1.1019932e-001;
	static final double B_Age_sq_Shop = +5.5715851e-005;		
	static final double B_Season_pt_Shop = +1.3486953e+000;
	static final double B_Dist_pt_Shop = +1.8348214e-002;
	static final double B_T2_pt_Shop = -7.6207183e-001;
	static final double B_T3_pt_Shop = -6.4683092e-001;
	static final double B_T4_pt_Shop = -9.4348474e-001;
	static final double B_T5_pt_Shop = -1.5534112e+000;
	static final double B_pt_car_never_Shop = +8.0390793e-001;
	static final double B_Const_ot_Shop = -7.6196412e-001;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public ModelModeChoiceShopLeisure() {
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
		util += B_Const_w_Shop * 1.0;
		util += B_Dist_w_Shop * dist;
		return util;
	}

	@Override
	protected final double calcBikeUtil() {
		if (!bike) { return Double.NEGATIVE_INFINITY; }
		double util = 0.0;
		util += B_Const_b_Shop * 1.0;
		util += B_Dist_b_Shop * dist;
		return util;
	}

	@Override
	protected final double calcCarUtil() {
		double util = 0.0;
		util += B_Const_c_Shop * 1.0;
		util += B_Dist_c_Shop * dist;
		util += B_Lic_c_Shop * license;
		if (car == "always") { util += B_Car_always_Shop * 1.0; }
		if (udeg == 1) { /* reference type */ }
		else if (udeg == 2) { util += B_T2_c_Shop * 1.0; }
		else if (udeg == 3) { util += B_T3_c_Shop * 1.0; }
		else if (udeg == 4) { util += B_T4_c_Shop * 1.0; }
		else if (udeg == 5) { util += B_T5_c_Shop * 1.0; }
		else { Gbl.errorMsg("This should never happen!"); }
		return util;
	}

	@Override
	protected final double calcPublicUtil() {
		double util = 0.0;
		util += B_Season_pt_Shop * tickets ;
		util += B_Age_sq_Shop * (age * age);
		util += B_Dist_pt_Shop * dist;
		if (car == "never"){ util += B_pt_car_never_Shop * 1.0; }
		if (udeg == 1) { /* reference type */ }
		else if (udeg == 2) { util += B_T2_pt_Shop * 1.0; }
		else if (udeg == 3) { util += B_T3_pt_Shop * 1.0; }
		else if (udeg == 4) { util += B_T4_pt_Shop * 1.0; }
		else if (udeg == 5) { util += B_T5_pt_Shop * 1.0; }
		else { Gbl.errorMsg("This should never happen!"); }
		return util;
	}

	@Override
	protected final double calcOtherUtil() {
		double util = 0.0;
		util += B_Const_ot_Shop * 1.0;
		return util;
	}
}
