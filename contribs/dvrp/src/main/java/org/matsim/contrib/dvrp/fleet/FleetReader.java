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

import java.util.Stack;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.load.DvrpLoad;
import org.matsim.contrib.dvrp.load.DvrpLoadType;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

/**
 * @author michalm
 */
public class FleetReader extends MatsimXmlParser {
	private static final String VEHICLE = "vehicle";

	private static final int DEFAULT_CAPACITY = 1;

	private final FleetSpecification fleet;
	private final DvrpLoadType loadType;

	public FleetReader(FleetSpecification fleet, DvrpLoadType loadType) {
		super(ValidationType.DTD_ONLY);
		this.fleet = fleet;
		this.loadType = loadType;
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
		Id<DvrpVehicle> vehicleId = Id.create(atts.getValue("id"), DvrpVehicle.class);
		return ImmutableDvrpVehicleSpecification.newBuilder()
				.id(vehicleId)
				.startLinkId(Id.createLinkId(atts.getValue("start_link")))
				.capacity(getCapacity(atts.getValue("capacity")))
				.serviceBeginTime(Double.parseDouble(atts.getValue("t_0")))
				.serviceEndTime(Double.parseDouble(atts.getValue("t_1")))
				.build();
	}

	private DvrpLoad getCapacity(String val) {
		return loadType.deserialize(val == null ? String.valueOf(DEFAULT_CAPACITY) : val);
	}
}
