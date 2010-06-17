/* *********************************************************************** *
 * project: org.matsim.*
 * SimulationTimer.java
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

package org.matsim.core.mobsim.queuesim.obsolete;

import org.matsim.ptproject.qsim.interfaces.SimTimerI;

public class SimulationTimer implements SimTimerI {
	private double simStartTime = 24*3600;
	private double time = 0;
	private double SIM_TICK_TIME_S = 1;

//	/**
//	 * @return Returns the time.
//	 */
//	private static final double getTimeOfDayStatic() {
//		return time;
//	}
//
//	/**
//	 * @param time The time to set.
//	 */
//	private static final void setTimeStatic(final double time) {
//		SimulationTimer.time = time;
//	}
//
//	/**
//	 * Increases the simulation time.
//	 *
//	 * @see #getSimTimestepSizeStatic()
//	 */
//	private static final void incrementTimeStatic() {
//		time += SIM_TICK_TIME_S;
//	}
//
//	/**
//	 * @return Returns the simStartTime. That is the lowest found start time of a leg
//	 */
//	private static final double getSimStartTimeStatic() {
//		return simStartTime;
//	}
//
//	/**
//	 * Returns the number of seconds (time steps) the simulation advances when increasing the simulation time.
//	 *
//	 * @return The number of time steps.
//	 * @see #incrementTimeStatic()
//	 */
//	private static final double getSimTimestepSizeStatic() {
//		return SIM_TICK_TIME_S;
//	}
//
//	private synchronized /*package*/ static final void setSimStartTimeStatic(final double newStartTime) {
//		SimulationTimer.simStartTime = newStartTime;
//	}
//
//	/** Resets the SimulationTimer for a new simulation. */
//	private static final void resetStatic() {
//		resetStatic(1);
//	}

//	/**
//	 * Resets the SimulationTimer and sets the step-size (<code>stepSize</code>) for the simulation.
//	 *
//	 * @param stepSize The step-size the simulation uses to advance time.
//	 */
//	public static final void resetStatic(final double stepSize) {
//		SimulationTimer.simStartTime = 24*3600;
//		SimulationTimer.time = 0;
//		SimulationTimer.SIM_TICK_TIME_S = stepSize;
//	}

	/*package*/ SimulationTimer( double stepSize ) {
		simStartTime = 24*3600;
		time = 0;
		SIM_TICK_TIME_S = stepSize;
	}

	@Override
	public double getSimStartTime() {
		return simStartTime ;
	}

	@Override
	public double getSimTimestepSize() {
		return SIM_TICK_TIME_S ;
	}

	@Override
	public double getTimeOfDay() {
		return time ;
	}

	@Override
	public double incrementTime() {
		return time += SIM_TICK_TIME_S ;
	}

	@Override
	public void setSimStartTime(double startTimeSec) {
		simStartTime = startTimeSec ;
	}

	@Override
	public void setTime(double timeSec) {
		time = timeSec ;
	}

}
