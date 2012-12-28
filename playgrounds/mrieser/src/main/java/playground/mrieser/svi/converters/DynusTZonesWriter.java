/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;

import playground.mrieser.svi.data.Zones;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

/**
 * @author mrieser
 */
public class DynusTZonesWriter {

	private final static Logger log = Logger.getLogger(DynusTZonesWriter.class);

	private final Zones zones;
	private final String attrName;

	public DynusTZonesWriter(final Zones zones, final String zoneIdAttributeName) {
		this.zones = zones;
		this.attrName = zoneIdAttributeName;
	}

	public void writeToDirectory(final String targetDir) {

		// write zone.dat

		final String filename = targetDir + "/zone.dat";
		BufferedWriter writer = IOUtils.getBufferedWriter(filename);
		try {
			writer.write(" This file defines zone regions\r\n");
			writer.write(" number of feature points, number of zones\r\n");

			int cntFeatures = 0;
			int cntCoords = 0;
			for (SimpleFeature f : this.zones.getAllZones()) {
				cntFeatures++;
				Object g = f.getDefaultGeometry();
				if (g instanceof Geometry) {
					cntCoords += ((Geometry) g).getCoordinates().length;
				}
			}

			writer.write(Integer.toString(cntCoords));
			writer.write(" ");
			writer.write(Integer.toString(cntFeatures));
			writer.write("\r\n");

			writer.write("node #, x-coordinate, y-coordinate\r\n");

			int idx = 0;
			for (SimpleFeature f : this.zones.getAllZones()) {
				Object g = f.getDefaultGeometry();
				if (g instanceof Geometry) {
					for (Coordinate c : ((Geometry) g).getCoordinates()) {
						idx++;
						writer.write(Integer.toString(idx));
						writer.write(" ");
						writer.write(Double.toString(c.x));
						writer.write(", ");
						writer.write(Double.toString(c.y));
						writer.write("\r\n");
					}
				}
			}

			writer.write(" zone #, number of nodes, node #'s\r\n");

			int zoneIdx = 0;
			idx = 0;
			for (SimpleFeature f : this.zones.getAllZones()) {
				zoneIdx++;
				Object g = f.getDefaultGeometry();
				if (g instanceof Geometry) {
					writer.write(Integer.toString(zoneIdx));
					writer.write(" ");
					Coordinate[] coords = ((Geometry) g).getCoordinates();
					writer.write(Integer.toString(coords.length));
					writer.write(": ");
					for (Coordinate c : coords) {
						idx++;
						writer.write(Integer.toString(idx));
						writer.write(", ");
					}
					writer.write("\r\n");
				}
			}

		} catch (IOException e) {
			log.error("Could not write file " + filename);
		} finally {
			try {
				writer.close();
			} catch (IOException e2) {
				log.error("Could not close file " + filename);
			}
		}

		// write zone_mapping.csv

		final String mappingFilename = targetDir + "/zone_mapping.csv";
		BufferedWriter mappingWriter = IOUtils.getBufferedWriter(mappingFilename);
		try {
			mappingWriter.write("ZONENO,TAZ\r\n");
			int zoneIdx = 0;
			for (SimpleFeature f : this.zones.getAllZones()) {
				zoneIdx++;
				mappingWriter.write(Integer.toString(zoneIdx));
				mappingWriter.write(",");
				mappingWriter.write(f.getAttribute(this.attrName).toString());
				mappingWriter.write("\r\n");
			}
		} catch (IOException e) {
			log.error("Could not write file " + mappingFilename);
		} finally {
			try {
				mappingWriter.close();
			} catch (IOException e2) {
				log.error("Could not close file " + mappingFilename);
			}
		}
	}
}
