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

package org.matsim.ptproject.qsim;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class Simulation {

	/*
	 * @cdobler
	 * Use AtomicIntegers as counting variables. Doing this avoids
	 * problems with race conditions in the ParallelQueueSimulation.
	 * 
	 * TODO
	 * We should discuss if there is a possibility to assign a 
	 * Simulation Objects to each thread. Using the AtomicIntegers
	 * solves the problems with the race conditions but it is still
	 * a bottleneck...
	 */
	
	/**
	 * Number of agents that have not yet reached their final activity location
	 */
	private static AtomicInteger living = new AtomicInteger(0);

	/**
	 * Number of agents that got stuck in a traffic jam and where removed from the simulation to solve a possible deadlock
	 */
	private static AtomicInteger lost = new AtomicInteger(0);
	
	private static double stuckTime = Double.MAX_VALUE;

	public static void reset(double stucktime) {
		setLiving(0);
		resetLost();
		setStuckTime(stucktime);
	}

	public static final double getStuckTime() {return stuckTime;	}
	private static final void setStuckTime(final double stuckTime) { Simulation.stuckTime = stuckTime; }

	public static final int getLiving() {return living.get();	}
	public static final void setLiving(final int count) {living.set(count);}
	public static final boolean isLiving() {return living.get() > 0;	}
	public static final int getLost() {return lost.get();	}
	public static final void incLost() {lost.incrementAndGet(); }
	public static final void incLost(final int count) {lost.addAndGet(count);}
	private static final void resetLost() { lost.set(0); }

	public static final void incLiving() {living.incrementAndGet();}
	public static final void incLiving(final int count) {living.addAndGet(count);}
	public static final void decLiving() {living.decrementAndGet();}
	public static final void decLiving(final int count) {living.decrementAndGet();}
	
}
