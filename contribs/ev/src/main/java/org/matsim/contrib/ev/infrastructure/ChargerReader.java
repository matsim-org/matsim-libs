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

package org.matsim.contrib.ev.infrastructure;

import java.util.Optional;
import java.util.Stack;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.ev.EvUnits;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

public class ChargerReader extends MatsimXmlParser {
	private final static String CHARGER = "charger";

	private final ChargingInfrastructureSpecification chargingInfrastructure;

	public ChargerReader(ChargingInfrastructureSpecification chargingInfrastructure) {
		this.chargingInfrastructure = chargingInfrastructure;
	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if (CHARGER.equals(name)) {
			chargingInfrastructure.addChargerSpecification(createSpecification(atts));
		}
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
	}

	private ChargerSpecification createSpecification(Attributes atts) {
		return ImmutableChargerSpecification.newBuilder()
				.id(Id.create(atts.getValue("id"), Charger.class))
				.linkId(Id.createLinkId(atts.getValue("link")))
				.chargerType(
						Optional.ofNullable(atts.getValue("type")).orElse(ChargerSpecification.DEFAULT_CHARGER_TYPE))
				.maxPower(
						EvUnits.kW_to_W(Double.parseDouble(atts.getValue("power"))))//TODO rename to "maxPower" in DTD??
				.plugCount(Integer.parseInt(atts.getValue("capacity")))//TODO rename to "plugCount" in DTD
				.build();
	}
}
