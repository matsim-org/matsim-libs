/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package playground.polettif.boescpa.converters.osm.scheduleCreator.hafasCreator;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;

import java.util.*;

/**
 * A public transport line as read out from HAFAS FPLAN.
 *
 * @author boescpa
 */
public class PtLineFPLAN {
	private static TransitScheduleFactory scheduleBuilder = new TransitScheduleFactoryImpl();

	private final Id<TransitLine> lineId;
	private final List<PtRouteFPLAN> routesFPLAN = new ArrayList<>();
	private final Set<Id<TransitRoute>> idRoutesFPLAN = new HashSet<>();

	public List<PtRouteFPLAN> getRoutesFPLAN() {
		return Collections.unmodifiableList(routesFPLAN);
	}

	public Set<Id<TransitRoute>> getIdRoutesFPLAN() {
		return Collections.unmodifiableSet(idRoutesFPLAN);
	}

	public PtLineFPLAN(Id<TransitLine> lineId) {
		this.lineId = Id.create(lineId.toString(), TransitLine.class);
	}

	public TransitLine createLine() {
		TransitLine line = scheduleBuilder.createTransitLine(lineId);
		for (PtRouteFPLAN route : this.routesFPLAN) {
			TransitRoute transitRoute = route.getRoute();
			if (transitRoute != null) {
				line.addRoute(transitRoute);
			}
		}
		if (!line.getRoutes().isEmpty()) {
			return line;
		} else {
			return null;
		}
	}

	public void addPtRouteFPLAN(PtRouteFPLAN route) {
		routesFPLAN.add(route);
		idRoutesFPLAN.add(route.getRouteId());
	}

	public boolean removePtRouteFPLAN(PtRouteFPLAN route) {
		if (idRoutesFPLAN.contains(route.getRouteId())) {
			routesFPLAN.remove(route);
			idRoutesFPLAN.remove(route.getRouteId());
			return true;
		}
		return false;
	}
}
