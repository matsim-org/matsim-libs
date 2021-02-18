
/* *********************************************************************** *
 * project: org.matsim.*
 * TerminationCriterion.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.core.controler;

/**
 * This termination criterion defines whether a MATSim simulation run should
 * stop. A tricky aspect of this is that the criterion may decide whether to
 * stop the simulation based on information gather from the previous
 * iteration(s). However, if we decide that the iteration is "done" with this
 * criterion, it is already to late to write out, for instance, events for this
 * iteration, unless there were written out in any case. It is therefore
 * necessary to decode *before* an iteration if the iteration *may* be the last
 * iteration. In case criteria, e.g. for convergence, change, this interface
 * can, however, still decide that the iteration that was though to be the last
 * is in fact not the one. For that reason, there are two methods implemented.
 * 
 * @author Sebastian Hörl
 */
public interface TerminationCriterion {
	/**
	 * This method called by the controller to decide a priori whether the coming
	 * iteration may be the last. This will trigger writing of events, statistics,
	 * etc. if configured this way.
	 */
	boolean mayTerminateAfterIteration(int iteration);

	/**
	 * This method is called by the controller to decide whether an iteration that
	 * was a priori decided to be the last, will, in fact, be the last. In case some
	 * large deviation from previous iteration is detected in this iteration that
	 * had printed all the output, we may decide to still continue simuilation to
	 * find a better output state.
	 */
	boolean doTerminate(int iteration);
}
