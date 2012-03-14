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

package playground.mrieser.svi.converters;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeBuilder;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

import playground.mrieser.svi.data.Zones;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

/**
 * @author mrieser
 */
public class DynusTZonesReader {

	private final static Logger log = Logger.getLogger(DynusTZonesReader.class);

	private final Zones zones;

	public DynusTZonesReader(final Zones zones) {
		this.zones = zones;
	}

	public void readFile(final String dynusTZonesFile) {
		BufferedReader reader = IOUtils.getBufferedReader(dynusTZonesFile);

		Map<Integer, Coordinate> points = new HashMap<Integer, Coordinate>();

		try {
			reader.readLine(); // header line 1
			reader.readLine(); // header line 2
			String line = reader.readLine();
			String[] parts = line.split("\\s+");
			int nOfPoints = Integer.parseInt(parts[0]);
			int nOfZones = Integer.parseInt(parts[1]);
			reader.readLine(); // commentary line
			for (int i = 0; i < nOfPoints; i++) {
				line = reader.readLine();
				parts = line.split("\\s+");
				int id = Integer.parseInt(parts[0]);
				double x = Double.parseDouble(parts[1].replace(",", ""));
				double y = Double.parseDouble(parts[2]);
				points.put(id, new Coordinate(x, y));
			}
			reader.readLine(); // commentary line

			GeometryFactory geometryFactory = new GeometryFactory();
			AttributeType[] attributes = new AttributeType[2];
			attributes[0] = AttributeTypeFactory.newAttributeType("Polygon", Polygon.class);
			attributes[1] = AttributeTypeFactory.newAttributeType("ID", Integer.class);
			FeatureType type = FeatureTypeBuilder.newFeatureType(attributes, "zone");

			for (int i = 0; i < nOfZones; i++) {
				line = reader.readLine();
				parts = line.split("\\s+");
				int zoneId = Integer.parseInt(parts[0]);
				int nOfZonePoints = Integer.parseInt(parts[1].replace(":", ""));
				Coordinate[] coords = new Coordinate[nOfZonePoints];
				for (int j = 0; j < nOfZonePoints; j++) {
					int point = Integer.parseInt(parts[2+j].replace(",", ""));
					coords[j] = points.get(point);
				}
				LinearRing ring = geometryFactory.createLinearRing(coords);
				Polygon polygon = geometryFactory.createPolygon(ring, null);
				Feature p = type.create(new Object[] {polygon, zoneId});
				this.zones.addZone(p);
			}
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		} catch (final IllegalAttributeException e) {
			throw new UncheckedIOException(e);
		} catch (final SchemaException e) {
			throw new UncheckedIOException(e);
		} finally {
			try { reader.close(); }
			catch (final IOException e) { log.error("Could not close file " + dynusTZonesFile, e); }
		}
	}

}
