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

package playground.michalm.ev.data.file;

import java.util.*;

import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;

import playground.michalm.ev.EvUnitConversions;
import playground.michalm.ev.data.Charger;

public class ChargerWriter extends MatsimXmlWriter {
	private Iterable<Charger> chargers;

	public ChargerWriter(Iterable<Charger> chargers) {
		this.chargers = chargers;
	}

	public void write(String file) {
		openFile(file);
		writeDoctype("chargers", "http://matsim.org/files/dtd/chargers_v1.dtd");
		writeStartTag("chargers", Collections.<Tuple<String, String>> emptyList());
		writeVehicles();
		writeEndTag("chargers");
		close();
	}

	private void writeVehicles() {
		for (Charger c : chargers) {
			List<Tuple<String, String>> atts = new ArrayList<>();
			atts.add(new Tuple<String, String>("id", c.getId().toString()));
			atts.add(new Tuple<String, String>("link", c.getLink().getId() + ""));
			double powerInKW = c.getPower() / EvUnitConversions.W_PER_kW;
			atts.add(new Tuple<String, String>("power", powerInKW + ""));
			atts.add(new Tuple<String, String>("capacity", c.getPlugs() + ""));
			writeStartTag("charger", atts, true);
		}
	}
}
