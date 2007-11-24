/* *********************************************************************** *
 * project: org.matsim.*
 * DisbenefitValue.java
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

package org.matsim.withinday.trafficmanagement.container;



public class DisbenefitValue {
	
	double value;
	private int simTime;
	
	
	public DisbenefitValue(NashTime nashTime, ControlVariable controlSignal, 
			double nomSplitting, double complianceRate, double flow, int sim_time){
		if(nashTime.getValue()<0)
			this.value = flow*Math.abs(nashTime.getValue()*
					(complianceRate*(1-controlSignal.getValue())) + (1-complianceRate)*(1-nomSplitting));
		else {
			this.value = flow*Math.abs(nashTime.getValue()*
					(complianceRate*controlSignal.getValue())) + (1-complianceRate)*nomSplitting;
		}
		this.simTime=sim_time;
	}
	
	@Override
	public String toString(){
		String s = Double.toString(value);
		return s;
	}
	
	public int getSimTime(){
		return simTime;
	}

	public double getValue() {
		return value;
	}

	
	
}
