/* *********************************************************************** *
 * project: org.matsim.*
 * ModelModeChoiceEducation.java
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

public class ModelModeChoiceEducation extends ModelModeChoice {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	static final double B_Dist_w_Edu = -3.4622068e-002;		
	static final double B_Const_w_Edu = +1.1371760e+000;
	static final double B_Dist_b_Edu = -8.2977921e-004;		
	static final double B_Const_b_Edu = -1.1032706e-001;
	static final double B_Dist_c_Edu = +1.6640945e-002;		
	static final double B_Const_c_Edu = -9.3205115e-001;
	static final double B_Lic_c_Edu = +8.0324356e-001;
	static final double B_T2_c_Edu = -7.2619376e-001;
	static final double B_T3_c_Edu = -1.1043757e-000;
	static final double B_T4_c_Edu = +1.0049912e-001;
	static final double B_T5_c_Edu = +2.2958927e-002;
	static final double B_Car_always_Edu = +2.2057535e+000;
	static final double B_Dist_pt_Edu = +1.8457565e-002;		
	static final double B_Season_pt_Edu = +1.6100989e+000;
	static final double B_Age_sq_Edu = +2.2835746e-004;
	static final double B_T2_pt_Edu = -4.4615663e-001;
	static final double B_T3_pt_Edu = -4.9524897e-001;
	static final double B_T4_pt_Edu = -1.2490401e-001;
	static final double B_T5_pt_Edu = -2.5164752e-001;
	static final double B_pt_car_never_Edu = -5.3451279e-001;
	static final double B_Const_ot_Edu = -1.4406206e-000;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public ModelModeChoiceEducation() {
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
		util += B_Const_w_Edu * 1.0;
		util += B_Dist_w_Edu * dist;
		return util;
	}

	@Override
	protected final double calcBikeUtil() {
		if (!bike) { return Double.NEGATIVE_INFINITY; }
		double util = 0.0;
		util += B_Const_b_Edu * 1.0;
		util += B_Dist_b_Edu * dist;
		return util;
	}

	@Override
	protected final double calcCarUtil() {
		double util = 0.0;
		util += B_Const_c_Edu * 1.0;
		util += B_Dist_c_Edu * dist;
		util += B_Lic_c_Edu * license;
		if (car == "always") { util += B_Car_always_Edu * 1.0; }
		if (udeg == 1) { /* reference type */ }
		else if (udeg == 2) { util += B_T2_c_Edu * 1.0; }
		else if (udeg == 3) { util += B_T3_c_Edu * 1.0; }
		else if (udeg == 4) { util += B_T4_c_Edu * 1.0; }
		else if (udeg == 5) { util += B_T5_c_Edu * 1.0; }
		else { Gbl.errorMsg("This should never happen!"); }
		return util;
	}

	@Override
	protected final double calcPublicUtil() {
		double util = 0.0;
		util += B_Season_pt_Edu * tickets ;
		util += B_Age_sq_Edu * (age * age);
		util += B_Dist_pt_Edu * dist;
		if (car == "never"){ util += B_pt_car_never_Edu * 1.0; }
		if (udeg == 1) { /* reference type */ }
		else if (udeg == 2) { util += B_T2_pt_Edu * 1.0; }
		else if (udeg == 3) { util += B_T3_pt_Edu * 1.0; }
		else if (udeg == 4) { util += B_T4_pt_Edu * 1.0; }
		else if (udeg == 5) { util += B_T5_pt_Edu * 1.0; }
		else { Gbl.errorMsg("This should never happen!"); }
		return util;
	}

	@Override
	protected final double calcOtherUtil() {
		double util = 0.0;
		util += B_Const_ot_Edu * 1.0;
		return util;
	}
}
