/* *********************************************************************** *
 * project: org.matsim.*
 * DepartureTimeDistribution.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package org.matsim.contrib.evacuation.io;


public class DepartureTimeDistribution {

	public final static String NORMAL = "normal";
	public final static String LOG_NORMAL = "log-normal";
	public final static String DIRAC_DELTA = "dirac-delta";
	
	private String distribution;
	private double sigma;
	private double mu;
	private double earliest;
	private double latest;

	public void setDistribution(String content) {
		this.distribution = content;
	}

	public void setSigma(double parseDouble) {
		this.sigma = parseDouble;
	}

	public void setMu(double parseDouble) {
		this.mu = parseDouble;
	}

	public void setEarliest(double parseDouble) {
		this.earliest = parseDouble;
		
	}

	public void setLatest(double parseDouble) {
		this.latest = parseDouble;
		
	}

	public double getEarliest() {
		return this.earliest;
	}
	
	public double getLatest(){
		return this.latest;
	}
	
	public double getSigma(){
		return this.sigma;
	}
	
	public double getMu() {
		return this.mu;
	}

	public String getDistribution() {
		return this.distribution;
	}
	

}
