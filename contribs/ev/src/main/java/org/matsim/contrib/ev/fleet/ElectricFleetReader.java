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

package org.matsim.contrib.ev.fleet;

import java.util.Optional;
import java.util.Stack;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.ev.EvUnits;
import org.matsim.contrib.ev.data.ChargerImpl;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import com.google.common.collect.ImmutableList;

/**
 * @author Michal Maciejewski (michalm)
 */
public class ElectricFleetReader extends MatsimXmlParser {
	private static final String VEHICLE = "vehicle";

	private final ElectricFleetSpecification fleet;

	public ElectricFleetReader(ElectricFleetSpecification fleet) {
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

	private ElectricVehicleSpecification createSpecification(Attributes atts) {
		return ImmutableElectricVehicleSpecification.newBuilder()
				.id(Id.create(atts.getValue("id"), ElectricVehicle.class))
				.batteryCapacity(EvUnits.kWh_to_J(Double.parseDouble(atts.getValue("battery_capacity"))))
				.initialSoc(EvUnits.kWh_to_J(Double.parseDouble(atts.getValue("initial_soc"))))
				.vehicleType(Optional.ofNullable(atts.getValue("vehicleType"))
						.orElse(ElectricVehicleImpl.DEFAULT_VEHICLE_TYPE))
				.chargerTypes(ImmutableList.copyOf(Optional.ofNullable(atts.getValue("chargerTypes"))
						.orElse(ChargerImpl.DEFAULT_CHARGER_TYPE)
						.split(",")))
				.build();
	}
}
