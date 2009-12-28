/* *********************************************************************** *
 * project: org.matsim.*
 * ModelMobiliyTools.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.balmermi.census2000.models;

import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;


public class ModelMobiliyTools {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private static final double B0_GAS95_P = -1.2966111e+000;
	private static final double B0_HH_DIM = +1.7888815e-001;
	private static final double B0_HH_KIDS = -6.0345396e-002;
	private static final double B1_AGE = -4.1142020e-001;
	private static final double B1_AGE_LN = +1.0487204e+001;
	private static final double B1_AGE_SEX = +6.5068026e-003;
	private static final double B1_AGE_SQ = +1.6735518e-003;
	private static final double B1_GEN = +4.9748673e-001;
	private static final double B1_INC = +1.8244275e-001;
	private static final double B1_INC_LN = +9.2333183e-002;
	private static final double B1_INC_SQ = -1.3267506e-003;
	private static final double B1_NAT = +7.9061431e-001;
	private static final double B1_T2 = +8.0914593e-001;
	private static final double B1_T3 = +1.0173742e+000;
	private static final double B1_T4 = +1.4482215e+000;
	private static final double B1_T5 = +1.6553746e+000;
	private static final double B1_W_E_DIST = +3.1965827e-005;
	private static final double B2_AGE = +1.1169789e-001;
	private static final double B2_AGE_LN = -7.8777321e-001;
	private static final double B2_AGE_SEX = -2.3197102e-002;
	private static final double B2_AGE_SQ = -1.2680699e-003;
	private static final double B2_GEN = +1.2979959e+000;
	private static final double B2_INC = +2.6911024e-001;
	private static final double B2_INC_LN = -6.2580308e-002;
	private static final double B2_INC_SQ = -7.3349707e-003;
	private static final double B2_NAT = +9.7202665e-001;
	private static final double B2_T2 = +4.1975881e-001;
	private static final double B2_T3 = +1.0855230e+000;
	private static final double B2_T4 = +1.2696477e+000;
	private static final double B2_T5 = +1.4620838e+000;
	private static final double B2_W_E_DIST = -3.4506246e-005;
	private static final double B3_AGE = -6.8808711e-002;
	private static final double B3_AGE_LN = +1.7592579e+000;
	private static final double B3_AGE_SEX = +2.1404138e-003;
	private static final double B3_AGE_SQ = +2.0673981e-004;
	private static final double B3_GEN = -2.9110260e-002;
	private static final double B3_INC = +7.6598698e-002;
	private static final double B3_INC_LN = -4.9907398e-002;
	private static final double B3_INC_SQ = -1.4519428e-003;
	private static final double B3_NAT = +8.0151096e-001;
	private static final double B3_T2 = -6.2150274e-001;
	private static final double B3_T3 = -3.6259175e-001;
	private static final double B3_T4 = -5.6562218e-001;
	private static final double B3_T5 = -8.9638120e-001;
	private static final double B3_W_E_DIST = +4.6739595e-005;
	private static final double B4_AGE = +2.2306074e-001;
	private static final double B4_AGE_LN = -3.7505776e+000;
	private static final double B4_AGE_SEX = +4.7601853e-004;
	private static final double B4_AGE_SQ = -1.7478106e-003;
	private static final double B4_GEN = +8.4237775e-002;
	private static final double B4_INC = +5.1130230e-001;
	private static final double B4_INC_LN = -5.0922461e-001;
	private static final double B4_INC_SQ = -1.2715212e-002;
	private static final double B4_NAT = +1.7690551e+000;
	private static final double B4_T2 = -1.5946686e-001;
	private static final double B4_T3 = +2.9296120e-001;
	private static final double B4_T4 = +2.5693041e-001;
	private static final double B4_T5 = +9.7614018e-002;
	private static final double B4_W_E_DIST = +4.0916742e-005;
	private static final double B5_AGE = -2.2126041e-001;
	private static final double B5_AGE_LN = +6.5853088e+000;
	private static final double B5_AGE_SEX = +6.2192207e-003;
	private static final double B5_AGE_SQ = +8.3078652e-004;
	private static final double B5_GEN = +2.3468957e-001;
	private static final double B5_INC = +3.5878527e-001;
	private static final double B5_INC_LN = -1.4017708e-001;
	private static final double B5_INC_SQ = -5.7133168e-003;
	private static final double B5_NAT = +1.4634557e+000;
	private static final double B5_T2 = +3.1991903e-001;
	private static final double B5_T3 = +6.8787838e-001;
	private static final double B5_T4 = +8.8884457e-001;
	private static final double B5_T5 = +7.7071775e-001;
	private static final double B5_W_E_DIST = +4.3573387e-005;
	private static final double KONST1 = -2.5901608e+001;
	private static final double KONST2 = -2.2305965e+000;
	private static final double KONST3 = -5.2442145e+000;
	private static final double KONST4 = +4.8906751e+000;
	private static final double KONST5 = -1.9048166e+001;
	
	private double age; // 0-[unlimited]
	private double sex; // male = 1; female = 0
	private double nat; // ch = 1; other = 0
	private double nump; // number of persons of the household
	private double numk; // number of kids of the household
	private double inc; // monthly income of the household (in 1000 SFr)
	private double udeg; // degree of urbanization [2-5] (1=urbanized=reference)
	private double license; // yes = 1; no = 0;
	private double disthw; // Euclidean distance between home and work (in meters)
	private double fuelcost; // av. cost of one liter fuel per municipality
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public ModelMobiliyTools() {
		this.age = -1.0;
		this.sex = -1.0;
		this.nat = -1.0;
		this.nump = -1.0;
		this.numk = -1.0;
		this.inc = -1.0;
		this.udeg = -1.0;
		this.license = -1.0;
		this.disthw = -1.0;
		this.fuelcost = -1.0;
		MatsimRandom.getRandom().nextDouble();
	}

	//////////////////////////////////////////////////////////////////////
	// set methods
	//////////////////////////////////////////////////////////////////////

	public final boolean setAge(int age) {
		if (age < 0) { return false; }
		this.age = age;
		return true;
	}
	
	public final boolean setSex(boolean male) {
		if (male) { this.sex = 1.0; }
		else { this.sex = 0.0; }
		return true;
	}

	public final boolean setNationality(boolean swiss) {
		if (swiss) { this.nat = 1.0; }
		else { this.nat = 0.0; }
		return true;
	}

	public final boolean setHHDimension(int nump) {
		if (nump <= 0) { return false; }
		this.nump = nump;
		return true;
	}

	public final boolean setHHKids(int numk) {
		if (numk < 0) { return false; }
		this.numk = numk;
		return true;
	}

	public final boolean setIncome(double inc) {
		if (inc <= 0) { return false; }
		this.inc = inc;
		return true;
	}

	public final boolean setUrbanDegree(int udeg) {
		if ((udeg < 1) || (5 < udeg)) { return false; }
		this.udeg = udeg;
		return true;
	}

	public final boolean setLicenseOwnership(boolean license) {
		if (license) { this.license = 1.0; }
		else { this.license = 0.0; }
		return true;
	}

	public final boolean setDistanceHome2Work(double distance) {
		if (distance < 0.0) { return false; }
		this.disthw = distance;
		return true;
	}

	public final boolean setFuelCost(double fuelcost) {
		if (fuelcost <= 0.0) { return false; }
		this.fuelcost = fuelcost;
		return true;
	}
	
	//////////////////////////////////////////////////////////////////////
	// calc methods
	//////////////////////////////////////////////////////////////////////

	  /**
	   * Calculates the combination of car_avail and public transport ticket
	   * ownership.
	   * 
	   * @return values between 0 and 5.<BR>
	   * 
	   * the meanings of the values are:<BR>
	   * <code>0: car_avail=never; pt_ticket=no</code><br>
	   * <code>1: car_avail=sometimes; pt_ticket=no</code><br>
	   * <code>2: car_avail=always; pt_ticket=no</code><br>
	   * <code>3: car_avail=never; pt_ticket=yes</code><br>
	   * <code>4: car_avail=sometimes; pt_ticket=yes</code><br>
	   * <code>5: car_avail=always; pt_ticket=yes</code><br>
	   */
	public final int calcMobilityTools() {
		double[] utils = new double[6];
		utils[0] = this.calcCARneverPTnoUtil();
		utils[1] = this.calcCARsometimesPTnoUtil();
		utils[2] = this.calcCARalwaysPTnoUtil();
		utils[3] = this.calcCARneverPTyesUtil();
		utils[4] = this.calcCARsometimesPTyesUtil();
		utils[5] = this.calcCARalwaysPTyesUtil();
		double [] probs = this.calcLogitProbability(utils);
		double r = MatsimRandom.getRandom().nextDouble();
		double prob_sum = 0.0;
		for (int i=0; i<probs.length; i++) {
			prob_sum += probs[i];
			if (r < prob_sum) { return i; }
		}
		Gbl.errorMsg("It should never reach this line!");
		return -1;
	}
	
	//////////////////////////////////////////////////////////////////////
	// calc methods (private)
	//////////////////////////////////////////////////////////////////////

	private final double[] calcLogitProbability(double[] utils) {
		double exp_sum = 0.0;
		for (int i=0; i<utils.length; i++) { exp_sum += Math.exp(utils[i]); }
		double [] probs = new double[utils.length];
		for (int i=0; i<utils.length; i++) { probs[i] = Math.exp(utils[i])/exp_sum; }
		return probs;
	}

	//////////////////////////////////////////////////////////////////////

	private final double calcCARneverPTnoUtil() {
		// if (one)
		// B0_GAS95_P * GAS_95 + B0_HH_DIM * HH_DIM + B0_HH_KIDS * HH_KIDS
		double util = 0.0;
		util += B0_GAS95_P * fuelcost;
		util += B0_HH_DIM * nump;
		util += B0_HH_KIDS * numk;
		return util;
	}
	
	private final double calcCARsometimesPTnoUtil() {
		// if (license)
		// KONST2 * one + B2_AGE * AGE + B2_AGE_SQ * AGE_SQ + B2_AGE_LN * AGE_LN +
		// B2_GEN * GENDER + B2_AGE_SEX * AGE_SEX + B2_NAT * NAT + B2_W_E_DIST * DIST_W_E +
		// B2_INC * INC_1000 + B2_INC_SQ * INC_SQ + B2_INC_LN * INC_LN +
		// B2_T2 * T2 + B2_T3 * T3 + B2_T4 * T4 + B2_T5 * T5
		double util = 0.0;
		if (license == 0.0) { return Double.NEGATIVE_INFINITY; }
		util += KONST2 * 1.0;
		util += B2_AGE * age;
		util += B2_AGE_SQ * (age * age);
		util += B2_AGE_LN * Math.log(age);
		util += B2_GEN * sex;
		util += B2_AGE_SEX * (age * sex);
		util += B2_NAT * nat;
		util += B2_W_E_DIST * disthw;
		util += B2_INC * inc;
		util += B2_INC_SQ * (inc * inc);
		util += B2_INC_LN * Math.log(inc);
		if (udeg == 1) { /* reference type */ }
		else if (udeg == 2) { util += B2_T2 * 1.0; }
		else if (udeg == 3) { util += B2_T3 * 1.0; }
		else if (udeg == 4) { util += B2_T4 * 1.0; }
		else if (udeg == 5) { util += B2_T5 * 1.0; }
		else { Gbl.errorMsg("This should never happen!"); }
		return util;
	}

	private final double calcCARalwaysPTnoUtil() {
		// if (license)
		// KONST1 * one + B1_AGE * AGE + B1_AGE_SQ * AGE_SQ + B1_AGE_LN * AGE_LN +
		// B1_GEN * GENDER + B1_AGE_SEX * AGE_SEX + B1_NAT * NAT + B1_W_E_DIST * DIST_W_E +
		// B1_INC * INC_1000 + B1_INC_SQ * INC_SQ + B1_INC_LN * INC_LN +
		// B1_T2 * T2 + B1_T3 * T3 + B1_T4 * T4 + B1_T5 * T5
		double util = 0.0;
		if (license == 0.0) { return Double.NEGATIVE_INFINITY; }
		util += KONST1 * 1.0;
		util += B1_AGE * age;
		util += B1_AGE_SQ * (age * age); // check that
		util += B1_AGE_LN * Math.log(age);
		util += B1_GEN * sex;
		util += B1_AGE_SEX * (age * sex);
		util += B1_NAT * nat;
		util += B1_W_E_DIST * disthw;
		util += B1_INC * inc;
		util += B1_INC_SQ * (inc * inc);
		util += B1_INC_LN * Math.log(inc);
		if (udeg == 1) { /* reference type */ }
		else if (udeg == 2) { util += B1_T2 * 1.0; }
		else if (udeg == 3) { util += B1_T3 * 1.0; }
		else if (udeg == 4) { util += B1_T4 * 1.0; }
		else if (udeg == 5) { util += B1_T5 * 1.0; }
		else { Gbl.errorMsg("This should never happen!"); }
		return util;
	}
	
	private final double calcCARneverPTyesUtil() {
		// if (one)
		// KONST3 * one + B3_AGE * AGE + B3_AGE_SQ * AGE_SQ + B3_AGE_LN * AGE_LN +
		// B3_GEN * GENDER + B3_AGE_SEX * AGE_SEX + B3_NAT * NAT + B3_W_E_DIST * DIST_W_E +
		// B3_INC * INC_1000 + B3_INC_SQ * INC_SQ + B3_INC_LN * INC_LN +
		// B3_T2 * T2 + B3_T3 * T3 + B3_T4 * T4 + B3_T5 * T5
		double util = 0.0;
		util += KONST3 * 1.0;
		util += B3_AGE * age;
		util += B3_AGE_SQ * (age * age); // check that
		util += B3_AGE_LN * Math.log(age);
		util += B3_GEN * sex;
		util += B3_AGE_SEX * (age * sex);
		util += B3_NAT * nat;
		util += B3_W_E_DIST * disthw;
		util += B3_INC * inc;
		util += B3_INC_SQ * (inc * inc);
		util += B3_INC_LN * Math.log(inc);
		if (udeg == 1) { /* reference type */ }
		else if (udeg == 2) { util += B3_T2 * 1.0; }
		else if (udeg == 3) { util += B3_T3 * 1.0; }
		else if (udeg == 4) { util += B3_T4 * 1.0; }
		else if (udeg == 5) { util += B3_T5 * 1.0; }
		else { Gbl.errorMsg("This should never happen!"); }
		return util;
	}
	
	private final double calcCARsometimesPTyesUtil() {
		// if (license)
		// KONST4 * one + B4_AGE * AGE + B4_AGE_SQ * AGE_SQ + B4_AGE_LN * AGE_LN +
		// B4_GEN * GENDER + B4_AGE_SEX * AGE_SEX + B4_NAT * NAT + B4_W_E_DIST * DIST_W_E +
		// B4_INC * INC_1000 + B4_INC_SQ * INC_SQ + B4_INC_LN * INC_LN +
		// B4_T2 * T2 + B4_T3 * T3 + B4_T4 * T4 + B4_T5 * T5
		double util = 0.0;
		if (license == 0.0) { return Double.NEGATIVE_INFINITY; }
		util += KONST4 * 1.0;
		util += B4_AGE * age;
		util += B4_AGE_SQ * (age * age); // check that
		util += B4_AGE_LN * Math.log(age);
		util += B4_GEN * sex;
		util += B4_AGE_SEX * (age * sex);
		util += B4_NAT * nat;
		util += B4_W_E_DIST * disthw;
		util += B4_INC * inc;
		util += B4_INC_SQ * (inc * inc);
		util += B4_INC_LN * Math.log(inc);
		if (udeg == 1) { /* reference type */ }
		else if (udeg == 2) { util += B4_T2 * 1.0; }
		else if (udeg == 3) { util += B4_T3 * 1.0; }
		else if (udeg == 4) { util += B4_T4 * 1.0; }
		else if (udeg == 5) { util += B4_T5 * 1.0; }
		else { Gbl.errorMsg("This should never happen!"); }
		return util;
	}
	
	private final double calcCARalwaysPTyesUtil() {
		// if (license)
		// KONST5 * one + B5_AGE * AGE + B5_AGE_SQ * AGE_SQ + B5_AGE_LN * AGE_LN +
		// B5_GEN * GENDER + B5_AGE_SEX * AGE_SEX + B5_NAT * NAT + B5_W_E_DIST * DIST_W_E +
		// B5_INC * INC_1000 + B5_INC_SQ * INC_SQ + B5_INC_LN * INC_LN +
		// B5_T2 * T2 + B5_T3 * T3 + B5_T4 * T4 + B5_T5 * T5
		double util = 0.0;
		if (license == 0.0) { return Double.NEGATIVE_INFINITY; }
		util += KONST5 * 1.0;
		util += B5_AGE * age;
		util += B5_AGE_SQ * (age * age); // check that
		util += B5_AGE_LN * Math.log(age);
		util += B5_GEN * sex;
		util += B5_AGE_SEX * (age * sex);
		util += B5_NAT * nat;
		util += B5_W_E_DIST * disthw;
		util += B5_INC * inc;
		util += B5_INC_SQ * (inc * inc);
		util += B5_INC_LN * Math.log(inc);
		if (udeg == 1) { /* reference type */ }
		else if (udeg == 2) { util += B5_T2 * 1.0; }
		else if (udeg == 3) { util += B5_T3 * 1.0; }
		else if (udeg == 4) { util += B5_T4 * 1.0; }
		else if (udeg == 5) { util += B5_T5 * 1.0; }
		else { Gbl.errorMsg("This should never happen!"); }
		return util;
	}
}
