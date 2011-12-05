/* *********************************************************************** *
 * project: org.matsim.*
 * PlanAcceptor.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.agentsmating.logitbasedmating.framework;

import org.matsim.api.core.v01.population.Plan;

/**
 * Accepts or not a plan for performing choice.
 * The {@link PlatformBasedModeChooser} uses an instance which filters one-activity
 * plans, but this can be used to exclude other kind of agents, as freight drivers.
 *
 * Implementing classes should implement equals, so that two instances giving the same
 * answer to the same requests are considered equal.
 *
 * @author thibautd
 */
public interface PlanAcceptor {
	/**
	 * @return true if this acceptor allows the mode choice to be performed
	 * on this plan.
	 */
	public boolean accept(Plan plan);
}

