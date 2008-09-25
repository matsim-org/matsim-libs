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

