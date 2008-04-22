package playground.ciarif.models.subtours;


import org.matsim.gbl.Gbl;

public class ModelModeChoiceEducation18Plus extends ModelModeChoice {
		
		//////////////////////////////////////////////////////////////////////
		// member variables
		//////////////////////////////////////////////////////////////////////
		
		static final double B1_CONST = -2.4216678e-001;
		static final double B1_Car_Always	= +2.4723895e+000;
		static final double B1_Dist = +1.1649350e+000;
		static final double B1_Prev =	+3.4951771e+000;
		static final double B1_T2 =	-4.6955261e-001;
		static final double B1_T3 = +1.3053780e-002;
		static final double B1_T4 =	-9.8112696e-001;
		static final double B1_T5 = -3.0486070e-001;
		static final double B2_Dist = +1.1652987e+000;
		static final double B2_Prev =	+9.8087247e-001;
		static final double B2_Season  = +2.1381660e+000;
		static final double B2_T2 = +3.5402915e-001;
		static final double B2_T3 =	+3.3905042e-001;
		static final double B2_T4 = -7.1233272e-001;
		static final double B2_T5 =	-7.3461326e-001;
		static final double B3_18_30 = +2.6793573e+000;
		static final double B3_CONST = -2.6492674e+000;
		static final double B3_Dist = +1.0563418e+000;
		static final double B3_Prev = +2.0302658e+000;
		static final double B4_CONST = +2.7938237e+000;
		static final double B4_Dist = +7.7803800e-001;
		static final double B5_CONST = +4.5111231e+000;
		static final double B5_Prev  = +2.1951780e+000;

		//////////////////////////////////////////////////////////////////////
		// constructors
		//////////////////////////////////////////////////////////////////////

		public ModelModeChoiceEducation18Plus() {
			super ();
		}

		//////////////////////////////////////////////////////////////////////
		// calc methods
		//////////////////////////////////////////////////////////////////////

		//4	Bike	one	B4_CONST * one + B4_Dist * DISTANCE
		//1	Car	one	B1_CONST * one + B1_Dist * DISTANCE + B1_Car_Always * CAR_ALWAYS + B1_Prev * PREV_CAR + B1_T2 * T2 + B1_T3 * T3 + B1_T4 * T4 + B1_T5 * T5
		//3	Car_Passenger	one	B3_CONST * one + B3_Dist * DISTANCE + B3_Prev * PREV_P_P_W + B3_18_30 * AGE_18_30
		//2	PT	one	B2_Season * TICKETS + B2_Dist * DISTANCE + B2_Prev * PREV_P_P_W + B2_T2 * T2 + B2_T3 * T3 + B2_T4 * T4 + B2_T5 * T5
		//5	Walk	one	B5_CONST * one + B5_Prev * PREV_P_P_W

		//////////////////////////////////////////////////////////////////////

		@Override
		protected final double calcWalkUtil() {
			// Computes for this person the utility of choosing walk as transportation mode 
			// when the tour (plan) has education as main purpose
			double util = 0.0;
			util += B5_CONST * 1.0;
			if ((prev_mode == 1) || (prev_mode == 2)|| (prev_mode == 4)) {util += B5_Prev * 1.0;}
			//System.out.println("Util walk = " + util);
			return util;
		}

		@Override
		protected final double calcBikeUtil() {
			if (bike) { 
				if ((prev_mode == 3)||(prev_mode == 5) ) {
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
			if (license == 1){
				//System.out.println("prev_mode_model = " + prev_mode);
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
			if ((prev_mode == 2) || (prev_mode == 4)|| (prev_mode == 1)) {util += B2_Prev * 1.0;}
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
					util += B3_Dist * dist_subtour;
					if ((prev_mode == 2) || (prev_mode == 4)|| (prev_mode == 1)) {util += B3_Prev * 1.0;}
					if (age <= 30 & age > 18) { util += B3_18_30 * 1.0; }
					//System.out.println("Util ride = " + util);
					return util;
				}
				else {return Double.NEGATIVE_INFINITY;}
			}
			else {return Double.NEGATIVE_INFINITY;}
		}

	}

