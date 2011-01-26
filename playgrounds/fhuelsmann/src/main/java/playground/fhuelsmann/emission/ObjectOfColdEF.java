package playground.fhuelsmann.emission;
/* *********************************************************************** *
 * project: org.matsim.*
 * FhMain.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

public class ObjectOfColdEF {

	
	private String parktinTime;
	private String component;
	private double distance;
	private double coldEf;
	public ObjectOfColdEF(String parktinTime, String component,
			double distance, double coldEf) {
		super();
		this.parktinTime = parktinTime;
		this.component = component;
		this.distance = distance;
		this.coldEf = coldEf;
	}
	public ObjectOfColdEF() {
		// TODO Auto-generated constructor stub
	}
	public String getParktinTime() {
		return parktinTime;
	}
	public void setParktinTime(String parktinTime) {
		this.parktinTime = parktinTime;
	}
	public String getComponent() {
		return component;
	}
	public void setComponent(String component) {
		this.component = component;
	}
	public double getDistance() {
		return distance;
	}
	public void setDistance(double distance) {
		this.distance = distance;
	}
	public double getColdEf() {
		return coldEf;
	}
	public void setColdEf(double coldEf) {
		this.coldEf = coldEf;
	}
	
}
