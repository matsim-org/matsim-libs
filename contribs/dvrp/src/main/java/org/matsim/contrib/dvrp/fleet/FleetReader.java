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

import java.util.Optional;
import java.util.Stack;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

/**
 * @author michalm
 */
public class FleetReader extends MatsimXmlParser {
	private static final String VEHICLE = "vehicle";

	private static final int DEFAULT_CAPACITY = 1;

	private final FleetSpecificationImpl fleet;

	public FleetReader(FleetSpecificationImpl fleet) {
		this.fleet = fleet;
	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if (VEHICLE.equals(name)) {
			fleet.addVehicleSpecification(createSpecification(atts));
		}
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
	}

	private DvrpVehicleSpecification createSpecification(Attributes atts) {
		return ImmutableDvrpVehicleSpecification.newBuilder()
				.id(Id.create(atts.getValue("id"), DvrpVehicle.class))
				.startLinkId(Id.createLinkId(atts.getValue("start_link")))
				.capacity(getCapacity(atts.getValue("capacity")))
				.serviceBeginTime(Double.parseDouble(atts.getValue("t_0")))
				.serviceEndTime(Double.parseDouble(atts.getValue("t_1")))
				.build();
	}

	private static int getCapacity(String capacityAttribute) {
		double capacity = Double.parseDouble(Optional.ofNullable(capacityAttribute).orElse(DEFAULT_CAPACITY + ""));
		if ((int)capacity != capacity) {
			//for backwards compatibility: use double when reading files (capacity used to be double)
			throw new IllegalArgumentException("capacity must be an integer value");
		}
		return (int)capacity;
	}
}
