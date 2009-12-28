package playground.ciarif.models.subtours;


import org.matsim.core.gbl.Gbl;

public class ModelModeChoiceEducation18Plus extends ModelModeChoice {
		
		//////////////////////////////////////////////////////////////////////
		// member variables
		//////////////////////////////////////////////////////////////////////
	
	static final double B0_CONST =	-9.6010828e-001;
	static final double B0_Car_Always =	+3.1205750e+000;
	static final double B0_Dist =	+4.0353585e-002;
	static final double B0_T2 =	-1.0382223e+000;
	static final double B0_T3 =	-8.0818441e-001;
	static final double B0_T4 =	-8.7181072e-001;
	static final double B0_T5 =	-1.4212187e-001;
	static final double B1_Dist =	+4.2491464e-002;
	static final double B1_Prev =	+1.8407637e+000;
	static final double B1_Season =	+1.8322595e+000;
	static final double B1_T2 	=-6.6705396e-002;
	static final double B1_T3 =	+4.0876398e-001;
	static final double B1_T4 =	-8.7370922e-001;
	static final double B1_T5 =	-4.1536910e-001;
	static final double B2_CONST =	-8.5167866e-001;
	static final double B3_CONST =	+2.5247565e+000;
	static final double B3_Dist =	-3.1979212e-001;
	static final double B4_CONST =	+4.3708623e+000;
	static final double B4_Dist =	-1.2690865e+000;

//	3	Bike	one	B3_CONST * one + B3_Dist * DISTANCE
//	0	Car	one	B0_CONST * one + B0_Dist * DISTANCE + B0_Car_Always * CAR_ALWAYS + B0_T2 * T2 + B0_T3 * T3 + B0_T4 * T4 + B0_T5 * T5
//	2	Car_Passenger	one	B2_CONST * one
//	1	PT	one	B1_Season * TICKETS + B1_Dist * DISTANCE + B1_Prev * PREV_P_P_W + B1_T2 * T2 + B1_T3 * T3 + B1_T4 * T4 + B1_T5 * T5
//	4	Walk	one	B4_CONST * one + B4_Dist * DISTANCE

	//Old version wiht other filters
	
//	static final double B1_CONST =	-7.0002406e-001;
//	static final double B1_Car_Always 	=+2.6034433e+000;
//	static final double B1_Dist =	+1.0617650e-001;
//	static final double B1_Prev =	+2.8953502e+000;
//	static final double B1_T2 =	+2.0706084e-001;
//	static final double B1_T3 =	+5.3884391e-001;
//	static final double B1_T4 =	-4.4745173e-001;
//	static final double B1_T5 =	+3.1666737e-001;
//	static final double B2_Dist =	+1.1425651e-001;
//	static final double B2_Prev =	+8.5672341e+000;
//	static final double B2_Season =	+2.1013993e+000;
//	static final double B2_T2 	=+4.3334178e-001;
//	static final double B2_T3 =	+3.6919145e-001;
//	static final double B2_T4 =	-7.3837802e-001;
//	static final double B2_T5 =	-7.2974722e-001;
//	static final double B3_18_30 	=+2.6244885e+000;
//	static final double B3_CONST 	=-2.5390169e+000;
//	static final double B3_Prev 	=+9.6032362e+000;
//	static final double B4_CONST =	+3.0145083e+000;
//	static final double B4_Dist =	-3.0646942e-001;
//	static final double B5_CONST =	+5.0042208e+000;
//	static final double B5_Dist =	-1.2548186e+000;
//	static final double B5_Prev =	+1.0000000e+001;


	//4	Bike	one	B4_CONST * one + B4_Dist * DISTANCE
	//1	Car	one	B1_CONST * one + B1_Dist * DISTANCE + B1_Car_Always * CAR_ALWAYS + B1_Prev * PREV_CAR + B1_T2 * T2 + B1_T3 * T3 + B1_T4 * T4 + B1_T5 * T5
	//3	Car_Passenger	one	B3_CONST * one + B3_Dist * DISTANCE + B3_Prev * PREV_P_P_W + B3_18_30 * AGE_18_30
	//2	PT	one	B2_Season * TICKETS + B2_Dist * DISTANCE + B2_Prev * PREV_P_P_W + B2_T2 * T2 + B2_T3 * T3 + B2_T4 * T4 + B2_T5 * T5
	//5	Walk	one	B5_CONST * one + B5_Prev * PREV_P_P_W


		//////////////////////////////////////////////////////////////////////
		// constructors
		//////////////////////////////////////////////////////////////////////

		public ModelModeChoiceEducation18Plus() {
			super ();
		}

		//////////////////////////////////////////////////////////////////////
		// calc methods
		//////////////////////////////////////////////////////////////////////


		//////////////////////////////////////////////////////////////////////

		@Override
		protected final double calcCarUtil() {
			if (license == 1){
				//System.out.println("prev_mode_model = " + prev_mode);
				if ((prev_mode == -1)||(prev_mode == 0)) {
				double util = 0.0;
				util += B0_CONST * 1.0;
				util += B0_Dist * dist_subtour;
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
			util += B1_Dist * dist_subtour;
			if ((prev_mode == 2) || (prev_mode == 4)|| (prev_mode == 1)) {util += B1_Prev * 1.0;}
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
					util += B2_CONST * 1.0;
					//util += B2_Dist * dist_subtour;
					//if ((prev_mode == 2) || (prev_mode == 4)|| (prev_mode == 1)) {util += B2_Prev * 1.0;}
					//if (age <= 30 & age > 18) { util += B2_18_30 * 1.0; }
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
				if ((prev_mode == 3)||(prev_mode == -1) ) {
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
			// when the tour (plan) has education as main purpose
			double util = 0.0;
			util += B4_CONST * 1.0;
			util += B4_Dist * dist_subtour;
			//if ((prev_mode == 1) || (prev_mode == 2)|| (prev_mode == 4)) {util += B4_Prev * 1.0;}
			//System.out.println("Util walk = " + util);
			return util;
		}

		
		
}

