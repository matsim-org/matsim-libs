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

package org.matsim.contrib.zone;

import java.io.File;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.zone.io.ZoneShpReader;
import org.matsim.contrib.zone.io.ZoneXmlReader;

public class Zones {
	public static Map<Id<Zone>, Zone> readZones(String zonesXmlFile, String zonesShpFile) {
		try {
			return readZones(new File(zonesXmlFile).toURI().toURL(), new File(zonesShpFile).toURI().toURL());
		} catch (MalformedURLException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static Map<Id<Zone>, Zone> readZones(URL zonesXmlUrl, URL zonesShpUrl) {
		ZoneXmlReader xmlReader = new ZoneXmlReader();
		xmlReader.readURL(zonesXmlUrl);
		Map<Id<Zone>, Zone> zones = xmlReader.getZones();

		ZoneShpReader shpReader = new ZoneShpReader(zones);
		shpReader.readZones(zonesShpUrl);
		return zones;
	}
}
