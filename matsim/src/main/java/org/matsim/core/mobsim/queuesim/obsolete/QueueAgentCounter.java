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

package org.matsim.core.mobsim.queuesim.obsolete;

import org.matsim.ptproject.qsim.interfaces.AgentCounterI;


/*package*/ class QueueAgentCounter implements AgentCounterI {

	/**
	 * Number of agents that have not yet reached their final activity location
	 */
	private static int living = 0;

	/**
	 * Number of agents that got stuck in a traffic jam and where removed from the simulation to solve a possible deadlock
	 */
	private static int lost = 0;

//	private static double stuckTime = Double.MAX_VALUE;
//
//	/*package*/ private static void reset(final double stuckTimeTmp) {
//		setLiving(0);
//		resetLost();
//		setStuckTime(stuckTimeTmp);
//	}
//
//	/*package*/ private static final double getStuckTime() {return stuckTime;	}
//	private static final void setStuckTime(final double stuckTime) { AbstractSimulation.stuckTime = stuckTime; }

	private static void staticReset() {
		staticSetLiving(0);
		staticResetLost();
	}

	private static final int staticGetLiving() {return living;	}
	private static final void staticSetLiving(final int count) {living = count;}
	private static final boolean staticIsLiving() {return living > 0;	}
	private static final int staticGetLost() {return lost;	}
	private static final void staticIncLost() {lost++;}
	private static final void staticIncLost(final int count) {lost += count;}
	private static final void staticResetLost() { lost = 0; }

	private static final void staticIncLiving() {living++;}
	private static final void staticIncLiving(final int count) {living += count;}
	private static final void staticDecLiving() {living--;}
	private static final void staticDecLiving(final int count) {living -= count;}

	/* (non-Javadoc)
	 * @see org.matsim.ptproject.qsim.AgentCounterI#decLiving()
	 */
	@Override
	public void decLiving() {
		QueueAgentCounter.staticDecLiving() ;
	}

	/* (non-Javadoc)
	 * @see org.matsim.ptproject.qsim.AgentCounterI#decLiving(int)
	 */
	@Override
	public void decLiving(int count) {
		QueueAgentCounter.staticDecLiving(count) ;
	}

	/* (non-Javadoc)
	 * @see org.matsim.ptproject.qsim.AgentCounterI#getLiving()
	 */
	@Override
	public int getLiving() {
		return QueueAgentCounter.staticGetLiving() ;
	}

	/* (non-Javadoc)
	 * @see org.matsim.ptproject.qsim.AgentCounterI#getLost()
	 */
	@Override
	public int getLost() {
		return QueueAgentCounter.staticGetLost() ;
	}

	/* (non-Javadoc)
	 * @see org.matsim.ptproject.qsim.AgentCounterI#incLiving()
	 */
	@Override
	public void incLiving() {
		QueueAgentCounter.staticIncLiving();
	}

	/* (non-Javadoc)
	 * @see org.matsim.ptproject.qsim.AgentCounterI#incLiving(int)
	 */
	@Override
	public void incLiving(int count) {
		QueueAgentCounter.staticIncLiving(count) ;
	}

	/* (non-Javadoc)
	 * @see org.matsim.ptproject.qsim.AgentCounterI#incLost()
	 */
	@Override
	public void incLost() {
		QueueAgentCounter.staticIncLost();
	}

	/* (non-Javadoc)
	 * @see org.matsim.ptproject.qsim.AgentCounterI#incLost(int)
	 */
	@Override
	public void incLost(int count) {
		QueueAgentCounter.staticIncLost(count) ;
	}

	/* (non-Javadoc)
	 * @see org.matsim.ptproject.qsim.AgentCounterI#isLiving()
	 */
	@Override
	public boolean isLiving() {
		return QueueAgentCounter.staticIsLiving();
	}

	/* (non-Javadoc)
	 * @see org.matsim.ptproject.qsim.AgentCounterI#reset()
	 */
	@Override
	public void reset() {
		QueueAgentCounter.staticReset() ;
	}

	/* (non-Javadoc)
	 * @see org.matsim.ptproject.qsim.AgentCounterI#setLiving(int)
	 */
	@Override
	public void setLiving(int count) {
		QueueAgentCounter.staticSetLiving(count) ;
	}
}
