/* *********************************************************************** *
 * project: org.matsim.*
 * ModelLicenseOwnership.java
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


public class ModelLicenseOwnershipV3 {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private static final double B_AGE_18_30 = +1.5124103e+000;
	private static final double B_AGE_60 = +8.0207344e-001;
	private static final double B_AGE_SEX = +2.9533555e-002;
	private static final double B_AGE_LN = +1.6506580e+001;
	private static final double B_CONST_NO = +4.4782306e+001;
	private static final double B_HH_DIM = +1.1294759e-001;
	private static final double B_HH_KIDS = -1.4713021e-003;
	private static final double B_INC = -1.2868986e-001;
	private static final double B_MUN_TYPE2 = -3.9893725e-001;
	private static final double B_MUN_TYPE3 = -4.5456051e-001;
	private static final double B_MUN_TYPE4 = -6.3835678e-001;
	private static final double B_MUN_TYPE5 = -7.5914814e-001;
	private static final double B_NAT = +7.7974576e-001;
	private static final double B_GENDER = -2.4184702e-001;
	

	private double age; // // 0-[unlimited]
	private double sex; // male = 1; female = 0
	private double nat; // ch = 1; other = 0
	private double nump; // number of persons of the household
	private double numk; // number of kids of the household
	private double inc; // monthly income of the household (in 1000 SFr)
	private double udeg; // degree of urbanization [2-5] (1=urbanized=reference)

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public ModelLicenseOwnershipV3() {
		this.age = -1.0;
		this.sex = -1.0;
		this.nat = -1.0;
		this.nump = -1.0;
		this.numk = -1.0;
		this.inc = -1.0;
		this.udeg = -1.0;
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

	//////////////////////////////////////////////////////////////////////
	// calc methods
	//////////////////////////////////////////////////////////////////////

	public final boolean calcLicenseOwnership() {
		double[] utilities = new double[2];
		utilities[0] = this.calcYesUtility();
		utilities[1] = this.calcNoUtility();
		double license_prob = this.calcLogitProbability(utilities[0],utilities);
		double r = MatsimRandom.getRandom().nextDouble();
		if (r < license_prob) { return true; }
		else { return false; }
	}
	
	// this actually belongs to somewhere else...
	private final double calcLogitProbability(double referenceUtility, double[] utilities) {
		double expSumOfAlternatives = 0;
		for (double utility : utilities) { expSumOfAlternatives += Math.exp(utility); }
		return Math.exp(referenceUtility) / expSumOfAlternatives;
	}

	private final double calcYesUtility() {
		// B_Age * AGE + B_Age_ln * AGE_LN + B_Age_Sex * AGE_SEX + B_Nat * NATIONALITY
		double yes_util = 0.0;
		if (age >=18 && age < 30 ) {
			yes_util += B_AGE_18_30;
		}
		if (age >=60) {
			yes_util += B_AGE_60;
		}
		yes_util += B_AGE_LN * Math.log(age);
		yes_util += B_AGE_SEX * (age * sex);
		yes_util += B_NAT * nat;
		yes_util += B_GENDER * sex;
		return yes_util;
	}

	private final double calcNoUtility() {
		// B_CONST_No * one + B_HH_Dim * HH_DIM + B_HH_Kids * HH_KIDS + B_Inc * INCOME_1000 + B_Mun_type2 * MUN_T2 + 
		// B_Mun_type3 * MUN_T3 + B_Mun_type4 * MUN_T4 + B_Mun_type5 * MUN_T5
		double no_util = 0.0;
		no_util += B_CONST_NO * 1.0;
		no_util += B_HH_DIM * nump;
		no_util += B_HH_KIDS * numk;
		no_util += B_INC * inc;
		if (udeg == 1) { /* reference type */ }
		else if (udeg == 2) { no_util += B_MUN_TYPE2 * 1.0; }
		else if (udeg == 3) { no_util += B_MUN_TYPE3 * 1.0; }
		else if (udeg == 4) { no_util += B_MUN_TYPE4 * 1.0; }
		else if (udeg == 5) { no_util += B_MUN_TYPE5 * 1.0; }
		else { Gbl.errorMsg("This should never happen!"); }
		return no_util;
	}
}
