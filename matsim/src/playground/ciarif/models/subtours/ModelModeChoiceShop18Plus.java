package playground.ciarif.models.subtours;

import org.matsim.gbl.Gbl;

public class ModelModeChoiceShop18Plus extends ModelModeChoice {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	static final double B0_CONST =	-2.0572160e-001;
	static final double B0_Car_Always =	+2.6021860e+000;
	static final double B0_Dist	= +5.3512561e-004;
	static final double B0_Prev = +2.5299529e+000;
	static final double B0_T2 =	-3.0125799e-002;
	static final double B0_T3 = +6.7556349e-001;
	static final double B0_T4 =	+4.6771852e-001;
	static final double B0_T5 = +5.3258786e-001;
	static final double B1_Car_Never = +7.8129405e-001;
	static final double B1_Dist = +2.4131034e-003;
	static final double B1_Season =	+1.6012594e+000;
	static final double B1_T2 =	-2.0484825e+000;
	static final double B1_T3 =	-9.2157124e-001;
	static final double B1_T4 =	-1.4718315e+000;
	static final double B1_T5 =	-2.3202820e+000;
	static final double B2_18_30 =	+1.1451936e+000;
	static final double B2_60 =	+9.0287340e-001;
	static final double B2_CONST =	-1.1470051e+000;
	static final double B3_CONST =	+1.6106302e+000;
	static final double B3_Dist =	-2.9325342e-001;
	static final double B4_CONST =	+5.0715354e+000;
	static final double B4_Dist =	-1.0751933e+000;
	static final double B4_Prev_c =	+1.9542669e+000;
	static final double B4_Prev_w =	+2.0028299e+000;
	static final double B4_T2 	=-7.8817253e-001;
	static final double B4_T3 =	-3.7308066e-001;
	static final double B4_T4 	= -9.2156987e-001;
	static final double B4_T5 =	-9.6988703e-001;
	
//	3	Bike	one	B3_CONST * one + B3_Dist * DISTANCE
//	0	Car	one	B0_CONST * one + B0_Dist * DISTANCE + B0_Car_Always * CAR_ALWAYS + B0_Prev * PREV_CAR + B0_T2 * T2 + B0_T3 * T3 + B0_T4 * T4 + B0_T5 * T5
//	2	Car_Passenger	one	B2_CONST * one + B2_18_30 * AGE_18_30 + B2_60 * AGE_60
//	1	PT	one	B1_Season * TICKETS + B1_Car_Never * CAR_NEVER + B1_Dist * DISTANCE + B1_T2 * T2 + B1_T3 * T3 + B1_T4 * T4 + B1_T5 * T5
//	4	Walk	one	B4_CONST * one + B4_Dist * DISTANCE + B4_Prev_c * PREV_CAR + B4_Prev_w * PREV_P_P_W + B4_T2 * T2 + B4_T3 * T3 + B4_T4 * T4 + B4_T5 * T5

	
//	static final double B1_CONST =	-3.5445134e-001;
//	static final double B1_Car_Always =	+2.4155749e+000;
//	static final double B1_Dist =	-2.3300970e-003;
//	static final double B1_Prev =	+4.1219079e+000;
//	static final double B1_T2 	=+2.1073006e-001;
//	static final double B1_T3 =	+4.0661316e-001;
//	static final double B1_T4 =	+4.8231068e-001;
//	static final double B1_T5 =	+5.2369519e-001;
//	static final double B2_Car_Never =	+7.3583144e-001;
//	static final double B2_Dist 	=-1.2506282e-003;
//	static final double B2_Season 	=+1.4737660e+000;
//	static final double B2_T2 =	-1.5267232e+000;
//	static final double B2_T3 	=-1.3742099e+000;
//	static final double B2_T4 =	-1.7592379e+000;
//	static final double B2_T5 =	-2.5589787e+000;
//	static final double B3_18_30 =	+8.3578608e-001;
//	static final double B3_60 =	+7.5497723e-001;
//	static final double B3_CONST 	=-1.4210068e+000;
//	static final double B4_CONST =	+1.0698927e+000;
//	static final double B4_Dist 	=-2.6991186e-001;
//	static final double B5_CONST =	+4.2426669e+000;
//	static final double B5_Dist 	=-8.7989591e-001;
//	static final double B5_Prev =	+2.6001488e+000;
//	static final double B5_T2 	=-7.3116158e-001;
//	static final double B5_T3 =	-3.6793852e-001;
//	static final double B5_T4 	=-8.6932200e-001;
//	static final double B5_T5 	=-8.8950490e-001;

	//4	Bike	one	B4_CONST * one + B4_Dist * DISTANCE
	//1	Car	one	B1_CONST * one + B1_Dist * DISTANCE + B1_Car_Always * CAR_ALWAYS + B1_Prev * PREV_CAR + B1_T2 * T2 + B1_T3 * T3 + B1_T4 * T4 + B1_T5 * T5
	//3	Car_Passenger	one	B3_CONST * one + B3_18_30 * AGE_18_30 + B3_60 * AGE_60
	//2	PT	one	B2_Season * TICKETS + B2_Car_Never * CAR_NEVER + B2_Dist * DISTANCE + B2_T2 * T2 + B2_T3 * T3 + B2_T4 * T4 + B2_T5 * T5
	//5	Walk	one	B5_CONST * one + B5_Dist * DISTANCE + B5_Prev * PREV_P_P_W

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
		if (pt) {
			double util = 0.0;
			util += B1_Season * tickets ;
			util += B1_Dist * dist_subtour;
			if (car == "never") { util += B1_Car_Never *  1.0; }
			if (udeg == 1) { util += 0;/* reference type */ }
			else if (udeg == 2) { util += B1_T2 * 1.0; }
			else if (udeg == 3) { util += B1_T3 * 1.0; }
			else if (udeg == 4) { util += B1_T4 * 1.0; }
			else if (udeg == 5) { util += B1_T5 * 1.0; }
			else { Gbl.errorMsg("This should never happen!"); }
			//System.out.println("Util pt = " + util);
			return util;
		}
		else {return Double.NEGATIVE_INFINITY;}
	}
	
	@Override
	protected final double calcCarRideUtil() {
		if (license == 0) {
			if (ride) {
				double util = 0.0;
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
			if ((prev_mode == 3)||(prev_mode == -1)){
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
		// when the tour (plan) has shop as main purpose
		double util = 0.0;
		util += B4_CONST * 1.0;
		util += B4_Dist * dist_subtour;
		if (prev_mode == 0) {util += B4_Prev_c * 1.0;}
		if ((prev_mode == 2) || (prev_mode == 4)|| (prev_mode == 1)) {util += B4_Prev_w * 1.0;}
		if (udeg == 1) { util += 0;/* reference type */ }
		else if (udeg == 2) { util += B4_T2 * 1.0; }
		else if (udeg == 3) { util += B4_T3 * 1.0; }
		else if (udeg == 4) { util += B4_T4 * 1.0; }
		else if (udeg == 5) { util += B4_T5 * 1.0; }
		//System.out.println("Util walk = " + util);
		return util;
	}
	
}

