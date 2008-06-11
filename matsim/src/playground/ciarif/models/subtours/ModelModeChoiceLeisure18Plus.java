package playground.ciarif.models.subtours;

import org.matsim.gbl.Gbl;

public class ModelModeChoiceLeisure18Plus  extends ModelModeChoice {
	
	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////
	
	static final double B0_CONST =	+5.0071426e-001;
	static final double B0_Car_Always =	+2.1672107e+000;
	static final double B0_Dist =	-6.5594332e-003;
	static final double B0_Prev =	+1.2361418e+000;
	static final double B0_T2 	=+3.4298426e-001;
	static final double B0_T3 =	+3.6207798e-001;
	static final double B0_T4 =	+6.8457208e-001;
	static final double B0_T5 =	+4.8574526e-001;
	static final double B1_60 =	+2.5469566e-001;
	static final double B1_Car_Never =	+9.2315048e-001;
	static final double B1_Season =	+1.3995956e+000;
	static final double B1_T2 =	-1.1686738e+000;
	static final double B1_T3 =	-8.5048065e-001;
	static final double B1_T4 =	-1.0678690e+000;
	static final double B1_T5 = -1.5003359e+000;
	static final double B2_18_30 = +6.4140885e-001;
	static final double B2_60 = +1.4232495e+000;
	static final double B2_CONST = -1.0429183e+000;
	static final double B2_Dist = -6.1355223e-003;
	static final double B3_CONST =	+1.1204255e+000;
	static final double B3_Dist =	-1.2383845e-001;
	static final double B4_CONST = +4.5647355e+000;
	static final double B4_Dist = -7.6487993e-001;
	static final double B4_Prev = +1.2641550e+000;
	static final double B4_T2 =	-3.9271834e-001;
	static final double B4_T3 =	-3.3476311e-001;
	static final double B4_T4 =	-4.4715447e-001;
	static final double B4_T5 =	-8.9515703e-001;

//	3	Bike	one	B3_CONST * one + B3_Dist * DISTANCE
//	0	Car	one	B0_CONST * one + B0_Dist * DISTANCE + B0_Car_Always * CAR_ALWAYS + B0_Prev * PREV_CAR + B0_T2 * T2 + B0_T3 * T3 + B0_T4 * T4 + B0_T5 * T5
//	2	Car_Passenger	one	B2_CONST * one + B2_Dist * DISTANCE + B2_18_30 * AGE_18_30 + B2_60 * AGE_60
//	1	PT	one	B1_Season * TICKETS + B1_Car_Never * CAR_NEVER + B1_60 * AGE_60 + B1_T2 * T2 + B1_T3 * T3 + B1_T4 * T4 + B1_T5 * T5
//	4	Walk	one	B4_CONST * one + B4_Dist * DISTANCE + B4_Prev * PREV_CAR + B4_T2 * T2 + B4_T3 * T3 + B4_T4 * T4 + B4_T5 * T5

//	static final double B1_CONST =	+9.5258432e-001;
//	static final double B1_Car_Always= 	+1.8146609e+000;
//	static final double B1_Dist =	-7.7998280e-004;
//	static final double B1_Prev =	+4.0741151e+000;
//	static final double B1_T2 =	+4.4338261e-001;
//	static final double B1_T3 	=+4.5697900e-001;
//	static final double B1_T4 	=+6.6189024e-001;
//	static final double B1_T5 	=+5.3088768e-001;
//	static final double B2_60 	=+4.3546113e-001;
//	static final double B2_Car_Never= 	+8.2878638e-001;
//	static final double B2_Season 	=+1.4425663e+000;
//	static final double B2_T2 	=-6.8517332e-001;
//	static final double B2_T3 	=-2.8769770e-001;
//	static final double B2_T4 	=-7.7544614e-001;
//	static final double B2_T5 	=-1.3176770e+000;
//	static final double B3_18_30 =	+7.0474106e-001;
//	static final double B3_60 =	+1.4979252e+000;
//	static final double B3_CONST =	-9.5490447e-001;
//	static final double B3_Dist =	-1.6232741e-003;
//	static final double B4_CONST =	+4.8999787e-001;
//	static final double B4_Dist =	-1.7425768e-002;
//	static final double B5_CONST =	+3.5822097e+000;
//	static final double B5_Dist =	-5.8316592e-001;
//	static final double B5_Prev =	-4.1263314e+000;
//	static final double B5_T2 =	-6.3414577e-002;
//	static final double B5_T3 =	+1.1242930e-001;
//	static final double B5_T4 =	+1.3407326e-001;
//	static final double B5_T5 =	-3.1322907e-001;
// 

	//4	Bike	one	B4_CONST * one + B4_Dist * DISTANCE
	//1	Car	one	B1_CONST * one + B1_Dist * DISTANCE + B1_Car_Always * CAR_ALWAYS + B1_Prev * PREV_CAR + B1_T2 * T2 + B1_T3 * T3 + B1_T4 * T4 + B1_T5 * T5
	//3	Car_Passenger	one	B3_CONST * one + B3_Dist * DISTANCE + B3_18_30 * AGE_18_30 + B3_60 * AGE_60
	//2	PT	one	B2_Season * TICKETS + B2_Car_Never * CAR_NEVER + B2_60 * AGE_60 + B2_T2 * T2 + B2_T3 * T3 + B2_T4 * T4 + B2_T5 * T5
	//5	Walk	one	B5_CONST * one + B5_Dist * DISTANCE + B5_Prev * PREV_P_P_W

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
	protected final double calcCarUtil() {
		if (license == 1) {
			if ((prev_mode == -1)||(prev_mode == 0)) {
				double util = 0.0;
				util += B0_CONST * 1.0;
				util += B0_Dist * dist_subtour;
				if (prev_mode == 0) {util += B0_Prev * 1.0;}
				if (car == "always") { util += B0_Car_Always * 1.0; }
				if (udeg == 1) { util += 0;/* reference type */ }
				else if (udeg == 2) { util += B0_T2 * 1.0; }
				else if (udeg == 3) { util += B0_T3 * 1.0; }
				else if (udeg == 4) { util += B0_T4 * 1.0; }
				else if (udeg == 5) { util += B0_T5 * 1.0; }
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
		util += B1_Season * tickets ;
		if (car == "never") { util += B1_Car_Never *  1.0; }
		if (age >= 60 ) { util += B1_60 * 1.0; }
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
		if (license ==0) {
			if (ride) {
				double util = 0.0;
				util += B2_Dist * dist_subtour;
				util += B2_CONST * 1.0;
				if (age >= 18 & age < 30) {util += B2_18_30 * 1.0;}
				if (age >= 60 ) { util += B2_60 * 1.0; }
				//System.out.println("Util ride = " + util);
				return util;
			}
			else {return Double.NEGATIVE_INFINITY;}
		}
		else {return Double.NEGATIVE_INFINITY;}
	}
	
	@Override
	protected final double calcBikeUtil() {
		if (bike) {
			if ((prev_mode == 3)||(prev_mode == -1)) {
				double util = 0.0;
				util += B3_CONST * 1.0;
				util += B3_Dist * dist_subtour;
				//System.out.println("Util bike = " + util);
				return util;
			}
			else {return Double.NEGATIVE_INFINITY;}
		}
		else {return Double.NEGATIVE_INFINITY;}
	}
	
	@Override
	protected final double calcWalkUtil() {
		// Computes for this person the utility of choosing walk as transportation mode 
		// when the tour (plan) has Leisure as main purpose
		// if (one)
		// B_Const_w* 1 + B_Dist_w * T_DIST
		double util = 0.0;
		util += B4_CONST * 1.0;
		util += B4_Dist * dist_subtour;
		if (prev_mode == 0) {util += B4_Prev * 1.0;}
		if (udeg == 1) { util += 0;/* reference type */ }
		else if (udeg == 2) { util += B4_T2 * 1.0; }
		else if (udeg == 3) { util += B4_T3 * 1.0; }
		else if (udeg == 4) { util += B4_T4 * 1.0; }
		else if (udeg == 5) { util += B4_T5 * 1.0; }
		//System.out.println("Util walk = " + util);
		return util;
	}
	
}
