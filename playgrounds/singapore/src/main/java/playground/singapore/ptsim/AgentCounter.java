/* *********************************************************************** *
 * project: org.matsim.*
 * AgentCounter
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
package playground.singapore.ptsim;

import java.util.concurrent.atomic.AtomicInteger;


/**
 * This class replaces static functionality of abstract class Simulation.
 * It is responsible for living/lost agent counting.
 *
 * This class is a simple not static replacement of the old static Simulation class.
 * When the agent representation of QSim is implemented we should
 * consider to remove this class completely as it should then no longer be needed. dg May 2010
 *
 * OLD COMMENTS from simulation class
 *
 * 	 * @cdobler
 * Use AtomicIntegers as counting variables. Doing this avoids
 * problems with race conditions in the ParallelQueueSimulation.
 *
 * We should discuss if there is a possibility to assign a
 * Simulation Objects to each thread. Using the AtomicIntegers
 * solves the problems with the race conditions but it is still
 * a bottleneck...
 *
 * --
 * My plan once was to have one SimEngine for each thread, instead of one Simulation per thread. marcel/15feb2010
 *
 *
 * @author dgrether
 *
 */
class AgentCounter implements org.matsim.core.mobsim.qsim.interfaces.AgentCounter {
	/**
	 * Number of agents that have not yet reached their final activity location
	 */
	private AtomicInteger living = new AtomicInteger(0);

	/**
	 * Number of agents that got stuck in a traffic jam and where removed from the simulation to solve a possible deadlock
	 */
	private AtomicInteger lost = new AtomicInteger(0);

	 void reset() {
		setLiving(0);
		resetLost();
	}

	@Override
	public final int getLiving() {return living.get();	}

	 final void setLiving(final int count) {living.set(count);}
	@Override
	public final boolean isLiving() {return living.get() > 0;	}
	@Override
	public final int getLost() {return lost.get();	}
	@Override
	public final void incLost() {lost.incrementAndGet(); }

	 final void incLost(final int count) {lost.addAndGet(count);}
	private final void resetLost() { lost.set(0); }

	 final void incLiving() {living.incrementAndGet();}

	 final void incLiving(final int count) {living.addAndGet(count);}
	@Override
	public final void decLiving() {living.decrementAndGet();}
//	@Override
//	public final void decLiving(final int count) {living.decrementAndGet();}
//	// yyyyyy does this work (`count' never used)??? kai, feb'12

}
