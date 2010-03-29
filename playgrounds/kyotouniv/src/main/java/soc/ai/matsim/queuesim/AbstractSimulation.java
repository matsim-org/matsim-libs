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

package soc.ai.matsim.queuesim;

import org.matsim.core.gbl.Gbl;

public abstract class AbstractSimulation {

	/**
	 * Number of agents that have not yet reached their final activity location
	 */
	private static int living = 0;

	/**
	 * Number of agents that got stuck in a traffic jam and where removed from the simulation to solve a possible deadlock
	 */
	private static int lost = 0;

	private static double stuckTime = Double.MAX_VALUE;

	public static void reset() {
		setLiving(0);
		resetLost();
		setStuckTime(Gbl.getConfig().simulation().getStuckTime());
	}

	public static final double getStuckTime() {return stuckTime;	}
	private static final void setStuckTime(final double stuckTime) { AbstractSimulation.stuckTime = stuckTime; }

	public static final int getLiving() {return living;	}
	public static final void setLiving(final int count) {living = count;}
	public static final boolean isLiving() {return living > 0;	}
	public static final int getLost() {return lost;	}
	public static final void incLost() {lost++;}
	public static final void incLost(final int count) {lost += count;}
	private static final void resetLost() { lost = 0; }

	public static final void incLiving() {living++;}
	public static final void incLiving(final int count) {living += count;}
	public static final void decLiving() {living--;}
	public static final void decLiving(final int count) {living -= count;}
}
