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

import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;

/**
 * @author michalm
 */
public class FleetWriter extends MatsimXmlWriter {
	private Stream<? extends DvrpVehicleSpecification> vehicleSpecifications;

	public FleetWriter(Stream<? extends DvrpVehicleSpecification> vehicleSpecifications) {
		this.vehicleSpecifications = vehicleSpecifications;
	}

	public void write(String file) {
		openFile(file);
		writeDoctype("vehicles", "http://matsim.org/files/dtd/dvrp_vehicles_v1.dtd");
		writeStartTag("vehicles", Collections.emptyList());
		writeVehicles();
		writeEndTag("vehicles");
		close();
	}

	private void writeVehicles() {
		vehicleSpecifications.forEach(veh -> {
			List<Tuple<String, String>> atts = Arrays.asList(Tuple.of("id", veh.getId().toString()),
					Tuple.of("start_link", veh.getStartLinkId() + ""), Tuple.of("t_0", veh.getServiceBeginTime() + ""),
					Tuple.of("t_1", veh.getServiceEndTime() + ""), Tuple.of("capacity", veh.getCapacity() + ""));
			writeStartTag("vehicle", atts, true);
		});
	}
}
