package playground.ciarif.models.subtours;

import org.matsim.gbl.Gbl;

public class ModelModeChoiceLeisure18Plus  extends ModelModeChoice {
	
	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////
	static final double B1_CONST =	+9.5258432e-001;
	static final double B1_Car_Always= 	+1.8146609e+000;
	static final double B1_Dist =	-7.7998280e-004;
	static final double B1_Prev =	+4.0741151e+000;
	static final double B1_T2 =	+4.4338261e-001;
	static final double B1_T3 	=+4.5697900e-001;
	static final double B1_T4 	=+6.6189024e-001;
	static final double B1_T5 	=+5.3088768e-001;
	static final double B2_60 	=+4.3546113e-001;
	static final double B2_Car_Never= 	+8.2878638e-001;
	static final double B2_Season 	=+1.4425663e+000;
	static final double B2_T2 	=-6.8517332e-001;
	static final double B2_T3 	=-2.8769770e-001;
	static final double B2_T4 	=-7.7544614e-001;
	static final double B2_T5 	=-1.3176770e+000;
	static final double B3_18_30 =	+7.0474106e-001;
	static final double B3_60 =	+1.4979252e+000;
	static final double B3_CONST =	-9.5490447e-001;
	static final double B3_Dist =	-1.6232741e-003;
	static final double B4_CONST =	+4.8999787e-001;
	static final double B4_Dist =	-1.7425768e-002;
	static final double B5_CONST =	+3.5822097e+000;
	static final double B5_Dist =	-5.8316592e-001;
	static final double B5_Prev =	-4.1263314e+000;
	static final double B5_T2 =	-6.3414577e-002;
	static final double B5_T3 =	+1.1242930e-001;
	static final double B5_T4 =	+1.3407326e-001;
	static final double B5_T5 =	-3.1322907e-001;
 
//	static final double B1_CONST =	+9.5553425e-001;
//	static final double B1_Car_Always =	+1.8115556e+000;
//	static final double B1_Dist =	-7.8557519e-004;
//	static final double B1_Prev =	+4.0762680e+000;
//	static final double B1_T2 =	+4.8602886e-001;
//	static final double B1_T3 =	+3.9614821e-001;
//	static final double B1_T4 =	+5.9134940e-001;
//	static final double B1_T5 =	+7.1647480e-001;
//	static final double B2_60 =	+4.3426624e-001;
//	static final double B2_Car_Never = 	+8.3078374e-001;
//	static final double B2_Season =	+1.4440766e+000;
//	static final double B2_T2 =	-6.4330312e-001;
//	static final double B2_T3 =	-3.4456595e-001;
//	static final double B2_T4 =	-8.4064198e-001;
//	static final double B2_T5 =	-1.1389486e+000;
//	static final double B3_18_30 =	+7.0035974e-001;
//	static final double B3_60 =	+1.4998404e+000;
//	static final double B3_CONST =	-9.2478752e-001;
//	static final double B3_Dist =	-1.5850306e-003;
//	static final double B4_CONST =	+5.1731400e-001;
//	static final double B4_Dist =	-1.7226169e-002;
//	static final double B5_CONST =	+3.5616866e+000;
//	static final double B5_Dist =	-5.7858224e-001;
//	static final double B5_Prev =	-4.1253010e+000;



//	static final double B1_CONST = +9.7135851e-001;
//	static final double B1_Car_Always = +1.7863477e+000;
//	static final double B1_Dist = -8.0133764e-004;
//	static final double B1_Prev = +3.8342053e+000;
//	static final double B1_T2 = +4.8196222e-001;
//	static final double B1_T3 = +3.9743825e-001;
//	static final double B1_T4 = +5.7954086e-001;
//	static final double B1_T5 = +6.8172148e-001;
//	static final double B2_60 = +4.2002117e-001;
//	static final double B2_Car_Never = +8.3618328e-001;
//	static final double B2_Season = +1.4526892e+000;
//	static final double B2_T2 = -6.5672879e-001;
//	static final double B2_T3 = -3.4072912e-001;
//	static final double B2_T4 = -8.4616918e-001;
//	static final double B2_T5 = -1.1763996e+000;
//	static final double B3_18_30 = +7.1519671e-001;
//	static final double B3_60 = +1.4935628e+000;
//	static final double B3_CONST = -9.3709542e-001;
//	static final double B3_Dist = -1.5869066e-003;
//	static final double B4_CONST = +5.0186587e-001;
//	static final double B4_Dist = -1.6833622e-002;
//	static final double B5_CONST = +3.2303337e+000;
//	static final double B5_Dist = -4.1487873e-001;
//	static final double B5_Prev = -3.8047074e+000;

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
		if ((prev_mode == 2) || (prev_mode == 4)|| (prev_mode == 1)) {util += B5_Prev * 1.0;}
//		if (udeg == 1) { util += 0;/* reference type */ }
//		else if (udeg == 2) { util += B5_T2 * 1.0; }
//		else if (udeg == 3) { util += B5_T3 * 1.0; }
//		else if (udeg == 4) { util += B5_T4 * 1.0; }
//		else if (udeg == 5) { util += B5_T5 * 1.0; }
		//System.out.println("Util walk = " + util);
		return util;
	}

	@Override
	protected final double calcBikeUtil() {
		if (bike) {
			if ((prev_mode == 3)||(prev_mode == 5)) {
				double util = 0.0;
				util += B4_CONST * 1.0;
				util += B4_Dist * dist_subtour;
				//System.out.println("Util bike = " + util);
				return util;
			}
			else {return Double.NEGATIVE_INFINITY;}
		}
		else {return Double.NEGATIVE_INFINITY;}
	}
	
	@Override
	protected final double calcCarUtil() {
		if (license == 1) {
			if ((prev_mode == 5)||(prev_mode == 0)) {
				double util = 0.0;
				util += B1_CONST * 1.0;
				util += B1_Dist * dist_subtour;
				if (prev_mode == 0) {util += B1_Prev * 1.0;}
				if (car == "always") { util += B1_Car_Always * 1.0; }
				if (udeg == 1) { util += 0;/* reference type */ }
				else if (udeg == 2) { util += B1_T2 * 1.0; }
				else if (udeg == 3) { util += B1_T3 * 1.0; }
				else if (udeg == 4) { util += B1_T4 * 1.0; }
				else if (udeg == 5) { util += B1_T5 * 1.0; }
				else { Gbl.errorMsg("This should never happen!"); }
				//System.out.println("Util car = " + util);
				return util;
			}
			else {return Double.NEGATIVE_INFINITY;}	
		}
		else {return Double.NEGATIVE_INFINITY;}
	}
	 
	@Override
	protected final double calcPublicUtil() {
		double util = 0.0;
		util += B2_Season * tickets ;
		if (car == "never") { util += B2_Car_Never *  1.0; }
		if (age >= 60 ) { util += B2_60 * 1.0; }
		if (udeg == 1) { util += 0;/* reference type */ }
		else if (udeg == 2) { util += B2_T2 * 1.0; }
		else if (udeg == 3) { util += B2_T3 * 1.0; }
		else if (udeg == 4) { util += B2_T4 * 1.0; }
		else if (udeg == 5) { util += B2_T5 * 1.0; }
		else { Gbl.errorMsg("This should never happen!"); }
		//System.out.println("Util pt = " + util);
		return util;
	}

	@Override
	protected final double calcCarRideUtil() {
		if (license ==0) {
			if (ride) {
				double util = 0.0;
				util += B3_CONST * 1.0;
				if (age >= 18 & age < 30) {util += B3_18_30 * 1.0;}
				if (age >= 60 ) { util += B3_60 * 1.0; }
				//System.out.println("Util ride = " + util);
				return util;
			}
			else {return Double.NEGATIVE_INFINITY;}
		}
		else {return Double.NEGATIVE_INFINITY;}
	}
}
