package playground.ciarif.models.subtours;

import org.matsim.gbl.Gbl;

public class ModelModeChoiceShop18Plus extends ModelModeChoice {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////
	static final double B1_CONST =	-3.4337093e-001;
	static final double B1_Car_Always= 	+2.4094841e+000;
	static final double B1_Dist =	-2.3346652e-003;
	static final double B1_Prev =	+1.9329097e+000;
	static final double B1_T2 =	+2.1255681e-001;
	static final double B1_T3 =	+4.0917050e-001;
	static final double B1_T4 =	+4.8753577e-001;
	static final double B1_T5 =	+5.2620142e-001;
	static final double B2_Car_Never= 	+7.4529996e-001;
	static final double B2_Dist 	=-1.2532284e-003;
	static final double B2_Season 	=+1.4779302e+000;
	static final double B2_T2 =	-1.5243462e+000;
	static final double B2_T3 =	-1.3743424e+000;
	static final double B2_T4 =	-1.7576144e+000;
	static final double B2_T5 =	-2.5563621e+000;
	static final double B3_18_30= 	+8.2671253e-001;
	static final double B3_60 	=+7.5909621e-001;
	static final double B3_CONST =	-1.4142765e+000;
	static final double B4_CONST =	+1.0787836e+000;
	static final double B4_Dist 	=-2.7031342e-001;
	static final double B5_CONST 	=+4.2404832e+000;
	static final double B5_Dist 	=-8.7782682e-001;
	static final double B5_Prev 	=+1.4507674e+000;
	static final double B5_T2 	=-7.2577391e-001;
	static final double B5_T3 	=-3.5975554e-001;
	static final double B5_T4 	=-8.6032064e-001;
	static final double B5_T5 	=-8.8317982e-001;

//	static final double B1_CONST =	+7.4606554e-002;
//	static final double B1_Car_Always= 	+2.1476736e+000;
//	static final double B1_Dist =	-1.2645877e-003;
//	static final double B1_Prev =	+3.3794614e+000;
//	static final double B1_T2 =	+5.7914959e-001;
//	static final double B1_T3 =	+6.2863886e-001;
//	static final double B1_T4 	=+9.0690771e-001;
//	static final double B1_T5 	=+9.6153551e-001;
//	static final double B2_Car_Never= 	+6.8292855e-001;
//	static final double B2_Dist =	-2.8544953e-003;
//	static final double B2_Season= 	+1.4175186e+000;
//	static final double B2_T2 	=-8.4727190e-001;
//	static final double B2_T3 	=-8.4404539e-001;
//	static final double B2_T4 	=-9.3857791e-001;
//	static final double B2_T5 	=-1.7615767e+000;
//	static final double B3_18_30 =	+9.0880757e-001;
//	static final double B3_60 	=+7.1144586e-001;
//	static final double B3_CONST =	-8.4785402e-001;
//	static final double B4_CONST =	+5.9842630e-001;
//	static final double B4_Dist 	=-3.0783559e-002;
//	static final double B5_CONST 	=+3.8046249e+000;
//	static final double B5_Dist 	=-7.6948799e-001;
//	static final double B5_Prev 	=-2.3643499e+000;



//	static final double B1_CONST = +8.9541839e-002;
//	static final double B1_Car_Always =	+2.1078673e+000;
//	static final double B1_Dist = -1.2839466e-003;
//	static final double B1_Prev = +2.5808785e+000;
//	static final double B1_T2 = +7.1487363e-001;
//	static final double B1_T3 = +8.1474665e-001;
//	static final double B1_T4 = +1.1111038e+000;
//	static final double B1_T5 = +1.0542291e+000;
//	static final double B2_Car_Never = +5.8946025e-001;
//	static final double B2_Dist = -2.5479685e-003;
//	static final double B2_Season = +1.4605841e+000;
//	static final double B2_T2 = -7.5316347e-001;
//	static final double B2_T3 = -6.2748031e-001;
//	static final double B2_T4 = -7.1830748e-001;
//	static final double B2_T5 = -1.7135462e+000;
//	static final double B3_18_30 = +9.0714680e-001;
//	static final double B3_60 = +6.5563659e-001;
//	static final double B3_CONST = -7.0474160e-001;
//	static final double B4_CONST = +5.9470926e-001;
//	static final double B4_Dist = -1.6601342e-002;
//	static final double B5_CONST = +2.4493962e+000;
//	static final double B5_Dist = -8.6210480e-002;
//	static final double B5_Prev = -1.1184532e+000;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public ModelModeChoiceShop18Plus() {
		super ();
	}

	//////////////////////////////////////////////////////////////////////
	// calc methods
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	//4	Bike	one	B4_CONST * one + B4_Dist * DISTANCE
	//1	Car	one	B1_CONST * one + B1_Dist * DISTANCE + B1_Car_Always * CAR_ALWAYS + B1_Prev * PREV_CAR + B1_T2 * T2 + B1_T3 * T3 + B1_T4 * T4 + B1_T5 * T5
	//3	Car_Passenger	one	B3_CONST * one + B3_18_30 * AGE_18_30 + B3_60 * AGE_60
	//2	PT	one	B2_Season * TICKETS + B2_Car_Never * CAR_NEVER + B2_Dist * DISTANCE + B2_T2 * T2 + B2_T3 * T3 + B2_T4 * T4 + B2_T5 * T5
	//5	Walk	one	B5_CONST * one + B5_Dist * DISTANCE + B5_Prev * PREV_P_P_W

	@Override
	protected final double calcWalkUtil() {
		// Computes for this person the utility of choosing walk as transportation mode 
		// when the tour (plan) has shop as main purpose
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
			if ((prev_mode == 3)||(prev_mode == 5)){
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
		util += B2_Dist * dist_subtour;
		if (car == "never") { util += B2_Car_Never *  1.0; }
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
		if (license == 0) {
			if (ride) {
				double util = 0.0;
				util += B5_CONST * 1.0;
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

