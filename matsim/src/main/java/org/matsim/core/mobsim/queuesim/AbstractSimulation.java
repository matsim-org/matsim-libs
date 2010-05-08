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

package org.matsim.core.mobsim.queuesim;


/*package*/ abstract class AbstractSimulation {

	/**
	 * Number of agents that have not yet reached their final activity location
	 */
	private static int living = 0;

	/**
	 * Number of agents that got stuck in a traffic jam and where removed from the simulation to solve a possible deadlock
	 */
	private static int lost = 0;

	private static double stuckTime = Double.MAX_VALUE;

	/*package*/ static void reset(final double stuckTimeTmp) {
		setLiving(0);
		resetLost();
		setStuckTime(stuckTimeTmp);
	}

	/*package*/ static final double getStuckTime() {return stuckTime;	}
	private static final void setStuckTime(final double stuckTime) { AbstractSimulation.stuckTime = stuckTime; }

	/*package*/ static final int getLiving() {return living;	}
	/*package*/ static final void setLiving(final int count) {living = count;}
	/*package*/ static final boolean isLiving() {return living > 0;	}
	/*package*/ static final int getLost() {return lost;	}
	/*package*/ static final void incLost() {lost++;}
	/*package*/ static final void incLost(final int count) {lost += count;}
	/*package*/ static final void resetLost() { lost = 0; }

	/*package*/ static final void incLiving() {living++;}
	/*package*/ static final void incLiving(final int count) {living += count;}
	/*package*/ static final void decLiving() {living--;}
	/*package*/ static final void decLiving(final int count) {living -= count;}
}
