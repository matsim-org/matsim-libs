/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.fleet;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.contrib.dvrp.load.DvrpLoadType;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;

/**
 * @author michalm
 */
public class FleetWriter extends MatsimXmlWriter {

	private final static Logger LOGGER = LogManager.getLogger(FleetWriter.class);

	private final Stream<? extends DvrpVehicleSpecification> vehicleSpecifications;
	private final DvrpLoadType dvrpLoadType;
	private boolean encounteredNonIntegerLoad;

	public FleetWriter(Stream<? extends DvrpVehicleSpecification> vehicleSpecifications, DvrpLoadType dvrpLoadType) {
		this.vehicleSpecifications = vehicleSpecifications;
		this.dvrpLoadType = dvrpLoadType;
	}

	public void write(String file) {
		openFile(file);
		writeDoctype("vehicles", "http://matsim.org/files/dtd/dvrp_vehicles_v1.dtd");
		writeStartTag("vehicles", Collections.emptyList());
		this.encounteredNonIntegerLoad = false;
		this.vehicleSpecifications.forEach(this::writeVehicle);
		writeEndTag("vehicles");
		close();
		if(encounteredNonIntegerLoad) {
			LOGGER.warn("Encountered a non-integer vehicle capacity. The resulting will not be compatible with the standard FleetReader");
		}
	}

	private synchronized void writeVehicle(DvrpVehicleSpecification vehicle) {
		String serializedCapacity = dvrpLoadType.serialize(vehicle.getCapacity());
		try {
			Integer.parseInt(serializedCapacity);
		} catch(NumberFormatException e) {
			this.encounteredNonIntegerLoad = true;
		}
		List<Tuple<String, String>> attributes = Arrays.asList(Tuple.of("id", vehicle.getId().toString()),
				Tuple.of("start_link", vehicle.getStartLinkId() + ""), Tuple.of("t_0", vehicle.getServiceBeginTime() + ""),
				Tuple.of("t_1", vehicle.getServiceEndTime() + ""), Tuple.of("capacity", serializedCapacity));
		writeStartTag("vehicle", attributes, true);
	}
}
