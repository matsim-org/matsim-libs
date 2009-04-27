/* *********************************************************************** *
 * project: org.matsim.*
 * DriverAgent.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

import org.matsim.core.api.network.Link;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Person;

public interface DriverAgent {

	/**
	 * The time the agent wants to depart from an Activity. If the agent is currently driving,
	 * the return value cannot be interpreted (e.g. it is not defined if it is the departure time
	 * from the previous activity, or from the next one).
	 *
	 * @return the time when the agent wants to depart from an activity.
	 */
	public double getDepartureTime();
	/* there is no corresponding setter, as the implementation should set the the corresponding time
	 * internally, e.g. in legEnds().
	 */
	
	public Link getDestinationLink();
	
	/**
	 * Returns the next link the vehicle will drive along.
	 *
	 * @return The next link the vehicle will drive on, or null if an error has happened.
	 */
	public Link chooseNextLink();

	/**
	 * Informs the agent that it arrived at the destination of the current leg.
	 * The agent can then decide if he wants to start an activity, or continue
	 * on another leg.
	 *
	 * @param now the current time in the simulation
	 */
	public void legEnds(final double now);
	
	/**
	 * Informs the agent that the currently executed activity is ended / is
	 * no longer performed.
	 * 
	 * @param now
	 */
	public void activityEnds(final double now);
	
	public void teleportToLink(final Link link);

	// the methods below are yet unclear how useful they are in the interface, or if they should be moved to a Vehicle interface.

	public void moveOverNode();

	public Leg getCurrentLeg();

	public Person getPerson();

}
