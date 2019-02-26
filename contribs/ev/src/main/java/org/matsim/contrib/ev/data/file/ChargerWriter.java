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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.matsim.contrib.ev.EvUnits;
import org.matsim.contrib.ev.data.Charger;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;

public class ChargerWriter extends MatsimXmlWriter {
	private Iterable<Charger> chargers;

	public ChargerWriter(Iterable<Charger> chargers) {
		this.chargers = chargers;
	}

	public void write(String file) {
		openFile(file);
		writeDoctype("chargers", "http://matsim.org/files/dtd/chargers_v1.dtd");
		writeStartTag("chargers", Collections.<Tuple<String, String>>emptyList());
		writeChargers();
		writeEndTag("chargers");
		close();
	}

	private void writeChargers() {
		for (Charger c : chargers) {
			List<Tuple<String, String>> atts = Arrays.asList(Tuple.of("id", c.getId().toString()),
					Tuple.of("link", c.getLink().getId() + ""), Tuple.of("power", EvUnits.W_to_kW(c.getPower()) + ""),
					Tuple.of("capacity", c.getPlugs() + ""), Tuple.of("type", c.getChargerType()));
			writeStartTag("charger", atts, true);
		}
	}
}
