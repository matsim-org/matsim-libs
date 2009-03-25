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

import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;


public class ModelMobilityTools {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private static final double B0_GAS95_P=-1.4137140e+000;
	private static final double B0_HH_DIM=+1.1486505e-001;
	private static final double B0_HH_KIDS=-3.1592107e-002;
	private static final double B1_AGE_18_29=+6.5721698e-002;
	private static final double B1_AGE_60=-1.0940296e+000;
	private static final double B1_AGE_SEX=-3.3003062e-002;
	private static final double B1_FRENCH=-1.3151972e+000;
	private static final double B1_GEN=+1.7037798e+000;
	private static final double B1_ITALIAN=-9.7634407e-001;
	private static final double B1_NAT=+8.7164628e-001;
	private static final double B1_T2=+4.7999937e-001;
	private static final double B1_T3=+1.0129119e+000;
	private static final double B1_T4=+1.2673543e+000;
	private static final double B1_T5=+1.3516620e+000;
	private static final double B1_W_E_DIST=-3.1179092e-005;
	private static final double B2_AGE_18_29=-2.0442925e-001;
	private static final double B2_AGE_60=-6.6336813e-001;
	private static final double B2_AGE_SEX=+1.5224115e-003;
	private static final double B2_FRENCH=+1.6201320e-001;
	private static final double B2_GEN=+7.2782313e-001;
	private static final double B2_INC=-1.8971080e-002;
	private static final double B2_INC_LN=+2.6612965e-001;
	private static final double B2_INC_SQ=+4.4271280e-003;
	private static final double B2_ITALIAN=+5.1250160e-001;
	private static final double B2_NAT=+7.3523016e-001;
	private static final double B2_T2=+8.0882729e-001;
	private static final double B2_T3=+1.0733457e+000;
	private static final double B2_T4=+1.4721918e+000;
	private static final double B2_T5=+1.6702310e+000;
	private static final double B2_W_E_DIST=+3.3331150e-005;
	private static final double B3_AGE_12_17=-1.7289749e-001;
	private static final double B3_AGE_18_29=+2.9774713e-001;
	private static final double B3_AGE_60=-2.0929897e-001;
	private static final double B3_AGE_6_11=-8.0056507e-001;
	private static final double B3_AGE_SEX=+1.3126902e-003;
	private static final double B3_GEN=-1.3850250e-002;
	private static final double B3_INC=+1.3502050e-002;
	private static final double B3_INC_LN=+1.0251351e-002;
	private static final double B3_INC_SQ=+5.2471342e-004;
	private static final double B3_NAT=+7.8793274e-001;
	private static final double B3_T2=-6.5696201e-001;
	private static final double B3_T3=-3.9054716e-001;
	private static final double B3_T4=-5.9363520e-001;
	private static final double B3_T5=-9.3238424e-001;
	private static final double B3_W_E_DIST=+4.7252683e-005;
	private static final double B4_AGE_18_29=+3.6431806e-001;
	private static final double B4_AGE_60=-6.7488708e-001;
	private static final double B4_AGE_SEX=-1.1974501e-002;
	private static final double B4_FRENCH=-1.8295791e+000;
	private static final double B4_GEN=+5.6281588e-001;
	private static final double B4_INC=+3.7371091e-001;
	private static final double B4_INC_LN=-4.0560230e-001;
	private static final double B4_INC_SQ=-1.0119779e-002;
	private static final double B4_ITALIAN=-1.6951274e+000;
	private static final double B4_NAT=+1.6224660e+000;
	private static final double B4_T2=-9.4143490e-002;
	private static final double B4_T3=+1.6053873e-001;
	private static final double B4_T4=+2.3104097e-001;
	private static final double B4_T5=-1.1564268e-003;
	private static final double B4_W_E_DIST=+4.4366911e-005;
	private static final double B5_AGE_18_29=-4.9844380e-001;
	private static final double B5_AGE_60=-1.2282040e-001;
	private static final double B5_AGE_SEX=+7.6453940e-003;
	private static final double B5_FRENCH=-6.0247683e-001;
	private static final double B5_GEN=+1.2354328e-001;
	private static final double B5_INC=+1.9354708e-001;
	private static final double B5_INC_LN=-2.0020572e-002;
	private static final double B5_INC_SQ=-1.7501693e-003;
	private static final double B5_ITALIAN=-9.2730729e-001;
	private static final double B5_NAT=+1.3774030e+000;
	private static final double B5_T2=+3.5586011e-001;
	private static final double B5_T3=+6.1518649e-001;
	private static final double B5_T4=+8.8257515e-001;
	private static final double B5_T5=+6.8708867e-001;
	private static final double B5_W_E_DIST=+4.5268558e-005;
	private static final double CONST1=-1.5685289e+000;
	private static final double CONST2=-9.5111691e-001;
	private static final double CONST3=-1.4512617e+000;
	private static final double CONST4=-2.3115941e+000;
	private static final double CONST5=-1.8134798e+000;

	
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
		MatsimRandom.getRandom().nextDouble();
	}

	//////////////////////////////////////////////////////////////////////
	// set methods
	//////////////////////////////////////////////////////////////////////

	public final boolean setAge(int age) {
		if (age < 0) { Gbl.errorMsg("age="+age+" not allowed."); }
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
		if (nump <= 0) { Gbl.errorMsg("nump="+nump+" not allowed."); }
		this.nump = nump;
		return true;
	}

	public final boolean setHHKids(int numk) {
		if (numk < 0) { Gbl.errorMsg("numk="+numk+" not allowed."); }
		this.numk = numk;
		return true;
	}

	public final boolean setIncome(double inc) {
		if (inc <= 0) { Gbl.errorMsg("inc="+inc+" not allowed."); }
		this.inc = inc;
		return true;
	}

	public final boolean setUrbanDegree(int udeg) {
		if ((udeg < 1) || (5 < udeg)) { Gbl.errorMsg("udeg="+udeg+" not allowed."); }
		this.udeg = udeg;
		return true;
	}

	public final boolean setLicenseOwnership(boolean license) {
		if (license) { this.license = 1.0; }
		else { this.license = 0.0; }
		return true;
	}

	public final boolean setDistanceHome2Work(double distance) {
		if (distance < 0.0) { Gbl.errorMsg("distance="+distance+" not allowed."); }
		this.disthw = distance;
		return true;
	}

	public final boolean setFuelCost(double fuelcost) {
		if (fuelcost <= 0.0) { Gbl.errorMsg("fuelcost="+fuelcost+" not allowed."); }
		this.fuelcost = fuelcost;
		return true;
	}
		
	public final boolean setLanguage(int language) {
		if ((language < 1) || (3 < language)) { Gbl.errorMsg("language="+language+" not allowed."); }
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
		double r = MatsimRandom.getRandom().nextDouble();
		double prob_sum = 0.0;
		for (int i=0; i<probs.length; i++) {
			prob_sum += probs[i];
			if (r < prob_sum) { return i; }
		}
		System.out.println("r="+r);
		System.out.println("age="+age);
		System.out.println("sex="+sex);
		System.out.println("nat="+nat);
		System.out.println("nump="+nump);
		System.out.println("numk="+numk);
		System.out.println("inc="+inc);
		System.out.println("udeg="+udeg);
		System.out.println("license="+license);
		System.out.println("disthw="+disthw);
		System.out.println("fuelcost="+fuelcost);
		System.out.println("language="+language);
		for (int i = 0; i<utils.length; i++) { System.out.println("utils["+i+"]="+utils[i]); }
		for (int i = 0; i<probs.length; i++) { System.out.println("probs["+i+"]="+probs[i]); }
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
		util += CONST1 * 1.0;
		util += B1_GEN * sex;
		util += B1_AGE_SEX * (age * sex);
		util += B1_NAT * nat;
		util += B1_W_E_DIST * disthw;
		if (age >=30 & age <60) { /*reference type*/}
		else if (age >= 18 & age <30) {util += B1_AGE_18_29 * 1.0; }
		else if (age >= 60) {util += B1_AGE_60 * 1.0; }
//		if (inc >=4 & inc < 8){ /* reference type */ }
//		else if (inc < 4 ){util += B1_INC_4 * 1.0; }
//		else if (inc >=8 & inc < 12) {util += B1_INC_8_12 * 1.0;}
//		else if (inc >= 12) {util += B1_INC_12 * 1.0;}
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
		util += CONST2 * 1.0;
		util += B2_GEN * sex;
		util += B2_AGE_SEX * (age * sex);
		util += B2_NAT * nat;
		util += B2_W_E_DIST * disthw;
		util += B2_INC * inc;
		util += B2_INC_SQ * (inc * inc);
		util += B2_INC_LN * Math.log(inc);
		if (age >=30 & age <60) { /*reference type*/}
		else if (age >= 18 & age <30) {util += B2_AGE_18_29 * 1.0; }
		else if (age >= 60) {util += B2_AGE_60 * 1.0; }
//		if (inc >=4 & inc < 8){ /* reference type */ }
//		else if (inc < 4 ){util += B2_INC_4 * 1.0; }
//		else if (inc >=8 & inc < 12) {util += B2_INC_8_12 * 1.0;}
//		else if (inc >= 12) {util += B2_INC_12 * 1.0;}
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
		util += CONST3 * 1.0;
		util += B3_GEN * sex;
		util += B3_AGE_SEX * (age * sex);
		util += B3_NAT * nat;
		util += B3_W_E_DIST * disthw;
		util += B3_INC * inc;
		util += B3_INC_SQ * (inc * inc);
		util += B3_INC_LN * Math.log(inc);
		if (age >=30 & age <60) { /*reference type*/}
		else if (age >= 6 & age <12) {util += B3_AGE_6_11 * 1.0; }
		else if (age >= 12 & age <18) {util += B3_AGE_12_17 * 1.0; }
		else if (age >= 18 & age <30) {util += B3_AGE_18_29 * 1.0; }
		else if (age >= 60) {util += B3_AGE_60 * 1.0; }
//		if (inc >=4 & inc < 8){ /* reference type */ }
//		else if (inc < 4 ){util += B3_INC_4 * 1.0; }
//		else if (inc >=8 & inc < 12) {util += B3_INC_8_12 * 1.0;}
//		else if (inc >= 12) {util += B3_INC_12 * 1.0;}
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
		util += CONST4 * 1.0;
		util += B4_GEN * sex;
		util += B4_AGE_SEX * (age * sex);
		util += B4_NAT * nat;
		util += B4_W_E_DIST * disthw;
		util += B4_INC * inc;
		util += B4_INC_SQ * (inc * inc);
		util += B4_INC_LN * Math.log(inc);
		if (age >=30 & age <60) { /*reference type*/}
		else if (age >= 18 & age <30) {util += B4_AGE_18_29 * 1.0; }
		else if (age >= 60) {util += B4_AGE_60 * 1.0; }
//		if (inc >=4 & inc < 8){ /* reference type */ }
//		else if (inc < 4 ){util += B4_INC_4 * 1.0; }
//		else if (inc >=8 & inc < 12) {util += B4_INC_8_12 * 1.0;}
//		else if (inc >= 12) {util += B4_INC_12 * 1.0;}
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
		util += CONST5 * 1.0;
		util += B5_GEN * sex;
		util += B5_AGE_SEX * (age * sex);
		util += B5_NAT * nat;
		util += B5_W_E_DIST * disthw;
		util += B5_INC * inc;
		util += B5_INC_SQ * (inc * inc);
		util += B5_INC_LN * Math.log(inc);
		if (age >=30 & age <60) { /*reference type*/}
		else if (age >= 18 & age <30) {util += B5_AGE_18_29 * 1.0; }
		else if (age >= 60) {util += B5_AGE_60 * 1.0; }
//		if (inc >=4 & inc < 8){ /* reference type */ }
//		else if (inc < 4 ){util += B5_INC_4 * 1.0; }
//		else if (inc >=8 & inc < 12) {util += B5_INC_8_12 * 1.0;}
//		else if (inc >= 12) {util += B5_INC_12 * 1.0;}
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
