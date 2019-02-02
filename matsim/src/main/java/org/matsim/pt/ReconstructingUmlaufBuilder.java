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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

public class ReconstructingUmlaufBuilder implements UmlaufBuilder {
	private static final Logger log = Logger.getLogger(ReconstructingUmlaufBuilder.class);

	private static final Comparator<UmlaufStueck> departureTimeComparator = new Comparator<UmlaufStueck>() {

		@Override
		public int compare(UmlaufStueck o1, UmlaufStueck o2) {
			return Double.compare(o1.getDeparture().getDepartureTime(), o2.getDeparture().getDepartureTime());
		}

	};

	private Collection<TransitLine> transitLines;
	private Vehicles vehicles;
	private Map<Id<Umlauf>,Umlauf> umlaeufe = null;
	private ArrayList<UmlaufStueck> umlaufStuecke;
	private UmlaufInterpolator umlaufInterpolator;
	private Map<Id<Vehicle>, Id<Umlauf>> umlaufIdsByVehicleId;

	public ReconstructingUmlaufBuilder(Network network, Collection<TransitLine> transitLines,
			Vehicles basicVehicles, PlanCalcScoreConfigGroup config) {
		this.umlaufInterpolator = new UmlaufInterpolator(network, config);
		this.transitLines = transitLines;
		this.vehicles = basicVehicles;
		this.umlaufIdsByVehicleId = new HashMap<>();
	}

	@Override
	public Collection<Umlauf> build() {
		umlaeufe = new HashMap<>();
		createEmptyUmlaeufe();
		createUmlaufStuecke();
		createUmlaeufe();
		return umlaeufe.values();
	}
	
	private void createUmlaeufe(){
		int cnt = 0;
		for (UmlaufStueck umlaufStueck : umlaufStuecke) {
			Id<Umlauf> umlaufId = this.getUmlaufIdForVehicleId(umlaufStueck.getDeparture().getVehicleId());
			if (umlaufId == null) {
				throw new RuntimeException("UmlaufId could not be found. veh=" + umlaufStueck.getDeparture().getVehicleId());
			}
			Umlauf umlauf = umlaeufe.get(umlaufId);
			if (umlauf == null) {
				throw new RuntimeException("Umlauf could not be found: " + umlaufId);
			}
			umlaufInterpolator.addUmlaufStueckToUmlauf(umlaufStueck, umlauf);
			cnt++;
			printStatus(cnt);
		}
	}
	
	private Id<Umlauf> getUmlaufIdForVehicleId(Id<Vehicle> vehId){
		return this.umlaufIdsByVehicleId.get(vehId);
	}
	
	private Id<Umlauf> createUmlaufIdFromVehicle(Vehicle vehicle){
		Id<Umlauf> id = Id.create(vehicle.getId().toString() + "_" + vehicle.getType().getId().toString(), Umlauf.class);
		this.umlaufIdsByVehicleId.put(vehicle.getId(), id);
		return id;
	}

	private void createEmptyUmlaeufe() {
		for (Vehicle vehicle : vehicles.getVehicles().values()) {
			UmlaufImpl umlauf = new UmlaufImpl(this.createUmlaufIdFromVehicle(vehicle));
			umlauf.setVehicleId(vehicle.getId());
			umlaeufe.put(umlauf.getId(), umlauf);
		}
	}

	private void createUmlaufStuecke() {
		this.umlaufStuecke = new ArrayList<>();
		log.info("Generating UmlaufStuecke ...");
		int cnt = 0;
		for (TransitLine line : transitLines) {
			for (TransitRoute route : line.getRoutes().values()) {
				Gbl.assertNotNull(route.getRoute()); // will fail much later if this is null.  kai, may'17
				for (Departure departure : route.getDepartures().values()) {
					UmlaufStueck umlaufStueck = new UmlaufStueck(line, route, departure);
					umlaufStuecke.add(umlaufStueck);
					cnt++;
					printStatus(cnt);
				}
			}
		}
		log.info("... done generating UmlaufStuecke");
		Collections.sort(this.umlaufStuecke, departureTimeComparator);
	}
	
	private void printStatus(int cnt){
		if ( cnt%100==0 ) {
			System.out.print('.');
			System.out.flush();
		}
		if ( cnt%10000==0 ) {
			System.out.println();
			System.out.flush();
		}
	}

}
