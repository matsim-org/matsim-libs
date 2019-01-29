/* *********************************************************************** *
 * project: org.matsim.*
 * QSimTimer
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package org.matsim.core.mobsim.framework;



/**
 * @author dgrether
 */
public class MobsimTimer {
	private double simStartTime = 24*3600; // initialized to 24h so as that the time can be successively
	           // set down when an agent starts earlier until the earliest point of time an agent starts
	private double time = 0.0;
	private double stepSize = 1.0;

	public MobsimTimer(){
		this(1.0);
	}


	public MobsimTimer(final double stepSize){
		this.simStartTime = 24*3600;
		this.time = 0;
		this.stepSize = stepSize;
	}

	public final double getSimStartTime() {
		return this.simStartTime;
	}
	
	public double getTimeOfDay() {
		return this.time;
	}

	public double incrementTime(){
		this.time += stepSize;
		return this.time;
	}

	public final double getSimTimestepSize() {
		return this.stepSize;
	}

	public void setSimStartTime(double startTimeSec) {
		this.simStartTime = startTimeSec;
	}

	public void setTime(double timeSec) {
		this.time = timeSec;
	}

}
