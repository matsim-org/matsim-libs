package playground.ciarif.models.subtours;

import org.matsim.gbl.Gbl;

public class ModelModeChoiceEducation18Minus extends ModelModeChoice {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	static final double B2_Dist = -1.5833356e-003;
	static final double B2_Season =	+1.3797913e+000;
	static final double B2_T2 = -3.5533789e-001;
	static final double B2_T3 =	-5.0883457e-001;
	static final double B2_T4 =	-2.4946200e-001;
	static final double B2_T5 =	-8.0357652e-002;
	static final double B3_6_12 = +1.9756793e+000;
	static final double B3_CONST = -1.2797341e+000;
	static final double B4_CONST = +4.4129207e-001;
	static final double B4_Dist = -1.3251270e-002;
	static final double B5_6_12 = +1.7371796e+000;
	static final double B5_CONST = +8.9557041e-001;
	static final double B5_Dist = -4.6043909e-002;
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public ModelModeChoiceEducation18Minus() {
		super ();
	}

	//////////////////////////////////////////////////////////////////////
	// calc methods
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	
	//4	Bike			B4_CONST * one + B4_Dist * DISTANCE
	//3	Car_Passenger	B3_CONST * one + B3_6_12 * AGE_6_12
	//2	PT				B2_Season * TICKETS + B2_Dist * DISTANCE + B2_T2 * T2 + B2_T3 * T3 + B2_T4 * T4 + B2_T5 * T5
	//5	Walk			B5_CONST * one + B5_Dist * DISTANCE + B5_6_12 * AGE_6_12

	
	@Override
	//To verify if isn't better to discard it instead of keeping it with a negative infinity value 
	protected final double calcCarUtil () {
		double util = Double.NEGATIVE_INFINITY;
		return util;
	}
	
	@Override
	protected final double calcPublicUtil() {
		double util = 0.0;
		util += B2_Season * tickets ;
		util += B2_Dist * dist_subtour;
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
		if (age <= 12 & age >= 6 ) { util += B3_6_12 * 1.0; }
		return util;
	}
	
	@Override
	protected final double calcBikeUtil() {
		if (!bike) { return Double.NEGATIVE_INFINITY; }
		double util = 0.0;
		util += B4_CONST * 1.0;
		util += B4_Dist * dist_subtour;
		return util;
	}
	
	@Override
	protected final double calcWalkUtil() {
		// Computes for this person the utility of choosing walk as transportation mode 
		// when the tour (plan) has work as main purpose
		// if (one)
		// B_Const_w* 1 + B_Dist_w * T_DIST
		double util = 0.0;
		util += B5_CONST * 1.0;
		util += B5_Dist * dist_subtour;
		if (age <= 12 & age >= 6 ) { util += B5_6_12 * 1.0; } 
		return util;
	}
}
