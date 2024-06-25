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

import org.geotools.api.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.prep.PreparedPolygon;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.contrib.common.zones.ZoneImpl;
import org.matsim.core.utils.gis.GeoFileReader;

import java.net.URL;
import java.util.Collection;
import java.util.Map;

public class ZoneShpReader {
	private final Map<Id<Zone>, Zone> zones;

	public ZoneShpReader(Map<Id<Zone>, Zone> zones) {
		this.zones = zones;
	}

	public void readZones(URL url) {
		readZones(url, ZoneShpWriter.ID_HEADER);
	}

	public void readZones(URL url, String idHeader) {
		Collection<SimpleFeature> features = GeoFileReader.getAllFeatures(url);
		if (features.size() != zones.size()) {
			throw new RuntimeException("Features#: " + features.size() + "; zones#: " + zones.size());
		}

		for (SimpleFeature ft : features) {
			String id = ft.getAttribute(idHeader).toString();
			Zone z = zones.get(Id.create(id, Zone.class));
			if(z instanceof ZoneImpl zImpl) {
				zImpl.setGeometry(new PreparedPolygon((MultiPolygon) ft.getDefaultGeometry()));
			}
		}
	}
}
