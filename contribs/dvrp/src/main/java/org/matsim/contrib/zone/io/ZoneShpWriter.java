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

package org.matsim.contrib.zone.io;

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.zone.Zone;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.*;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class ZoneShpWriter {
	public static final String ID_HEADER = "ID";

	private final Map<Id<Zone>, Zone> zones;
	private final String coordinateSystem;

	public ZoneShpWriter(Map<Id<Zone>, Zone> zones, String coordinateSystem) {
		this.zones = zones;
		this.coordinateSystem = coordinateSystem;
	}

	public void write(String shpFile) {
		CoordinateReferenceSystem crs = MGC.getCRS(coordinateSystem);

		PolygonFeatureFactory factory = new PolygonFeatureFactory.Builder().addAttribute(ID_HEADER, String.class)
				.setCrs(crs).setName("zone").create();

		List<SimpleFeature> features = new ArrayList<>();
		for (Zone z : zones.values()) {
			String id = z.getId() + "";
			features.add(factory.createPolygon(z.getMultiPolygon(), new Object[] { id }, id));
		}

		GeoFileWriter.writeGeometries(features, shpFile);
	}
}
