/* *********************************************************************** *
 * project: org.matsim.*
 * TransitAgent.java
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

package playground.marcel.pt.integration;

import org.matsim.core.api.facilities.Facility;
import org.matsim.core.api.population.Person;
import org.matsim.core.mobsim.queuesim.PersonAgent;
import org.matsim.core.mobsim.queuesim.QueueSimulation;

import playground.marcel.pt.interfaces.PassengerAgent;

public class TransitAgent extends PersonAgent implements PassengerAgent {

	private boolean isFirstQuery = true;

	public TransitAgent(final Person p, final QueueSimulation simulation) {
		super(p, simulation);
	}

	public boolean arriveAtStop(final Facility stop) {
		return this.getDestinationLink() == stop.getLink(); // TODO [MR] not yet perfect...
	}

	public boolean ptLineAvailable() {
		if (this.isFirstQuery) {
			this.isFirstQuery = false;
			this.activityEnds(7*3600);
			return true;
		}
		return false;
	}

}
