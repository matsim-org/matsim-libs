package playground.ciarif.models.subtours;

import org.matsim.core.gbl.Gbl;

public class ModelModeChoiceOther18Minus extends ModelModeChoice {
	
	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	static final double B1_Dist =	+1.6180224e-004;
	static final double B1_Season =	+1.1787878e+000;
	static final double B1_T2 =	-5.1869157e-001;
	static final double B1_T3 =	-8.4501697e-001;
	static final double B1_T4 =	-1.4219061e+000;
	static final double B1_T5 =	-1.5547946e+000;
	static final double B2_6_12 =	+1.8856752e+000;
	static final double B2_CONST =	-2.9855559e-001;
	static final double B3_CONST =	+1.5208155e+000;
	static final double B3_Dist =	-3.1202074e-001;
	static final double B4_6_12 =	+4.6814364e-001;
	static final double B4_CONST =	+3.5007160e+000;
	static final double B4_Dist =	-1.1477140e+000;
//
//	3	Bike	one	B3_CONST * one + B3_Dist * DISTANCE
//	2	Car_Passenger	one	B2_CONST * one + B2_6_12 * AGE_6_12
//	1	PT	one	B1_Season * TICKETS + B1_Dist * DISTANCE + B1_T2 * T2 + B1_T3 * T3 + B1_T4 * T4 + B1_T5 * T5
//	4	Walk	one	B4_CONST * one + B4_Dist * DISTANCE + B4_6_12 * AGE_6_12

//	static final double B2_Dist = +3.8443530e-004;
//	static final double B2_Season = +1.2177049e+000;
//	static final double B2_T2 = -1.0764101e+000;
//	static final double B2_T3 = -7.0101782e-001;
//	static final double B2_T4 = -1.3145273e+000;
//	static final double B2_T5 = -1.4777988e+000;
//	static final double B3_6_12 = +1.5479209e+000;
//	static final double B3_CONST = +1.0497098e-001;
//	static final double B4_CONST = +5.1439056e-001;
//	static final double B4_Dist = -1.3065237e-001;
//	static final double B5_6_12 = +8.7437010e-001;
//	static final double B5_CONST = +7.2182640e-001;
//	static final double B5_Dist = -1.3787648e-001;

	//4	Bike	one	B4_CONST * one + B4_Dist * DISTANCE
	//3	Car_Passenger	one	B3_CONST * one + B3_6_12 * AGE_6_12
	//2	PT	one	B2_Season * TICKETS + B2_Dist * DISTANCE + B2_T2 * T2 + B2_T3 * T3 + B2_T4 * T4 + B2_T5 * T5
	//5	Walk	one	B5_CONST * one + B5_Dist * DISTANCE + B5_6_12 * AGE_6_12
	
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
	protected final double calcPublicUtil() {
		double util = 0.0;
		util += B1_Season * tickets ;
		util += B1_Dist * dist_subtour;
		if (udeg == 1) { /* reference type */ }
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
			if ((prev_mode == 3)|| (prev_mode == -1)) {
				double util = 0.0;
				util += B3_CONST * 1.0;
				util += B3_Dist * dist_subtour;
				//System.out.println("Util bike = " + util);
				return util;
			}
			else {return Double.NEGATIVE_INFINITY;}
		}
		else { return Double.NEGATIVE_INFINITY; }
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

	
	
	@Override
	protected final double calcCarUtil () {
		double util = Double.NEGATIVE_INFINITY;
		return util;
	}
	
	

	
}

