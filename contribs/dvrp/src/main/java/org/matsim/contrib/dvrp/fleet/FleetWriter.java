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

import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author michalm
 */
public class FleetWriter extends MatsimXmlWriter {
	private final Stream<? extends DvrpVehicleSpecification> vehicleSpecifications;

	public FleetWriter(Stream<? extends DvrpVehicleSpecification> vehicleSpecifications) {
		this.vehicleSpecifications = vehicleSpecifications;
	}

	public void write(String file) {
		openFile(file);
		writeDoctype("vehicles", "http://matsim.org/files/dtd/dvrp_vehicles_v1.dtd");
		writeStartTag("vehicles", Collections.emptyList());
		this.vehicleSpecifications.forEach(this::writeVehicle);
		writeEndTag("vehicles");
		close();
	}

	private synchronized void writeVehicle(DvrpVehicleSpecification vehicle) {
		List<Tuple<String, String>> attributes = Arrays.asList(Tuple.of("id", vehicle.getId().toString()),
				Tuple.of("start_link", vehicle.getStartLinkId() + ""), Tuple.of("t_0", vehicle.getServiceBeginTime() + ""),
				Tuple.of("t_1", vehicle.getServiceEndTime() + ""), Tuple.of("capacity", vehicle.getCapacity() + ""));
		writeStartTag("vehicle", attributes, true);
	}
}
