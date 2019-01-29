/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

import org.matsim.contrib.ev.EvUnits;
import org.matsim.contrib.ev.data.ElectricVehicle;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class ElectricVehicleWriter extends MatsimXmlWriter {
	private Iterable<ElectricVehicle> vehicles;
	private DecimalFormat format;

	public ElectricVehicleWriter(Iterable<ElectricVehicle> vehicles) {
		this.vehicles = vehicles;
		this.format = new DecimalFormat();
		format.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
		format.setMinimumIntegerDigits(1);
		format.setMaximumFractionDigits(2);
		format.setGroupingUsed(false);
	}

	public void write(String file) {
		openFile(file);
		writeDoctype("vehicles", "http://matsim.org/files/dtd/electric_vehicles_v1.dtd");
		writeStartTag("vehicles", Collections.<Tuple<String, String>>emptyList());
		writeVehicles();
		writeEndTag("vehicles");
		close();
	}

	private void writeVehicles() {
		for (ElectricVehicle v : vehicles) {
			List<Tuple<String, String>> atts = new ArrayList<>();
			atts.add(new Tuple<String, String>("id", v.getId().toString()));
			atts.add(new Tuple<String, String>("battery_capacity",
					format.format(EvUnits.J_to_kWh(v.getBattery().getCapacity())) + ""));
			atts.add(new Tuple<String, String>("initial_soc",
					format.format(EvUnits.J_to_kWh(v.getBattery().getSoc())) + ""));
			atts.add(new Tuple<>("chargerTypes", v.getChargingTypes().stream().collect(Collectors.joining(","))));
			atts.add(new Tuple<>("vehicleType", v.getVehicleType()));
			writeStartTag("vehicle", atts, true);
		}

	}
}
