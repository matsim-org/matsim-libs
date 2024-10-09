/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.common.zones.io;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.contrib.common.zones.ZoneImpl;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;

public class ZoneXmlReader extends MatsimXmlParser {
	private final static String ZONE = "zone";

	private final Map<Id<Zone>, Zone> zones = new LinkedHashMap<>();

	public Map<Id<Zone>, Zone> getZones() {
		return zones;
	}

	public ZoneXmlReader() {
		super(ValidationType.DTD_ONLY);
	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if (ZONE.equals(name)) {
			startZone(atts);
		}
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
	}

	private void startZone(Attributes atts) {
		Id<Zone> id = Id.create(atts.getValue("id"), Zone.class);
		String type = atts.getValue("type");
		zones.put(id, new ZoneImpl(id, null, null, type));
	}
}
