/* *********************************************************************** *
 * project: org.matsim.*																															*
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

package org.matsim.core.mobsim.qsim.pt;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.Umlauf;
import org.matsim.pt.transitSchedule.api.*;

class UmlaufCache {
	public static final String ELEMENT_NAME = "umlaufCache";

	private final TransitSchedule transitSchedule;
	private final Collection<Umlauf> umlaeufe;
	private final Set<Id<Departure>> departuresDependingOnChains = new LinkedHashSet<>();

	public UmlaufCache(TransitSchedule transitSchedule, Collection<Umlauf> umlaeufe) {
		this.transitSchedule = transitSchedule;
		this.umlaeufe = umlaeufe;

		for (TransitLine line : transitSchedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (Departure departure : route.getDepartures().values()) {
					for (ChainedDeparture c : departure.getChainedDepartures()) {
						this.departuresDependingOnChains.add(c.getChainedDepartureId());
					}
				}
			}
		}
	}

	public TransitSchedule getTransitSchedule() {
		return this.transitSchedule;
	}

	public Collection<Umlauf> getUmlaeufe() {
		return this.umlaeufe;
	}

	public Set<Id<Departure>> getDeparturesDependingOnChains() {
		return departuresDependingOnChains;
	}
}
