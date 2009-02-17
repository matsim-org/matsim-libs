/* *********************************************************************** *
 * project: org.matsim.*
 * TransitRoute.java
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

package playground.marcel.pt.transitSchedule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.basic.v01.Id;
import org.matsim.facilities.Facility;
import org.matsim.population.routes.Route;

public class TransitRoute {

	private final Id id;
	private final Route route = null;
	private final List<Facility> stops = null; // TODO [MR] use new class "Stop" instead of Facility for addititonal data
	private String description = null;
	private final Map<Id, Departure> departures = new HashMap<Id, Departure>();

	public TransitRoute(final Id id) {
		this.id = id;
	}

	public Id getId() {
		return this.id;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public String getDescription() {
		return this.description;
	}

	public void addDeparture(final Id id, final Departure departure) {
		if (this.departures.containsKey(id)) {
			throw new IllegalArgumentException("There is already a departure with id " + id.toString());
		}
		this.departures.put(id, departure);

	}

}
