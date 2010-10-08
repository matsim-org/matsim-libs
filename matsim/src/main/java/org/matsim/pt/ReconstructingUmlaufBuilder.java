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
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.transitSchedule.api.Departure;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
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
	private Vehicles basicVehicles;
	private Map<Id,Umlauf> umlaeufe = new HashMap<Id,Umlauf>();
	private ArrayList<UmlaufStueck> umlaufStuecke;

	private UmlaufInterpolator umlaufInterpolator;


	public ReconstructingUmlaufBuilder(Network network, Collection<TransitLine> transitLines,
			Vehicles basicVehicles, CharyparNagelScoringConfigGroup config) {
		super();
		this.umlaufInterpolator = new UmlaufInterpolator(network, config);
		this.transitLines = transitLines;
		this.basicVehicles = basicVehicles;
	}

	@Override
	public Collection<Umlauf> build() {
		createEmptyUmlaeufe();
		createUmlaufStuecke();
		log.info("Generating Umlaeufe; if this is extremely slow, try more memory:") ;
		System.out.flush() ;
		int cnt = 0 ;
		for (UmlaufStueck umlaufStueck : umlaufStuecke) {
			Umlauf umlauf = umlaeufe.get(umlaufStueck.getDeparture().getVehicleId());
			umlaufInterpolator.addUmlaufStueckToUmlauf(umlaufStueck, umlauf);
			cnt++ ;
			if ( cnt%100==0 ) System.out.print('.') ;
			if ( cnt%10000==0 ) System.out.println();
		}
		System.out.println() ;
		System.out.flush() ;
		return umlaeufe.values();
	}

	private void createEmptyUmlaeufe() {
		for (Vehicle basicVehicle : basicVehicles.getVehicles().values()) {
			UmlaufImpl umlauf = new UmlaufImpl(basicVehicle.getId());
			umlauf.setVehicleId(basicVehicle.getId());
			umlaeufe.put(umlauf.getId(), umlauf);
		}
	}

	private void createUmlaufStuecke() {
		this.umlaufStuecke = new ArrayList<UmlaufStueck>();
		log.info("Generating UmlaufStuecke") ;
		System.out.flush() ;
		int cnt = 0 ;
		for (TransitLine line : transitLines) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (Departure departure : route.getDepartures().values()) {
					UmlaufStueck umlaufStueck = new UmlaufStueck(line, route,
							departure);
					umlaufStuecke.add(umlaufStueck);
					cnt++ ;
					if ( cnt%100==0 ) System.out.print('.') ;
					if ( cnt%10000==0 ) System.out.println();
				}
			}
		}
		System.out.println() ;
		System.out.flush() ;
		Collections.sort(this.umlaufStuecke, departureTimeComparator);
	}

}
