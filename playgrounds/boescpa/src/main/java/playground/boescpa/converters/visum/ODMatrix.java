/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
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
 * *********************************************************************** *
 */

package playground.boescpa.converters.visum;

import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;
import playground.boescpa.converters.visum.obj.VisumTrip;
import playground.boescpa.converters.visum.obj.Zone;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Creates OD-matrices for Visum from a given trip file and given shp-files (zones, borders, farOuts).
 *
 * @author boescpa
 */
public class ODMatrix {

	// TODO-boescpa Write tests...

	private final List<VisumTrip> trips;
	private final int hour;
	private final String mode;

	private TreeMap<Long, TreeMap<Long,Long[]>> origins;
	private HashMap<Long,Zone> zones;
	private HashMap<Long,Zone> borders;
	private HashMap<Long,Zone> farOut;

	public ODMatrix(List<VisumTrip> tripCollection, int hour, String mode) {
		this.trips = tripCollection;
		this.hour = hour;
		this.mode = mode;

		origins = new TreeMap<Long, TreeMap<Long,Long[]>>();
		this.zones = new HashMap<Long,Zone>();
		this.borders = null;
		this.farOut = null;
	}


	// ************** Load shp-files

	public void loadZones(String zonesFile) {
		Set<SimpleFeature> features = new HashSet<SimpleFeature>();
		features.addAll(ShapeFileReader.getAllFeatures(zonesFile));
		
		for (SimpleFeature feature : features) {
			Zone zone = new Zone(feature);
			this.zones.put(zone.getZoneId(), zone);
		}
	}

	public void loadBorders(String bordersFile) {
		// Assumes bordersFile to be shp-File with centroids close to border of OD-Area.
		
		Set<SimpleFeature> features = new HashSet<SimpleFeature>();
		features.addAll(ShapeFileReader.getAllFeatures(bordersFile));
		
		this.borders = new HashMap<Long,Zone>();
		for (SimpleFeature feature : features) {
			Zone zone = new Zone(feature);
			this.borders.put(zone.getZoneId(), zone);
		}
	}

	public void loadFarOut(String farOutFile) {
		// Assumes farOutFile to be shp-File with centroids far out of OD-Area.
		
		Set<SimpleFeature> features = new HashSet<SimpleFeature>();
		features.addAll(ShapeFileReader.getAllFeatures(farOutFile));
		
		this.farOut = new HashMap<Long,Zone>();
		for (SimpleFeature feature : features) {
			Zone zone = new Zone(feature);
			this.farOut.put(zone.getZoneId(), zone);
		}
	}


	// ************** Create the OD-Matrix

	public void createODMatrix() {
		for (VisumTrip visumTrip : trips) {
			if (mode.equals("") || visumTrip.isModeType(mode)) {
				this.addTrip(visumTrip);
			}
		}
	}

	private void addTrip(VisumTrip visumTrip) {
		
		Long[] odValues = new Long[3];
		System.arraycopy(getODofTrip(visumTrip), 0, odValues, 0, 2);
		odValues[2] = (long)1;
		
		if (odValues[0] == 0 && odValues[1] == 0) {
			// Trip, which could not attributed to any of the known zones. -> It is dropped.
			return;
		}
		else if (origins.containsKey(odValues[0])) {
			TreeMap<Long,Long[]> destination = origins.get(odValues[0]);
			if (destination.containsKey(odValues[1])) {
				Long[] tempValues = destination.get(odValues[1]);
				tempValues[2] = tempValues[2] + odValues[2]; 
				destination.put(odValues[1], tempValues);
			}
			else {
				destination.put(odValues[1], odValues);
			}
			origins.put(odValues[0], destination);
		}
		else {
			TreeMap<Long, Long[]> destination = new TreeMap<Long, Long[]>();
			destination.put(odValues[1], odValues);
			origins.put(odValues[0], destination);
		}
	}

	private Long[] getODofTrip(VisumTrip visumTrip) {
		// Returns the Id of the trip's origin-zone [0] and destination-zone [1].
		// If no matching zone could be found, they get a zone id of '0'.
		
		Long[] odValues = new Long[2];
		
		for (Zone zone : this.zones.values()) {
			if (visumTrip.isOriginZone(zone)) {
				odValues[0] = zone.getZoneId(); 
			}
			if (visumTrip.isDestinZone(zone)) {
				 odValues[1] = zone.getZoneId(); 
			}
		}
		if (odValues[0] == null && odValues[1] == null) {
			// If non of both found, then uninteresting...
			odValues[0] = (long)0;
			odValues[1] = (long)0;
		}
		else if (odValues[0] == null) {
			// Means that odValues[1] != null, else initial if would have caught...
			odValues[0] = getClosestBorderOrFarOut(visumTrip, true);
		}
		else if (odValues[1] == null) {
			// Means that odValues[0] != null, else initial if would have caught...
			odValues[1] = getClosestBorderOrFarOut(visumTrip, false);
		}
		
		return odValues;
	}

	private Long getClosestBorderOrFarOut(VisumTrip visumTrip, boolean origOrDest) {
		// origOrDest: True for origin, false for destination.
		
		double distance = -1;
		double presDist;
		Long idClosest = (long)0;
		
		// Find nearest border centroid:
		for (Zone border : this.borders.values()) {
			presDist = visumTrip.distanceToCentroid(border, origOrDest);
			if (distance < 0 || distance > presDist) {
				distance = presDist;
				idClosest = border.getZoneId();
			}
		}
		// If closest centroid further than 5km, find nearest farOut:
		if (distance > 5000) {
			distance = -1;
			for (Zone farOut : this.farOut.values()) {
				presDist = visumTrip.distanceToCentroid(farOut, origOrDest);
				if (distance < 0 || distance > presDist) {
					distance = presDist;
					idClosest = farOut.getZoneId();
				}
			}
		}
		return idClosest;
	}


	// ************** Write Visum O-file

	public void createVisumOFile(String fileToWrite) throws IOException {
		
		BufferedWriter writer = IOUtils.getBufferedWriter(fileToWrite + "_" + this.mode + ".mtx");
		
		// Write header:
		writeVisumOFileHeader(writer, hour);
		
		// Write OD-lines:
		for (TreeMap<Long, Long[]> originZ : origins.values()) {
			for (Long[] odValues : originZ.values()) {
				writer.write(odValues[0] + " " + odValues[1] + " " + odValues[2]);
				writer.newLine();
			}
			writer.flush();
		}
		
		// Write footer:
		writeVisumOFileFooter(writer);
		
		// Close File:
		writer.close();
	}

	private void writeVisumOFileHeader(BufferedWriter writer, int hour) throws IOException {
		// Writes the header for a Visum-OD-File of the type o.
		
		writer.write("$O"); writer.newLine();
		writer.write("* Von  Bis"); writer.newLine();
		writer.write(hour + ".00 " + (hour+1) + ".00"); writer.newLine();
		writer.write("* Faktor"); writer.newLine();
		writer.write("1.00"); writer.newLine();
		writer.write("*"); writer.newLine();
		writer.write("* ETH Eidgenössische Technische Hochschule Zürich"); writer.newLine();
		String date = new SimpleDateFormat("dd.MM.yy").format(new GregorianCalendar().getTime());
		writer.write("* " + date); writer.newLine();
		writer.flush();
	}

	private void writeVisumOFileFooter(BufferedWriter writer) throws IOException {
		// Writes the footer for a Visum-OD-File of the type o.
		
		writer.write("* Netzobjektnamen"); writer.newLine();
		writer.write("$NAMES"); writer.newLine();
		
		for (Zone zone : this.zones.values()) {
			writer.write(zone.getZoneId() + " \"" + zone.getName() + "\"");
			writer.newLine();
		}
		for (Zone zone : this.borders.values()) {
			writer.write(zone.getZoneId() + " \"" + zone.getName() + "\"");
			writer.newLine();
		}
		for (Zone zone : this.farOut.values()) {
			writer.write(zone.getZoneId() + " \"" + zone.getName() + "\"");
			writer.newLine();
		}
		
		writer.flush();
	}
	
}
