/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package org.matsim.contrib.ev.data.file;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.ev.EvUnits;
import org.matsim.contrib.ev.data.*;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

public class ElectricVehicleReader extends MatsimXmlParser {
	private static final String VEHICLE = "vehicle";

	private final ElectricFleetImpl fleet;

	public ElectricVehicleReader(ElectricFleetImpl fleet) {
		this.fleet = fleet;
	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if (VEHICLE.equals(name)) {
			fleet.addElectricVehicle(createVehicle(atts));
		}
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
	}

	private ElectricVehicle createVehicle(Attributes atts) {
		Id<ElectricVehicle> id = Id.create(atts.getValue("id"), ElectricVehicle.class);
		double batteryCapacity_kWh = Double.parseDouble(atts.getValue("battery_capacity"));
		double initialSoc_kWh = Double.parseDouble(atts.getValue("initial_soc"));
		String chargerType = atts.getValue("chargerTypes");
		List<String> chargerTypes = chargerType != null ?
				Arrays.asList(chargerType.split(",")) :
				Collections.singletonList(ChargerImpl.DEFAULT_CHARGER_TYPE);
		String vehicleType = atts.getValue("vehicleType");
		vehicleType = vehicleType != null ? vehicleType : ElectricVehicleImpl.DEFAULTVEHICLETYPE;

		return new ElectricVehicleImpl(id,
				new BatteryImpl(EvUnits.kWh_to_J(batteryCapacity_kWh), EvUnits.kWh_to_J(initialSoc_kWh)), chargerTypes,
				vehicleType);
	}
}
