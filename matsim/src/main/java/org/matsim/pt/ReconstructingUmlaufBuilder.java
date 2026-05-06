/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.pt;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.gbl.Gbl;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates Umlaeufe (= vehicle runs) from the transit schedule.  Will do something interesting only if {@link Departure}s in the
 * {@link org.matsim.pt.transitSchedule.api.TransitSchedule} have vehicle ids, <i>and</i> the same vehicle is used for multiple departures.
 *
 * @author (of documentation) kai
 */
public final class ReconstructingUmlaufBuilder implements UmlaufBuilder {
	private static final Logger log = LogManager.getLogger(ReconstructingUmlaufBuilder.class);

	private static final Comparator<UmlaufStueck> departureTimeComparator = Comparator.comparingDouble(o -> o.getDeparture().getDepartureTime());

	private final Collection<TransitLine> transitLines;
	private final Vehicles vehicles;
	private Map<Id<Umlauf>, Umlauf> umlaeufe = null;
	private List<UmlaufStueck> umlaufStuecke;
	private final UmlaufInterpolator umlaufInterpolator;
	private final Map<Id<Vehicle>, Id<Umlauf>> umlaufIdsByVehicleId;

	@Inject
	public ReconstructingUmlaufBuilder(Scenario scenario) {
		// (normal constructor used from TransitQSimEngine for testing :-(.  kai, mar'20) yy change

		this.umlaufInterpolator = new UmlaufInterpolator(scenario.getNetwork(), scenario.getConfig().scoring());
		this.transitLines = scenario.getTransitSchedule().getTransitLines().values();
		this.vehicles = scenario.getTransitVehicles();
		this.umlaufIdsByVehicleId = new HashMap<>();
	}

	@Override
	public Collection<Umlauf> build() {
		umlaeufe = new LinkedHashMap<>();
		createEmptyUmlaeufe();
		createUmlaufStuecke();
		createUmlaeufe();
		return umlaeufe.values();
	}

	/**
	 * (connect multiple umlautStuecke to one umlauf.  will do anything interesting only if same vehicle is shared across multiple departures.  vehicle
	 * id can be set for departures in transit schedule.  but usually isn't)
	 */
	private void createUmlaeufe() {
		for (UmlaufStueck umlaufStueck : umlaufStuecke) {
			Id<Umlauf> umlaufId = this.getUmlaufIdForVehicleId(umlaufStueck.getDeparture().getVehicleId());
			if (umlaufId == null) {
				throw new RuntimeException("Rotation ID could not be found. veh=" + umlaufStueck.getDeparture().getVehicleId());
			}
			Umlauf umlauf = umlaeufe.get(umlaufId);
			if (umlauf == null) {
				throw new RuntimeException("Rotation could not be found: " + umlaufId);
			}
			umlaufInterpolator.addUmlaufStueckToUmlauf(umlaufStueck, umlauf);
		}
	}

	private Id<Umlauf> getUmlaufIdForVehicleId(Id<Vehicle> vehId) {
		return this.umlaufIdsByVehicleId.get(vehId);
	}

	private Id<Umlauf> createUmlaufIdFromVehicle(Vehicle vehicle) {
		Id<Umlauf> id = Id.create(vehicle.getId().toString() + "_" + vehicle.getType().getId().toString(), Umlauf.class);
		this.umlaufIdsByVehicleId.put(vehicle.getId(), id);
		return id;
	}

	/**
	 * (create the data structures. each transit vehicles gets an empty umlauf)
	 */
	private void createEmptyUmlaeufe() {
		for (Vehicle vehicle : vehicles.getVehicles().values()) {
			UmlaufImpl umlauf = new UmlaufImpl(this.createUmlaufIdFromVehicle(vehicle));
			umlauf.setVehicleId(vehicle.getId());
			umlaeufe.put(umlauf.getId(), umlauf);
		}
	}

	/**
	 * (one for each "departure" in the transit schedule.  also sort them by departure time)
	 */
	private void createUmlaufStuecke() {
		this.umlaufStuecke = new ArrayList<>();
		log.info("Generating rotation segments");
		for (TransitLine line : transitLines) {
			for (TransitRoute route : line.getRoutes().values()) {
				Gbl.assertNotNull(route.getRoute()); // will fail much later if this is null.  kai, may'17
				for (Departure departure : route.getDepartures().values()) {
					UmlaufStueck umlaufStueck = new UmlaufStueck(line, route, departure);
					umlaufStuecke.add(umlaufStueck);
				}
			}
		}
		log.info("Finished generating rotation segments");
		this.umlaufStuecke.sort(departureTimeComparator);
	}
}
