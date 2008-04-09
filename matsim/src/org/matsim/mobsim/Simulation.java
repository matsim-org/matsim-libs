/* *********************************************************************** *
 * project: org.matsim.*
 * Simulation.java
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

package org.matsim.mobsim;

import java.util.Date;

import org.matsim.gbl.Gbl;

// TODO [DS] this is completely wrong: move more functionality into QueueSimulator
// leaving only most rudimentary functionality in Simulator itself
public abstract class Simulation {

	protected Date starttime = new Date();

	private static int living = 0;
	private static int lost = 0;
	private static int stuckTime = Integer.MAX_VALUE;
	protected double stopTime = 100*3600;

	public Simulation() {
		setLiving(0);
		resetLost();
		setStuckTime((int)Gbl.getConfig().simulation().getStuckTime());//TODO [DS] change time to double
	}

	protected abstract void prepareSim();

	protected abstract void cleanupSim();

	public abstract void beforeSimStep(final double time);

	public abstract boolean doSimStep(final double time);

	public abstract void afterSimStep(final double time);

	//////////////////////////////////////////////////////////////////////
	// only the very basic simulation scheme here
	// overload prepare/cleanup and doSim step
	//////////////////////////////////////////////////////////////////////
	public final void run()
	{
		prepareSim();
		//do iterations
		boolean cont = true;
		while (cont) {
			double time = SimulationTimer.getTime();
			beforeSimStep(time);
			cont = doSimStep(time);
			afterSimStep(time);
			if (cont) {
				SimulationTimer.incTime();
			}
		}
		cleanupSim();
	}

	//////////////////////////////////////////////////////////////////////
	// some getter / setter functions
	//////////////////////////////////////////////////////////////////////
	public static final int getStuckTime() {return stuckTime;	}
	private static final void setStuckTime(final int stuckTime) { Simulation.stuckTime = stuckTime; }

	public static final int getLiving() {return living;	}
	public static final void setLiving(final int count) {living = count;}
	public static final boolean isLiving() {return living > 0;	}
	public static final int getLost() {return lost;	}
	public static final void incLost() {lost++;}
	public static final void incLost(final int count) {lost += count;}
	private static final void resetLost() { lost = 0; }

	// Why is incLiving() synchronized, but not decLiving()?? / mrieser, 07sep2007
	synchronized public static final void incLiving() {living++;}
	synchronized public static final void incLiving(final int count) {living += count;}
	public static final void decLiving() {living--;}
	public static final void decLiving(final int count) {living -= count;}
}
