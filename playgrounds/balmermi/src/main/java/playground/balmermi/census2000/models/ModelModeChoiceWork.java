/* *********************************************************************** *
 * project: org.matsim.*
 * ModelModeChoiceWork.java
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

public class ModelModeChoiceWork extends ModelModeChoice {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	static final double B_Dist_w_Work = -3.2773065e-001;
	static final double B_Const_w_Work = +2.4705414e+000;
	static final double B_Dist_b_Work = -6.7554472e-002;		
	static final double B_Const_b_Work = +1.2865056e+000;
	static final double B_Dist_c_Work = +1.8278377e-002;		
	static final double B_Const_c_Work = +1.8875411e-001;
	static final double B_Lic_c_Work = +3.9967454e-001;
	static final double B_T2_c_Work = +4.0581360e-001;
	static final double B_T3_c_Work = +4.1148877e-001;
	static final double B_T4_c_Work = +6.3737487e-001;
	static final double B_T5_c_Work = +5.9674345e-001;
	static final double B_Car_always_Work = +1.8670932e+000;
	static final double B_Dist_pt_Work = +2.2111449e-002;
	static final double B_Season_pt_Work = +2.4361779e+000;
	static final double B_Age_sq_Work = -1.0945597e-004;
	static final double B_T2_pt_Work = -3.3640439e-001;
	static final double B_T3_pt_Work = -7.5685136e-002;
	static final double B_T4_pt_Work = -2.8762912e-001;
	static final double B_T5_pt_Work = -9.6655080e-001;
	static final double B_pt_car_never_Work = +5.3410438e-001;
	static final double B_Const_ot_Work = +1.4143329e-001;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public ModelModeChoiceWork() {
		super();
	}

	//////////////////////////////////////////////////////////////////////
	// calc methods
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////

	@Override
	protected final double calcWalkUtil() {
		//Computes for this person the utility of choosing walk as transportation mode 
		//when the tour (plan) has work as main purpose
		// if (one)
		// B_Const_w* 1 + B_Dist_w * T_DIST
		double util = 0.0;
		util += B_Const_w_Work * 1.0;
		util += B_Dist_w_Work * dist;
		return util;
	}

	@Override
	protected final double calcBikeUtil() {
		if (!bike) { return Double.NEGATIVE_INFINITY; }
		double util = 0.0;
		util += B_Const_b_Work * 1.0;
		util += B_Dist_b_Work * dist;
		return util;
	}

	@Override
	protected final double calcCarUtil() {
		double util = 0.0;
		util += B_Const_c_Work * 1.0;
		util += B_Dist_c_Work * dist;
		util += B_Lic_c_Work * license;
		if (car == "always") { util += B_Car_always_Work * 1.0; }
		if (udeg == 1) { /* reference type */ }
		else if (udeg == 2) { util += B_T2_c_Work * 1.0; }
		else if (udeg == 3) { util += B_T3_c_Work * 1.0; }
		else if (udeg == 4) { util += B_T4_c_Work * 1.0; }
		else if (udeg == 5) { util += B_T5_c_Work * 1.0; }
		else { Gbl.errorMsg("This should never happen!"); }
		return util;
	}

	@Override
	protected final double calcPublicUtil() {
		double util = 0.0;
		util += B_Season_pt_Work * tickets ;
		util += B_Age_sq_Work * (age * age);
		util += B_Dist_pt_Work * dist;
		if (car == "never") { util += B_pt_car_never_Work * 1.0; }
		if (udeg == 1) { /* reference type */ }
		else if (udeg == 2) { util += B_T2_pt_Work * 1.0; }
		else if (udeg == 3) { util += B_T3_pt_Work * 1.0; }
		else if (udeg == 4) { util += B_T4_pt_Work * 1.0; }
		else if (udeg == 5) { util += B_T5_pt_Work * 1.0; }
		else { Gbl.errorMsg("This should never happen!"); }
		return util;
	}

	@Override
	protected final double calcOtherUtil() {
		double util = 0.0;
		util += B_Const_ot_Work * 1.0;
		return util;
	}
}
