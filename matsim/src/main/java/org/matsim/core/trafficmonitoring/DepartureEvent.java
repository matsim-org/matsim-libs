/* *********************************************************************** *
 * project: org.matsim.*
 * DepartureEvent.java
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

package org.matsim.core.trafficmonitoring;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

/*package*/ class DepartureEvent {

	private final Id<Person> agentId;

	protected DepartureEvent(final Id<Person> agentId) {
		this.agentId = agentId;
	}

	@Override
	public boolean equals(final Object arg0) {
		if (!(arg0 instanceof DepartureEvent)) return false;

		DepartureEvent event = (DepartureEvent)arg0;
		return (this.agentId.equals(event.agentId));
	}

	@Override
	public String toString() {
		return "[[agentId = " + this.agentId + "]";
	}

	@Override
	public int hashCode() {
		return this.agentId.hashCode();
	}

}
