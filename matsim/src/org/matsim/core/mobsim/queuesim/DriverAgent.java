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

import java.util.List;

import org.matsim.api.basic.v01.population.BasicPlanElement;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Person;

public interface DriverAgent {

	public void setVehicle(final QueueVehicle veh);

	public void setCurrentLink(final Link link);

	public Link getCurrentLink();

	public Link getDestinationLink();

	public void incCurrentNode();

	/**
	 * Returns the next link the vehicle will drive along.
	 *
	 * @return The next link the vehicle will drive on, or null if an error has happened.
	 */
	public Link chooseNextLink();

	// those below shouldn't be part of the interface in my opinion...
	
	public Person getPerson();

	public void reachActivity(final double now, final QueueLink currentQueueLink);
	
	public void leaveActivity(final double now);

	public Leg getCurrentLeg();
	
	public int getCurrentNodeIndex();
	
	public int getNextActivity();

	public List<? extends BasicPlanElement> getActsLegs();
}
