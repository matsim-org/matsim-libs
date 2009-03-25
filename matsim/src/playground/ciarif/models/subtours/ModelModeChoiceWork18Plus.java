package playground.ciarif.models.subtours;

import org.matsim.core.gbl.Gbl;



public class ModelModeChoiceWork18Plus extends ModelModeChoice {

	////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////
	
	static final double B0_CONST 	=+3.4313444e-001;
	static final double B0_Car_Always= 	+2.5331318e+000;
	static final double B0_Dist 	=-6.6685792e-004;
	static final double B0_H_W 	=-1.4513607e-002;
	static final double B0_Male =	+8.5592668e-002;
	static final double B0_Prev =	+1.3329686e+000;
	static final double B0_T2 	=+7.2477753e-001;
	static final double B0_T3 	=+4.4789813e-001;
	static final double B0_T4 	=+5.7980215e-001;
	static final double B0_T5 	=+7.2871285e-001;
	static final double B1_18_29 =	+3.8415430e-001;
	static final double B1_45_59 =	-3.6085276e-002;
	static final double B1_60 	=-1.3831191e-001;
	static final double B1_Car_Never= 	+5.7198567e-001;
	static final double B1_Dist 	=+3.4945593e-003;
	static final double B1_H_W 	=+5.0181932e-004;
	static final double B1_Prev =	-1.1515017e+000;
	static final double B1_Season= 	+2.4285239e+000;
	static final double B1_T2 	=-1.9977253e-001;
	static final double B1_T3 	=-2.5132876e-001;
	static final double B1_T4 	=-5.3509061e-001;
	static final double B1_T5 	=-1.2316214e+000;
	static final double B2_18_29 =	+1.1789386e+000;
	static final double B2_45_59 =	-5.2031003e-002;
	static final double B2_60 	=+1.0135066e+000;
	static final double B2_CONST =	-9.2016267e-001;
	static final double B2_H_W 	=-7.6199805e-002;
	static final double B2_Prev =	+2.3807534e-002;
	static final double B3_CONST =	+2.1356128e+000;
	static final double B3_Dist 	=-1.7516994e-001;
	static final double B3_H_W 	=-1.2602296e-003;
	static final double B3_Prev =	+2.3924436e+000;
	static final double B4_CONST =	+4.8942048e+000;
	static final double B4_Dist 	=-9.9099871e-001;
	static final double B4_Prev_p 	=+1.9477603e-001;
	static final double B4_T2 	=-4.8377780e-002;
	static final double B4_T3 	=-5.8741573e-001;
	static final double B4_T4 	=-6.2286719e-001;
	static final double B4_T5 	=-7.3411362e-001;

//	3	Bike	one	B3_CONST * one + B3_Dist * DISTANCE + B3_H_W * DIST_W_H_Km + B3_Prev * PREV_BIKE
//	0	Car	one	B0_CONST * one + B0_Dist * DISTANCE + B0_H_W * DIST_W_H_Km + B0_Car_Always * CAR_ALWAYS + B0_Male * GENDER + B0_Prev * PREV_CAR + B0_T2 * T2 + B0_T3 * T3 + B0_T4 * T4 + B0_T5 * T5
//	2	Car_Passenger	one	B2_CONST * one + B2_H_W * DIST_W_H_Km + B2_Prev * PREV_P_P_W + B2_18_29 * AGE_18_29 + B2_60 * AGE_60 + B2_45_59 * AGE_45_59
//	1	PT	one	B1_Season * TICKETS + B1_Car_Never * CAR_NEVER + B1_Dist * DISTANCE + B1_H_W * DIST_W_H_Km + B1_Prev * PREV_CAR + B1_18_29 * AGE_18_29 + B1_60 * AGE_60 + B1_45_59 * AGE_45_59 + B1_T2 * T2 + B1_T3 * T3 + B1_T4 * T4 + B1_T5 * T5
//	4	Walk	one	B4_CONST * one + B4_Dist * DISTANCE + B4_Prev * PREV_CAR + B4_T2 * T2 + B4_T3 * T3 + B4_T4 * T4 + B4_T5 * T5

	/// Old parameters (old MZ 2005 models with different filters for the discarded cases)
	
//	static final double B1_CONST =	+1.8882393e-001;
//	static final double B1_Car_Always =	+2.3372796e+000;
//	static final double B1_Dist =	-1.3380393e-003;
//	static final double B1_H_W 	=-2.6733272e-002;
//	static final double B1_Male =	+9.9899026e-002;
//	static final double B1_Prev =	+2.0845765e+000;
//	static final double B1_T2 =	+9.5298925e-001;
//	static final double B1_T3 =	+7.0506967e-001;
//	static final double B1_T4 =	+7.2432797e-001;
//	static final double B1_T5 =	+9.2768081e-001;
//	static final double B2_18_29 	=+3.9287417e-001;
//	static final double B2_45_59 	=+4.8061662e-002;
//	static final double B2_60 	=-1.2472391e-001;
//	static final double B2_Car_Never =	+5.9568335e-001;
//	static final double B2_Dist =	-9.4613857e-004;
//	static final double B2_H_W 	=-4.9518338e-003;
//	static final double B2_Season 	=+2.3585496e+000;
//	static final double B2_T2 	=-3.3901023e-001;
//	static final double B2_T3 =	-5.0063364e-001;
//	static final double B2_T4 =	-6.2397783e-001;
//	static final double B2_T5 =	-1.2641238e+000;
//	static final double B3_18_29 =	+1.3834010e+000;
//	static final double B3_45_59 =	+2.7049466e-002;
//	static final double B3_60 =	+1.1173729e+000;
//	static final double B3_CONST =	-1.2874782e+000;
//	static final double B3_H_W =	-7.8024945e-002;
//	static final double B3_Prev =	+9.4790378e-001;
//	static final double B4_CONST =	+1.7996603e+000;
//	static final double B4_Dist =	-1.4960764e-001;
//	static final double B4_H_W =	-1.5839790e-002;
//	static final double B4_Prev =	+2.0724291e+000;
//	static final double B5_CONST =	+4.4659319e+000;
//	static final double B5_Dist =	-7.6627930e-001;
//	static final double B5_Prev =	+1.9598123e+000;
//	static final double B5_T2 =	-7.2784258e-002;
//	static final double B5_T3 =	-7.3125224e-001;
//	static final double B5_T4 	=-8.6718190e-001;
//	static final double B5_T5 	=-1.0190004e+000;

	//4	Bike	one	B4_CONST * one + B4_Dist * DISTANCE + B4_H_W * DIST_W_H_Km + B4_Prev * PREV_BIKE
	//1	Car	one	B1_CONST * one + B1_Dist * DISTANCE + B1_H_W * DIST_W_H_Km + B1_Car_Always * CAR_ALWAYS + B1_Male * GENDER + B1_Prev * PREV_CAR + B1_T2 * T2 + B1_T3 * T3 + B1_T4 * T4 + B1_T5 * T5
	//3	Car_Passenger	one	B3_CONST * one + B3_H_W * DIST_W_H_Km + B3_Prev * PREV_P_P_W + B3_18_29 * AGE_18_29 + B3_60 * AGE_60 + B3_45_59 * AGE_45_59
	//2	PT	one	B2_Season * TICKETS + B2_Car_Never * CAR_NEVER + B2_Dist * DISTANCE + B2_H_W * DIST_W_H_Km + B2_18_29 * AGE_18_29 + B2_60 * AGE_60 + B2_45_59 * AGE_45_59 + B2_T2 * T2 + B2_T3 * T3 + B2_T4 * T4 + B2_T5 * T5
	//5	Walk	one	B5_CONST * one + B5_Dist * DISTANCE + B5_Prev * PREV_P_P_W + B5_T2 * T2 + B5_T3 * T3 + B5_T4 * T4 + B5_T5 * T5
	
	


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

	//modes= 0:
	@Override

	protected final double calcCarUtil() {
		
		if (license == 1){
			//System.out.println("prev_mode_model = " + prev_mode);
			if (prev_mode == 5 || prev_mode == -1) {
				double util = 0.0;
				util += B0_CONST * 1.0;
				util += B0_Dist * dist_subtour;
				util += B0_H_W * dist_h_w; 
				if (male == "m") { util += B0_Male * 1.0; }
				if (car == "always") { util += B0_Car_Always * 1.0; }
				if (prev_mode == 0) { util += B0_Prev * 1.0;} 
				if (udeg == 1) { util += 0; /* reference type */ }
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
//		System.out.println("pt = " + pt);
		if (pt) {
			double util = 0.0;
			util += B1_Season * tickets ;
			util += B1_H_W * dist_h_w;
			util +=B1_Dist * dist_subtour;
			if (prev_mode == 0) {util+=B1_Prev * 1.0;}
			if (age >= 18 & age < 30) {util += B1_18_29 * 1.0;}
			if (age >= 45 & age < 59) {util += B1_45_59 * 1.0;}
			if (age >= 60) {util += B1_60 * 1.0;}
			if (car == "never") { util += B1_Car_Never *  1.0; }
			if (udeg == 1) { util += 0; /* reference type */ }
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
				//util += B2_Dist * dist_subtour;
				if (age >= 18 & age < 30) {util += B2_18_29 * 1.0;}
				if (age >= 45 & age < 59) {util += B2_45_59 * 1.0;}
				if (age >= 60) {util += B2_60 * 1.0;}
				//util += B2_H_W * dist_h_w;
				if ((prev_mode == 2) || (prev_mode == 4)|| (prev_mode == 1)) {util += B2_Prev * 1.0;}
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
			if ((prev_mode == 3)||(prev_mode == -1) ){
				double util = 0.0;
				util += B3_CONST * 1.0;
				util += B3_Dist * dist_subtour;
				util += B3_H_W * dist_h_w;
				if (prev_mode == 3) {util += B3_Prev * prev_mode;}
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
		// when the tour (plan) has work as main purpose
		double util = 0.0;
		util += B4_CONST * 1.0;
		util += B4_Dist * dist_subtour;
		if ((prev_mode == 2) || (prev_mode == 4)|| (prev_mode == 1)) {util += B4_Prev_p * 1.0;}
		//if (prev_mode == 0) { util += B4_Prev_c * 1.0;}
		if (udeg == 1) { util += 0; /* reference type */ }
		else if (udeg == 2) { util += B4_T2 * 1.0; }
		else if (udeg == 3) { util += B4_T3 * 1.0; }
		else if (udeg == 4) { util += B4_T4 * 1.0; }
		else if (udeg == 5) { util += B4_T5 * 1.0; }
		//System.out.println("Util walk = " + util);
		return util;
	}

	
	
}


