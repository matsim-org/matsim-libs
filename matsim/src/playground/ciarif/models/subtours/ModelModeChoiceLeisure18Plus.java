package playground.ciarif.models.subtours;

import org.matsim.gbl.Gbl;

public class ModelModeChoiceLeisure18Plus  extends ModelModeChoice {
	
	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////
	static final double B1_CONST = +9.7135851e-001;
	static final double B1_Car_Always = +1.7863477e+000;
	static final double B1_Dist = -8.0133764e-004;
	static final double B1_Prev = +3.8342053e+000;
	static final double B1_T2 = +4.8196222e-001;
	static final double B1_T3 = +3.9743825e-001;
	static final double B1_T4 = +5.7954086e-001;
	static final double B1_T5 = +6.8172148e-001;
	static final double B2_60 = +4.2002117e-001;
	static final double B2_Car_Never = +8.3618328e-001;
	static final double B2_Season = +1.4526892e+000;
	static final double B2_T2 = -6.5672879e-001;
	static final double B2_T3 = -3.4072912e-001;
	static final double B2_T4 = -8.4616918e-001;
	static final double B2_T5 = -1.1763996e+000;
	static final double B3_18_30 = +7.1519671e-001;
	static final double B3_60 = +1.4935628e+000;
	static final double B3_CONST = -9.3709542e-001;
	static final double B3_Dist = -1.5869066e-003;
	static final double B4_CONST = +5.0186587e-001;
	static final double B4_Dist = -1.6833622e-002;
	static final double B5_CONST = +3.2303337e+000;
	static final double B5_Dist = -4.1487873e-001;
	static final double B5_Prev = -3.8047074e+000;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public ModelModeChoiceLeisure18Plus() {
		super ();
	}

	//////////////////////////////////////////////////////////////////////
	// calc methods
	//////////////////////////////////////////////////////////////////////
	
	//4	Bike	one	B4_CONST * one + B4_Dist * DISTANCE
	//1	Car	one	B1_CONST * one + B1_Dist * DISTANCE + B1_Car_Always * CAR_ALWAYS + B1_Prev * PREV_CAR + B1_T2 * T2 + B1_T3 * T3 + B1_T4 * T4 + B1_T5 * T5
	//3	Car_Passenger	one	B3_CONST * one + B3_Dist * DISTANCE + B3_18_30 * AGE_18_30 + B3_60 * AGE_60
	//2	PT	one	B2_Season * TICKETS + B2_Car_Never * CAR_NEVER + B2_60 * AGE_60 + B2_T2 * T2 + B2_T3 * T3 + B2_T4 * T4 + B2_T5 * T5
	//5	Walk	one	B5_CONST * one + B5_Dist * DISTANCE + B5_Prev * PREV_P_P_W

	//////////////////////////////////////////////////////////////////////

	@Override
	protected final double calcWalkUtil() {
		// Computes for this person the utility of choosing walk as transportation mode 
		// when the tour (plan) has Leisure as main purpose
		// if (one)
		// B_Const_w* 1 + B_Dist_w * T_DIST
		double util = 0.0;
		util += B5_CONST * 1.0;
		util += B5_Dist * dist_subtour;
		if ((prev_mode == 2) || (prev_mode == 4)|| (prev_mode == 5)) {util += B5_Prev * 1.0;}
		return util;
	}

	@Override
	protected final double calcBikeUtil() {
		if (!bike) { return Double.NEGATIVE_INFINITY; }
		if ((prev_mode == 4)||(prev_mode == 0)) {
			double util = 0.0;
			util += B4_CONST * 1.0;
			util += B4_Dist * dist_subtour;
			return util;
		}
		else {return Double.NEGATIVE_INFINITY;}
	}
	
	@Override
	protected final double calcCarUtil() {
		if (license==0)  {return Double.NEGATIVE_INFINITY;}
		if ((prev_mode == 3)||(prev_mode == 0)) {
			double util = 0.0;
			util += B1_CONST * 1.0;
			util += B1_Dist * dist_subtour;
			if (prev_mode == 1) {util += B1_Prev * 1.0;}
			if (car == "always") { util += B1_Car_Always * 1.0; }
			if (udeg == 1) { /* reference type */ }
			else if (udeg == 2) { util += B1_T2 * 1.0; }
			else if (udeg == 3) { util += B1_T3 * 1.0; }
			else if (udeg == 4) { util += B1_T4 * 1.0; }
			else if (udeg == 5) { util += B1_T5 * 1.0; }
			else { Gbl.errorMsg("This should never happen!"); }
			return util;
		}
		else {return Double.NEGATIVE_INFINITY;}	
	}
	 
	@Override
	protected final double calcPublicUtil() {
		double util = 0.0;
		util += B2_Season * tickets ;
		if (car == "never") { util += B2_Car_Never *  1.0; }
		if (udeg == 1) { /* reference type */ }
		if (age >= 60 ) { util += B2_60 * 1.0; }
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
		if (age >= 18 & age < 30) {util += B3_18_30 * 1.0;}
		if (age >= 60 ) { util += B3_60 * 1.0; }
		return util;
	}
}
