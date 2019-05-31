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

import java.util.Map;
import java.util.Optional;
import java.util.Stack;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.ev.EvUnits;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

public class ChargerReader extends MatsimXmlParser {
	private final static String CHARGER = "charger";

	private final ChargingInfrastructureImpl chargingInfrastructure;
	private Map<Id<Link>, ? extends Link> links;

	public ChargerReader(Network network, ChargingInfrastructureImpl chargingInfrastructure) {
		this.chargingInfrastructure = chargingInfrastructure;
		links = network.getLinks();
	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if (CHARGER.equals(name)) {
			chargingInfrastructure.addCharger(createCharger(atts));
		}
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
	}

	private Charger createCharger(Attributes atts) {
		Id<Charger> id = Id.create(atts.getValue("id"), Charger.class);
		Link link = links.get(Id.createLinkId(atts.getValue("link")));
		double power_kW = Double.parseDouble(atts.getValue("power"));
		int plugs = Integer.parseInt(atts.getValue("capacity"));
		String chargerType = Optional.ofNullable(atts.getValue("type")).orElse(ChargerImpl.DEFAULT_CHARGER_TYPE);
		return new ChargerImpl(id, EvUnits.kW_to_W(power_kW), plugs, link, link.getCoord(), chargerType);
	}
}
