/* *********************************************************************** *
 * project: org.matsim.*
 * TransitLine.java
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
import java.util.Map;

import org.matsim.basic.v01.Id;

public class TransitLine {

	private final Id id;
	private final Map<Id, TransitRoute> transitRoutes = new HashMap<Id, TransitRoute>();
	
	public TransitLine(final Id id) {
		this.id = id;
	}

	public Id getId() {
		return id;
	}

	public void addRoute(final Id id, final TransitRoute transitRoute) {
		if (this.transitRoutes.containsKey(id)) {
			throw new IllegalArgumentException("There is already a transit route with id " + id.toString());
		}
		this.transitRoutes.put(id, transitRoute);
	}

	
}
