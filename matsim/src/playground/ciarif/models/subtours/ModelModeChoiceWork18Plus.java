package playground.ciarif.models.subtours;

import org.matsim.gbl.Gbl;



public class ModelModeChoiceWork18Plus extends ModelModeChoice {

	////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////
	static final double B1_CONST = +5.0130499e-001;
	static final double B1_Car_Always = +2.1401317e+000;
	static final double B1_Dist = +4.1378782e-001;
	static final double B1_H_W = -2.2212802e-002;
	static final double B1_Male = +1.3398303e-001;
	static final double B1_Prev = +9.7449264e-001;
	static final double B1_T2 = +7.6227062e-001;
	static final double B1_T3 =	+9.1326466e-001;
	static final double B1_T4 =	+9.7518486e-001;
	static final double B1_T5 =	+1.1907352e+000;
	static final double B2_18_30 = +4.4026395e-001;
	static final double B2_Car_Never = +6.5455351e-001;
	static final double B2_Dist = +4.1438359e-001;
	static final double B2_H_W  = -2.0433965e-003;
	static final double B2_Season = +2.3732134e+000;
	static final double B2_T2 = -3.9347101e-001;
	static final double B2_T3 = -1.6273906e-001;
	static final double B2_T4 =	-2.3523927e-001;
	static final double B2_T5 = -8.4951585e-001;
	static final double B3_18_30 = +1.1128588e+000;
	static final double B3_CONST = -6.6474323e-001;
	static final double B3_Dist = +4.1485737e-001;
	static final double B3_H_W 	= -8.1972995e-002;
	static final double B3_Prev = +1.0705870e+000;
	static final double B4_CONST = +1.9066074e+000;
	static final double B4_Dist = +2.9400672e-001;
	static final double B4_H_W 	= -2.2751501e-002;
	static final double B4_Prev = +1.7256511e+000;
	static final double B5_CONST = +3.4150245e+000;
	static final double B5_Prev = +1.2955542e+000;

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

	
	//4	Bike			B4_CONST * one + B4_Dist * DISTANCE + B4_H_W * DIST_W_H_Km + B4_Prev * PREV_BIKE
	//1	Car				B1_CONST * one + B1_Dist * DISTANCE + B1_H_W * DIST_W_H_Km + B1_Car_Always * CAR_ALWAYS + B1_Male * GENDER + B1_Prev * PREV_CAR + B1_T2 * T2 + B1_T3 * T3 + B1_T4 * T4 + B1_T5 * T5
	//3	Car_Passenger	B3_CONST * one + B3_Dist * DISTANCE + B3_H_W * DIST_W_H_Km + B3_Prev * PREV_P_P_W + B3_18_30 * AGE_18_30
	//2	PT				B2_Season * TICKETS + B2_Car_Never * CAR_NEVER + B2_Dist * DISTANCE + B2_H_W * DIST_W_H_Km + B2_18_30 * AGE_18_30 + B2_T2 * T2 + B2_T3 * T3 + B2_T4 * T4 + B2_T5 * T5
	//5	Walk			B5_CONST * one + B5_Prev * PREV_P_P_W

	@Override
	protected final double calcWalkUtil() {
		// Computes for this person the utility of choosing walk as transportation mode 
		// when the tour (plan) has work as main purpose
		// if (one)
		// B_Const_w* 1 + B_Dist_w * T_DIST
		double util = 0.0;
		util += B5_CONST * 1.0;
		if ((prev_mode == 2) || (prev_mode == 4)|| (prev_mode == 5)) {util += B5_Prev * 1.0;}
		return util;
	}

	@Override
	protected final double calcBikeUtil() {
		if (!bike) { return Double.NEGATIVE_INFINITY; }
		double util = 0.0;
		util += B4_CONST * 1.0;
		util += B4_Dist * dist_subtour;
		util += B4_H_W * dist_w_e;
		if (prev_mode == 4) {util += B4_Prev * prev_mode;}
		return util;
	}
	
	@Override
	protected final double calcCarUtil() {
		double util = 0.0;
		util += B1_CONST * 1.0;
		util += B1_Dist * dist_subtour;
		util += B1_H_W * dist_w_e;
		if (male == "m") { util += B1_Male * 1.0; }
		if (car == "always") { util += B1_Car_Always * 1.0; }
		if (udeg == 1) { /* reference type */ }
		else if (udeg == 2) { util += B1_T2 * 1.0; }
		else if (udeg == 3) { util += B1_T3 * 1.0; }
		else if (udeg == 4) { util += B1_T4 * 1.0; }
		else if (udeg == 5) { util += B1_T5 * 1.0; }
		else { Gbl.errorMsg("This should never happen!"); }
		return util;
	}
	 
	@Override
	protected final double calcPublicUtil() {
		double util = 0.0;
		util += B2_Season * tickets ;
		util += B2_H_W * dist_w_e;
		if (age >= 18 & age < 30) {util += B2_18_30 * 1.0;}
		if (car == "never") { util += B2_Car_Never *  1.0; }
		if (udeg == 1) { /* reference type */ }
		else if (udeg == 2) { util += B2_T2 * 1.0; }
		else if (udeg == 3) { util += B2_T3 * 1.0; }
		else if (udeg == 4) { util += B2_T4 * 1.0; }
		else if (udeg == 5) { util += B2_T5 * 1.0; }
		else { Gbl.errorMsg("This should never happen!"); }
		return util;
	}

	@Override
	protected final double calcCarRideUtil() {
		double util = 0.0;
		util += B3_CONST * 1.0;
		util += B3_Dist * dist_subtour;
		util += B3_H_W * dist_w_e;
		if ((prev_mode == 2) || (prev_mode == 4)|| (prev_mode == 5)) {util += B3_Prev * 1.0;}
		if (age <= 30 & age > 18) { util += B3_18_30 * 1.0; }
		return util;
	}
}


