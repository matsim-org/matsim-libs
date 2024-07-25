/* *********************************************************************** *
 * project: org.matsim.*
 * EmissionEventsReader.java
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
package org.matsim.contrib.emissions.events;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.vehicles.Vehicle;


/**
 * @author benjamin
 *
 */
public final class EmissionEventsReader implements MatsimReader {
	// leave this public so that external code can generate "standard" emission events. MATSIM-893


	private final MatsimEventsReader delegate ;

	public EmissionEventsReader( EventsManager events ){
		this.delegate = new MatsimEventsReader(events);

		// yyyy should be possible to make these mappers available to other readers (that may want to combine event types that are not in the core).  kai, jan'19

		this.delegate.addCustomEventMapper(WarmEmissionEvent.EVENT_TYPE, event -> {

			Map<String, String> attributes = event.getAttributes();
			Map<Pollutant, Double> warmEmissions = new LinkedHashMap<>();

			double time = Double.NaN;
			Id<Link> linkId = null;
			Id<Vehicle> vehicleId = null;

			// the loop is necessary since we do now know which pollutants are in the event.
			for (Map.Entry<String, String> entry : attributes.entrySet()) {

				switch (entry.getKey()) {
					case "time" -> time = Double.parseDouble(entry.getValue());
					case "type" -> {
						// I don't think that we are doing anything here. kai, jan'19
					}
					case WarmEmissionEvent.ATTRIBUTE_LINK_ID -> linkId = Id.createLinkId(entry.getValue());
					case WarmEmissionEvent.ATTRIBUTE_VEHICLE_ID -> vehicleId = Id.createVehicleId(entry.getValue());
					case null, default -> {
						String pollutant = null; // the previous versions would write NOX instead of NOx
						if (entry.getKey() != null) {
							pollutant = entry.getKey().equals("NOX") ?
								"NOx" :
								entry.getKey();
						}

						Double value = Double.parseDouble(entry.getValue());
						warmEmissions.put(Pollutant.valueOf(pollutant), value);
					}
				}
			}

			return new WarmEmissionEvent(time, linkId, vehicleId, warmEmissions);
		});

		this.delegate.addCustomEventMapper(ColdEmissionEvent.EVENT_TYPE, event -> {

			Map<String, String> attributes = event.getAttributes();
			Map<Pollutant, Double> coldEmissions = new LinkedHashMap<>();

			double time = Double.NaN;
			Id<Link> linkId = null;
			Id<Vehicle> vehicleId = null;

			// the loop is necessary since we do now know which pollutants are in the event.
			for (Map.Entry<String, String> entry : attributes.entrySet()) {

				switch (entry.getKey()) {
					case "time" -> time = Double.parseDouble(entry.getValue());
					case "type" -> {
						// do nothing
					}
					case ColdEmissionEvent.ATTRIBUTE_LINK_ID -> linkId = Id.createLinkId(entry.getValue());
					case ColdEmissionEvent.ATTRIBUTE_VEHICLE_ID -> vehicleId = Id.createVehicleId(entry.getValue());
					case null, default -> {
						String pollutant = null; // the previous versions would write NOX instead of NOx
						if (entry.getKey() != null) {
							pollutant = entry.getKey().equals("NOX") ?
								"NOx" :
								entry.getKey();
						}

						Double value = Double.parseDouble(entry.getValue());
						coldEmissions.put(Pollutant.valueOf(pollutant), value);
					}
				}
			}
			return new ColdEmissionEvent(time, linkId, vehicleId, coldEmissions);

		});
	}

	@Override
	public void readFile( String filename ){
		delegate.readFile( filename );
	}

	@Override
	public void readURL( URL url ){
		delegate.readURL( url );
	}
}
