package playground.ciarif.models.subtours;

import org.matsim.core.gbl.Gbl;

public class ModelModeChoiceEducation18Minus extends ModelModeChoice {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	
	static final double B1_Dist =	+5.1310009e-003;
	static final double B1_Season =	+1.1907854e+000;
	static final double B1_T2 =	-1.1488506e+000;
	static final double B1_T3 =	-8.7876021e-001;
	static final double B1_T4 =	-3.0167298e-001;
	static final double B1_T5= 	-3.1376447e-001;
	static final double B2_6_12 =	+1.8740869e+000;
	static final double B2_CONST =	-1.5830165e+000;
	static final double B3_CONST =	+3.5428221e-001;
	static final double B3_Dist =	-1.8105471e-002;
	static final double B4_6_12 =	+1.4813182e+000;
	static final double B4_CONST =	+2.5429578e+000;
	static final double B4_Dist =	-9.1441852e-001;

//	Bike	one	B3_CONST * one + B3_Dist * DISTANCE
//	Car_Passenger	one	B2_CONST * one + B2_6_12 * AGE_6_12
//	PT	one	B1_Season * TICKETS + B1_Dist * DISTANCE + B1_T2 * T2 + B1_T3 * T3 + B1_T4 * T4 + B1_T5 * T5
//	Walk	one	B4_CONST * one + B4_Dist * DISTANCE + B4_6_12 * AGE_6_12

//	static final double B2_Dist = -1.5833356e-003;
//	static final double B2_Season =	+1.3797913e+000;
//	static final double B2_T2 = -3.5533789e-001;
//	static final double B2_T3 =	-5.0883457e-001;
//	static final double B2_T4 =	-2.4946200e-001;
//	static final double B2_T5 =	-8.0357652e-002;
//	static final double B3_6_12 = +1.9756793e+000;
//	static final double B3_CONST = -1.2797341e+000;
//	static final double B4_CONST = +4.4129207e-001;
//	static final double B4_Dist = -1.3251270e-002;
//	static final double B5_6_12 = +1.7371796e+000;
//	static final double B5_CONST = +8.9557041e-001;
//	static final double B5_Dist = -4.6043909e-002;
	
	//4	Bike			B4_CONST * one + B4_Dist * DISTANCE
	//3	Car_Passenger	B3_CONST * one + B3_6_12 * AGE_6_12
	//2	PT				B2_Season * TICKETS + B2_Dist * DISTANCE + B2_T2 * T2 + B2_T3 * T3 + B2_T4 * T4 + B2_T5 * T5
	//5	Walk			B5_CONST * one + B5_Dist * DISTANCE + B5_6_12 * AGE_6_12
	
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
	
	

	
	@Override
	//To verify if isn't better to discard it instead of keeping it with a negative infinity value 
	protected final double calcCarUtil () {
		double util = Double.NEGATIVE_INFINITY;
		return util;
	}
	
	@Override
	protected final double calcPublicUtil() {
		double util = 0.0;
		util += B1_Season * tickets ;
		util += B1_Dist * dist_subtour;
		if (udeg == 1) { util += 0;/* reference type */ }
		else if (udeg == 2) { util += B1_T2 * 1.0; }
		else if (udeg == 3) { util += B1_T3 * 1.0; }
		else if (udeg == 4) { util += B1_T4 * 1.0; }
		else if (udeg == 5) { util += B1_T5 * 1.0; }
		else { Gbl.errorMsg("This should never happen!"); }
		//System.out.println("Util pt = " + util);
		return util;
	}

	@Override
	protected final double calcCarRideUtil() {
		if (ride) {
			double util = 0.0;
			util += B2_CONST * 1.0;
			if (age <= 12 & age >= 6 ) { util += B2_6_12 * 1.0; }
			//System.out.println("Util ride = " + util);
			return util;
		}
		else {return Double.NEGATIVE_INFINITY;}
	}
	
	@Override
	protected final double calcBikeUtil() {
		if (bike) {
			if (prev_mode == 3 || prev_mode == -1) {
				double util = 0.0;
				util += B3_CONST * 1.0;
				util += B3_Dist * dist_subtour;
				//System.out.println("Util bike = " + util);
				return util;
			}
			else {return Double.NEGATIVE_INFINITY;}
		}
		else{ return Double.NEGATIVE_INFINITY; }
	}
	
	@Override
	protected final double calcWalkUtil() {
		// Computes for this person the utility of choosing walk as transportation mode 
		// when the tour (plan) has work as main purpose
		// if (one)
		// B_Const_w* 1 + B_Dist_w * T_DIST
		double util = 0.0;
		util += B4_CONST * 1.0;
		util += B4_Dist * dist_subtour;
		if (age <= 12 & age >= 6 ) { util += B4_6_12 * 1.0; } 
		//System.out.println("Util walk = " + util);
		return util;
	}
}
