/* *********************************************************************** *
 * project: org.matsim.*
 * ModelMobilityTools.java
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

package playground.ciarif.models;

import org.matsim.gbl.Gbl;


public class ModelMobilityTools {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private static final double B0_GAS95_P = -1.3747168e+000;
	private static final double B0_HH_DIM = +1.8519173e-001;
	private static final double B0_HH_KIDS = -6.8637703e-002;
	private static final double B1_AGE = +1.5308959e-001;
	private static final double B1_AGE_LN = -1.7366217e+000;
	private static final double B1_AGE_SEX = -2.4642458e-002;
	private static final double B1_AGE_SQ = +1.4655707e-003;
	private static final double B1_FRENCH = -1.3304756e+000;
	private static final double B1_GEN = +1.3249964e+000;
	private static final double B1_INC = +3.1856321e-001;
	private static final double B1_INC_LN = -1.0369679e-001;
	private static final double B1_INC_SQ = -1.0161130e-002;
	private static final double B1_ITALIAN = -9.4174300e-001;
	private static final double B1_NAT = +8.9172801e-001;
	private static final double B1_T2 = +4.8744613e-001;
	private static final double B1_T3 = +9.8479954e-001;
	private static final double B1_T4 = +1.2085380e+000;
	private static final double B1_T5 = +1.3534957e+000;
	private static final double B1_W_E_DIST = -3.6895583e-005;
	private static final double B2_AGE = -4.1488669e-001;
	private static final double B2_AGE_LN = +1.0533422e+001;
	private static final double B2_AGE_SEX = +6.1815687e-003;
	private static final double B2_AGE_SQ = +1.6963341e-003;
	private static final double B2_FRENCH = +1.6439993e-001;
	private static final double B2_GEN = +5.2913082e-001;
	private static final double B2_INC = +1.7899927e-001;
	private static final double B2_INC_LN = +1.0274846e-001;
	private static final double B2_INC_SQ = -1.0274846e-001;
	private static final double B2_ITALIAN = +5.5102854e-001;
	private static final double B2_NAT = +7.9417233-001;
	private static final double B2_T2 = +8.3935137e-001;
	private static final double B2_T3 = +1.0932893e+000;
	private static final double B2_T4 = +1.4767330e+000;
	private static final double B2_T5 = +1.7120490e+000;
	private static final double B2_W_E_DIST = +3.1602868e-005;
	private static final double B3_AGE = -6.7972513e-002;
	private static final double B3_AGE_LN = +1.7386385e+000;
	private static final double B3_AGE_SEX = +2.2658816e-003;
	private static final double B3_AGE_SQ = +2.0214453e-004;
	private static final double B3_GEN = -3.7301596e-002;
	private static final double B3_INC = +7.6523606e-002;
	private static final double B3_INC_LN = -4.3553585e-002;
	private static final double B3_INC_SQ = -1.5087563e-003;
	private static final double B3_NAT = +8.0695049e-001;
	private static final double B3_T2 = -6.3631171e-001;
	private static final double B3_T3 = -3.7814508e-001;
	private static final double B3_T4 = -5.8141117e-001;
	private static final double B3_T5 = -9.1254621e-001;
	private static final double B3_W_E_DIST = +4.6258239e-005;
	private static final double B4_AGE = +2.7800712e-001;
	private static final double B4_AGE_LN = -5.0392905e+000;
	private static final double B4_AGE_SEX = -6.9136356e-004;
	private static final double B4_AGE_SQ = -2.0070719e-003;
	private static final double B4_FRENCH = -1.8462767e+000;
	private static final double B4_GEN = +9.3001714e-002;
	private static final double B4_INC = +5.4223347e-001;
	private static final double B4_INC_LN = -5.3339773e-001;
	private static final double B4_INC_SQ = -1.4806047e-002;
	private static final double B4_ITALIAN = -1.7078714e+000;
	private static final double B4_NAT = +1.6586562e+000;
	private static final double B4_T2 = -6.8027995e-002;
	private static final double B4_T3 = +1.6764483e-001;
	private static final double B4_T4 = +2.0102510e-001;
	private static final double B4_T5 = -3.1332454e-002;
	private static final double B4_W_E_DIST = +4.1951885e-005;
	
	private static final double B5_AGE = -2.0857020e-001;
	private static final double B5_AGE_LN = +6.2487346e+000;
	private static final double B5_AGE_SEX = +5.8652982e-003;
	private static final double B5_AGE_SQ = +7.8388047e-004;
	private static final double B5_FRENCH = -6.0817122e-001;
	private static final double B5_GEN = +2.2564616e-001;
	private static final double B5_INC = +3.8396470e-001;
	private static final double B5_INC_LN = -1.5536946e-001;
	private static final double B5_INC_SQ = -7.1894039e-003;
	private static final double B5_ITALIAN = -8.9246762e-001;
	private static final double B5_NAT = +1.3928543e+000;
	private static final double B5_T2 = +3.6643303e-001;
	private static final double B5_T3 = +6.1872312e-001;
	private static final double B5_T4 = +8.6858026e-001;
	private static final double B5_T5 = +7.1583654e-001;
	private static final double B5_W_E_DIST = +4.3733178e-005;
	private static final double KONST1 = +1.1260062-001;
	private static final double KONST2 = -2.6218601e+001;
	private static final double KONST3 = -5.2957412e+000;
	private static final double KONST4 = +8.1598019e+000;
	private static final double KONST5 = -1.8162738e+001;
	
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
	private double language; // german = 1; french = 2; italian = 3
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public ModelMobilityTools() {
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
		this.language = -1.0;
		Gbl.random.nextDouble();
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
		
	public final boolean setLanguage(int language) {
		if ((language < 1) || (3 < language)) { return false; }
		this.language = language;
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
		double r = Gbl.random.nextDouble();
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
		// KONST1 * one + B1_AGE * AGE + B1_AGE_SQ * AGE_SQ + B1_AGE_LN * AGE_LN +
		// B1_GEN * GENDER + B1_AGE_SEX * AGE_SEX + B1_NAT * NAT + B1_W_E_DIST * DIST_W_E +
		// B1_INC * INC_1000 + B1_INC_SQ * INC_SQ + B1_INC_LN * INC_LN +
		// B1_T2 * T2 + B1_T3 * T3 + B1_T4 * T4 + B1_T5 * T5
		// B1_FRENCH * FRENCH + B1_ITALIAN * ITALIAN
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
		if (language == 1) {/*reference*/}
		else if (language == 2) {util += B1_FRENCH * 1.0;}
		else if (language == 3) {util += B1_ITALIAN * 1.0;}
		else { Gbl.errorMsg("This should never happen!"); }
		return util;
	}
	
	private final double calcCARalwaysPTnoUtil() {
		// if (license)
		// KONST2 * one + B2_AGE * AGE + B2_AGE_SQ * AGE_SQ + B2_AGE_LN * AGE_LN +
		// B2_GEN * GENDER + B2_AGE_SEX * AGE_SEX + B2_NAT * NAT + B2_W_E_DIST * DIST_W_E +
		// B2_INC * INC_1000 + B2_INC_SQ * INC_SQ + B2_INC_LN * INC_LN +
		// B2_T2 * T2 + B2_T3 * T3 + B2_T4 * T4 + B2_T5 * T5
		// B2_FRENCH * FRENCH + B2_ITALIAN * ITALIAN
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
		if (language == 1) {/*reference*/}
		else if (language == 2) {util += B2_FRENCH * 1.0;}
		else if (language == 3) {util += B2_ITALIAN * 1.0;}
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
		// B4_FRENCH * FRENCH + B4_ITALIAN * ITALIAN		
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
		if (language == 1) {/*reference*/}
		else if (language == 2) {util += B4_FRENCH * 1.0;}
		else if (language == 3) {util += B4_ITALIAN * 1.0;}
		else { Gbl.errorMsg("This should never happen!"); }
		return util;
	}
	
	private final double calcCARalwaysPTyesUtil() {
		// if (license)
		// KONST5 * one + B5_AGE * AGE + B5_AGE_SQ * AGE_SQ + B5_AGE_LN * AGE_LN +
		// B5_GEN * GENDER + B5_AGE_SEX * AGE_SEX + B5_NAT * NAT + B5_W_E_DIST * DIST_W_E +
		// B5_INC * INC_1000 + B5_INC_SQ * INC_SQ + B5_INC_LN * INC_LN +
		// B5_T2 * T2 + B5_T3 * T3 + B5_T4 * T4 + B5_T5 * T5
		// B5_FRENCH * FRENCH + B5_ITALIAN * ITALIAN
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
		if (language == 1) {/*reference*/}
		else if (language == 2) {util += B5_FRENCH * 1.0;}
		else if (language == 3) {util += B5_ITALIAN * 1.0;}
		else { Gbl.errorMsg("This should never happen!"); }
		return util;
	}
}
