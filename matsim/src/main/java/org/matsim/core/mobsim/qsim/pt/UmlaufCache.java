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

import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.matsim.api.core.v01.Id;
import org.matsim.pt.Umlauf;
import org.matsim.pt.transitSchedule.api.*;

import java.util.Collection;

class UmlaufCache {
	public static final String ELEMENT_NAME = "umlaufCache";

	private final TransitSchedule transitSchedule;
	private final Collection<Umlauf> umlaeufe;
	private final Object2IntMap<Id<Departure>> departuresDependingOnChains;

	static Object2IntMap<Id<Departure>> calculateDeparturesDependingOnChains(TransitSchedule transitSchedule) {
		Object2IntMap<Id<Departure>> result = new Object2IntLinkedOpenHashMap<>();
		for (TransitLine line : transitSchedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (Departure departure : route.getDepartures().values()) {
					for (ChainedDeparture c : departure.getChainedDepartures()) {
						result.mergeInt(c.getChainedDepartureId(), 1, Integer::sum);
					}
				}
			}
		}

		return result;
	}

	public UmlaufCache(TransitSchedule transitSchedule, Collection<Umlauf> umlaeufe) {
		this.transitSchedule = transitSchedule;
		this.umlaeufe = umlaeufe;
		this.departuresDependingOnChains = calculateDeparturesDependingOnChains(transitSchedule);
	}

	public TransitSchedule getTransitSchedule() {
		return this.transitSchedule;
	}

	public Collection<Umlauf> getUmlaeufe() {
		return this.umlaeufe;
	}

	public Object2IntMap<Id<Departure>> getDeparturesDependingOnChains() {
		return departuresDependingOnChains;
	}
}
