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

package soc.ai.matsim.dbsim;

public class SimulationTimer {
	static private double simStartTime = 24*3600;
	static private double time = 0;
	static private double SIM_TICK_TIME_S = 1;

	/**
	 * @return Returns the time.
	 */
	public static final double getTime() {
		return time;
	}

	/**
	 * @param time The time to set.
	 */
	public static final void setTime(final double time) {
		SimulationTimer.time = time;
	}

	/**
	 * Increases the simulation time.
	 *
	 * @see #getSimTickTime()
	 */
	public static final void incTime() {
		time += SIM_TICK_TIME_S;
	}

	/**
	 * @return Returns the simStartTime. That is the lowest found start time of a leg
	 */
	public static final double getSimStartTime() {
		return simStartTime;
	}

	/**
	 * Returns the number of seconds (time steps) the simulation advances when increasing the simulation time.
	 *
	 * @return The number of time steps.
	 * @see #incTime()
	 */
	public static final double getSimTickTime() {
		// TODO [MR,DS] rename this to something like getSimTimestepSize?
		return SIM_TICK_TIME_S;
	}

	synchronized public static final void setSimStartTime(final double newStartTime) {
		SimulationTimer.simStartTime = newStartTime;
	}

	/** Resets the SimulationTimer for a new simulation. */
	public static final void reset() {
		reset(1);
	}

	/**
	 * Resets the SimulationTimer and sets the step-size (<code>stepSize</code>) for the simulation.
	 *
	 * @param stepSize The step-size the simulation uses to advance time.
	 */
	/*package*/ static final void reset(final double stepSize) {
		SimulationTimer.simStartTime = 24*3600;
		SimulationTimer.time = 0;
		SimulationTimer.SIM_TICK_TIME_S = stepSize;
	}

}
