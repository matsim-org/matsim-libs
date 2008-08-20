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

package playground.balmermi.census2000v2.models;

import org.matsim.gbl.Gbl;
import org.matsim.gbl.MatsimRandom;


public class ModelMobilityTools {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private static final double B0_GAS95_P = -1.2957811e+000;
	private static final double B0_HH_DIM = +1.4608430e-001;
	private static final double B0_HH_KIDS = -5.4094257e-002;
	private static final double B1_AGE_18_29 = +9.3633111e-002;
	private static final double B1_AGE_60 = -8.5914328e-001;
	private static final double B1_AGE_SEX = -3.3176276e-002;
	private static final double B1_FRENCH = -1.3222215e+000;
	private static final double B1_GEN = +1.6745826e+000;
	private static final double B1_INC_12 = +9.6015133e-001;
	private static final double B1_INC_4 = -8.3574517e-001;
	private static final double B1_INC_8_12 = +4.1639338e-001;
	private static final double B1_ITALIAN = -9.3684671e-001;
	private static final double B1_NAT = +8.3670645e-001;
	private static final double B1_T2 = +4.6616179e-001;
	private static final double B1_T3 = +9.6655131e-001;
	private static final double B1_T4 = +1.1979197e+000;
	private static final double B1_T5 = +1.3626437e+000;
	private static final double B1_W_E_DIST = -3.5280912e-005;
	private static final double B2_AGE_18_29 = -1.7633305e-001;
	private static final double B2_AGE_60 = -5.3550974e-001;
	private static final double B2_AGE_SEX = +1.5848388e-003;
	private static final double B2_FRENCH = +1.6759431e-001;
	private static final double B2_GEN = +7.0292617e-001;
	private static final double B2_INC_12 = +1.4939409e+000;
	private static final double B2_INC_4 = -7.3483549e-001;
	private static final double B2_INC_8_12 = +6.9240154e-001;
	private static final double B2_ITALIAN = +5.3283188e-001;
	private static final double B2_NAT = +7.05506333-001;
	private static final double B2_T2 = +8.0754814e-001;
	private static final double B2_T3 = +1.0547861e+000;
	private static final double B2_T4 = +1.4480490e+000;
	private static final double B2_T5 = +1.6913581e+000;
	private static final double B2_W_E_DIST = +3.1583928e-005;
	private static final double B3_AGE_12_17 = -1.8218924e-001;
	private static final double B3_AGE_18_29 = +2.9131596e-001;
	private static final double B3_AGE_60 = -2.0115026e-001;
	private static final double B3_AGE_6_11 = -8.112659e-001;
	private static final double B3_AGE_SEX = +1.4142018e-003;
	private static final double B3_GEN = -1.7009944e-002;
	private static final double B3_INC_12 = +3.7940817e-001;
	private static final double B3_INC_4 = -5.8499360e-002;
	private static final double B3_INC_8_12 = +3.5589653e-001;
	private static final double B3_NAT = +7.6781459e-001;
	private static final double B3_T2 = -6.4973000e-001;
	private static final double B3_T3 = -3.9207519e-001;
	private static final double B3_T4 = -5.8959534e-001;
	private static final double B3_T5 = -9.1325318e-001;
	private static final double B3_W_E_DIST = +4.6224506e-005;
	private static final double B4_AGE_18_29 = +3.9536559e-001;
	private static final double B4_AGE_60 = -5.5530334e-001;
	private static final double B4_AGE_SEX = -1.1765808e-002;
	private static final double B4_FRENCH = -1.8035350e+000;
	private static final double B4_GEN = +5.3184756e-001;
	private static final double B4_INC_12 = +1.7285220e+000;
	private static final double B4_INC_4 = -1.0058346e+000;
	private static final double B4_INC_8_12 = +9.4541946e-001;
	private static final double B4_ITALIAN = -1.6527274e+000;
	private static final double B4_NAT = +1.5960829e+000;
	private static final double B4_T2 = -9.0671684e-002;
	private static final double B4_T3 = +1.4828357e-001;
	private static final double B4_T4 = +2.1596504e-001;
	private static final double B4_T5 = +2.4716905e-002;
	private static final double B4_W_E_DIST = +4.2815586e-005;
	private static final double B5_AGE_18_29 = -4.6618995e-001;
	private static final double B5_AGE_60 = +3.8088325e-003;
	private static final double B5_AGE_SEX = +7.7849939e-003;
	private static final double B5_FRENCH = -5.8355826e-001;
	private static final double B5_GEN = +9.3959603e-002;
	private static final double B5_INC_12 = +1.9991203e+000;
	private static final double B5_INC_4 = -1.0593960e+000;
	private static final double B5_INC_8_12 = +9.4722134e-001;
	private static final double B5_ITALIAN = -8.9535261e-001;
	private static final double B5_NAT = +1.3489224e+000;
	private static final double B5_T2 = +3.5924078e-001;
	private static final double B5_T3 = +6.0108309e-001;
	private static final double B5_T4 = +8.6789855e-001;
	private static final double B5_T5 = +7.1406685e-001;
	private static final double B5_W_E_DIST = +4.3587748e-005;
	private static final double KONST1 = -1.2297866+000;
	private static final double KONST2 = -1.4998450e-001;
	private static final double KONST3 = -1.1246506e+000;
	private static final double KONST4 = -8.9209655e-001;
	private static final double KONST5 = -4.6626479e-001;
	
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
		MatsimRandom.random.nextDouble();
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
		double r = MatsimRandom.random.nextDouble();
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
		util += B1_GEN * sex;
		util += B1_AGE_SEX * (age * sex);
		util += B1_NAT * nat;
		util += B1_W_E_DIST * disthw;
		if (age >=30 & age <60) { /*reference type*/}
		else if (age >= 18 & age <30) {util += B1_AGE_18_29 * 1.0; }
		else if (age >= 60) {util += B1_AGE_60 * 1.0; }
		if (inc >=4 & inc < 8){ /* reference type */ }
		else if (inc < 4 ){util += B1_INC_4 * 1.0; }
		else if (inc >=8 & inc < 12) {util += B1_INC_8_12 * 1.0;}
		else if (inc >= 12) {util += B1_INC_12 * 1.0;}
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
		util += B2_GEN * sex;
		util += B2_AGE_SEX * (age * sex);
		util += B2_NAT * nat;
		util += B2_W_E_DIST * disthw;
		if (age >=30 & age <60) { /*reference type*/}
		else if (age >= 18 & age <30) {util += B2_AGE_18_29 * 1.0; }
		else if (age >= 60) {util += B2_AGE_60 * 1.0; }
		if (inc >=4 & inc < 8){ /* reference type */ }
		else if (inc < 4 ){util += B2_INC_4 * 1.0; }
		else if (inc >=8 & inc < 12) {util += B2_INC_8_12 * 1.0;}
		else if (inc >= 12) {util += B2_INC_12 * 1.0;}
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
		util += B3_GEN * sex;
		util += B3_AGE_SEX * (age * sex);
		util += B3_NAT * nat;
		util += B3_W_E_DIST * disthw;
		if (age >=30 & age <60) { /*reference type*/}
		else if (age >= 6 & age <12) {util += B3_AGE_6_11 * 1.0; }
		else if (age >= 12 & age <18) {util += B3_AGE_12_17 * 1.0; }
		else if (age >= 18 & age <30) {util += B3_AGE_18_29 * 1.0; }
		else if (age >= 60) {util += B3_AGE_60 * 1.0; }
		if (inc >=4 & inc < 8){ /* reference type */ }
		else if (inc < 4 ){util += B3_INC_4 * 1.0; }
		else if (inc >=8 & inc < 12) {util += B3_INC_8_12 * 1.0;}
		else if (inc >= 12) {util += B3_INC_12 * 1.0;}
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
		util += B4_GEN * sex;
		util += B4_AGE_SEX * (age * sex);
		util += B4_NAT * nat;
		util += B4_W_E_DIST * disthw;
		if (age >=30 & age <60) { /*reference type*/}
		else if (age >= 18 & age <30) {util += B4_AGE_18_29 * 1.0; }
		else if (age >= 60) {util += B4_AGE_60 * 1.0; }
		if (inc >=4 & inc < 8){ /* reference type */ }
		else if (inc < 4 ){util += B4_INC_4 * 1.0; }
		else if (inc >=8 & inc < 12) {util += B4_INC_8_12 * 1.0;}
		else if (inc >= 12) {util += B4_INC_12 * 1.0;}
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
		util += B5_GEN * sex;
		util += B5_AGE_SEX * (age * sex);
		util += B5_NAT * nat;
		util += B5_W_E_DIST * disthw;
		if (age >=30 & age <60) { /*reference type*/}
		else if (age >= 18 & age <30) {util += B5_AGE_18_29 * 1.0; }
		else if (age >= 60) {util += B5_AGE_60 * 1.0; }
		if (inc >=4 & inc < 8){ /* reference type */ }
		else if (inc < 4 ){util += B5_INC_4 * 1.0; }
		else if (inc >=8 & inc < 12) {util += B5_INC_8_12 * 1.0;}
		else if (inc >= 12) {util += B5_INC_12 * 1.0;}
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
