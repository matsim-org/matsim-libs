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

public final class ChargerReader extends MatsimXmlParser {
	private final static String CHARGER = "charger";

	private final ChargingInfrastructureSpecification chargingInfrastructure;

	public ChargerReader(ChargingInfrastructureSpecification chargingInfrastructure) {
		super(ValidationType.DTD_ONLY);
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
				.plugPower(EvUnits.kW_to_W(Double.parseDouble(atts.getValue("plug_power"))))
				.plugCount(Optional.ofNullable(atts.getValue("plug_count"))
						.map(Integer::parseInt)
						.orElse(ChargerSpecification.DEFAULT_PLUG_COUNT))
				.build();
	}
}
