/* *********************************************************************** *
 * project: org.matsim.*
 * ModelModeChoice.java
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

import java.util.TreeSet;

import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;

public abstract class ModelModeChoice {
	
	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////
	
	protected double age; // 0-[unlimited]
	protected double nump; // number of persons of the household
	protected double udeg; // degree of urbanization [2-5] (1=urbanized=reference)
	protected double license; // yes = 1; no = 0;
	protected double dist_tour; // distance of the tour (in kilometers)
	protected double dist_w_e; // distance between home and work or education facility (in meter)
	protected double tickets; // holds some kind of season tickets 
	protected int purpose; // main purpose of the tour (Work = 1, Education = 2, Shop=3)
	protected String car; // av. of car (Always, Sometimes, Never)
	protected boolean bike; // bike ownership
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public ModelModeChoice() {
		this.age = -1.0;
		this.nump = -1.0;
		this.udeg = -1.0;
		this.license = -1.0;
		this.dist_tour = -1.0;
		this.dist_w_e = -1.0;
		this.car= null;
		this.tickets = -1.0;
		this.purpose = -1;
		this.bike = false;
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
	
	public final boolean setDistanceTour(double dist_tour) {
		if (dist_tour < 0.0) { return false; }
		this.dist_tour = dist_tour;
		return true;
	}
	
	public final boolean setDistanceHome2Work(double distance) {
		if (distance < 0.0) { return false; }
		this.dist_w_e = distance;
		return true;
	}
	
	public final boolean setMainPurpose(int purpose) {
		if (purpose < 0.0) { return false; }
		this.purpose = purpose;
		return true;
	}

	public final boolean setHHDimension(int nump) {
		if (nump <= 0) { return false; }
		this.nump = nump;
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

	public final boolean setCar(String car) {
		if (car == null) {return false; }
		this.car = car;
		return true;
	}
	
	public final boolean setTickets(TreeSet<String> tickets) {
		if (tickets.isEmpty()) { this.tickets = 0.0; }
		else { this.tickets = 1.0; }
		return true;
	}

	public final boolean setBike(boolean has_bike) {
		this.bike = has_bike;
		return true;
	}
	
	//////////////////////////////////////////////////////////////////////
	// calc methods
	//////////////////////////////////////////////////////////////////////

	/**
	 * Calculates the combination of car_avail and public transport ticket
	 * ownership.
	 * 
	 * @return values between 0 and 4.<BR>
	 * 
	 * the meanings of the values are:<BR>
	 * <code>0: Transportation mode chosen is Walk<br>
	 * <code>1: Transportation mode chosen is Bike<br>
	 * <code>2: Transportation mode chosen is Car<br>
	 * <code>3: Transportation mode chosen is Pt<br>
	 * <code>4: Transportation mode chosen is Other<br>
	 */
	public final int calcModeChoice() {
		double[] utils = new double[5];
		utils[0] = this.calcWalkUtil();
		utils[1] = this.calcBikeUtil();
		utils[2] = this.calcCarUtil();
		utils[3] = this.calcPublicUtil();		
		utils[4] = this.calcCarRideUtil();
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

	protected abstract double calcWalkUtil();
	
	protected abstract double calcBikeUtil();

	protected abstract double calcCarUtil();
	
	protected abstract double calcPublicUtil();
	
	protected abstract double calcCarRideUtil();

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	public final void print() {
		System.out.println("age=" + age +
		                   "nump=" + nump +
		                   "udeg=" + udeg +
		                   "license=" + license +
		                   "dist_tour=" + dist_tour +
		                   "dsit_w_e=" + dist_w_e +
		                   "tickets=" + tickets +
		                   "purpose=" + purpose +
		                   "car=" + car +
		                   "bike=" + bike);
	}
}


