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
package org.matsim.ptproject.qsim;


/**
 * @author dgrether
 */
public class QSimTimer {
	/**
	 * TODO 24 * 3600 is a quite strange time to initialize this
	 */
	private double simStartTime = 24*3600;
	private double time = 0.0;
	private double stepSize = 1.0;
	
	public QSimTimer(){
		this(1.0);
	}
	
	
	public QSimTimer(final double stepSize){
		this.simStartTime = 24*3600;
		this.time = 0;
		this.stepSize = stepSize;
	}
	
	
	/**
	 * @return Returns the simStartTime. That is the lowest found start time of a leg
	 */
	public final double getSimStartTime() {
		return this.simStartTime;
	}
	/**
	 * @return the time of day in seconds
	 */
	public double getTimeOfDay() {
		return this.time;
	}
	/**
	 * Increments the time by one timestep
	 * @return the new time in seconds
	 */
	public double incrementTime(){
		this.time += stepSize;
		return this.time;
	}
	
	/**
	 * Returns the number of seconds (time steps) the simulation advances when increasing the simulation time.
	 * @return The number of time steps.
	 */
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
