/* *********************************************************************** *
 * project: org.matsim.*
 * PlanStrategy.java
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

/**
 * 
 */
package playground.johannes.mobsim;

import org.matsim.core.population.PlanImpl;

/**
 * An IntradayStrategy encapsulates the behavioral model for
 * intraday-re-planning. The method {@link #replan(double)} returns a mutated
 * copy of the selected plan. The concrete modification is up to the specific
 * implementation. This can include en-route route-switching, departure time
 * choice, mode choice or even activity addition, removal or re-ordering.
 * However, modifications to plan entries the agent already performed or to
 * parts of the route the agent already passed are not allowed. Use
 * {@link PlanAgent#getCurrentPlanIndex()} and
 * {@link PlanAgent#getCurrentRouteIndex()} to determine the agent's current state.
 * 
 * 
 * @author illenberger
 * 
 */
public interface IntradayStrategy {

	/**
	 * Returns a new mutated copy of the selected plan. Modifications must keep
	 * the restrictions mentioned in the class description. Can return
	 * <tt>null</tt> if re-planning failed or is currently not allowed for any
	 * reason.
	 * 
	 * @param time
	 *            the current simulation time.
	 * @return a new plan, or <tt>null</tt> if re-planning failed or is
	 *         currently not allowed.
	 */
	public PlanImpl replan(double time);
	
}
