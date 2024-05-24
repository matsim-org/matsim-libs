/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimesWriter.java
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

package org.matsim.contrib.analysis.christoph;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.locationtech.jts.geom.Coordinate;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.TimeBinUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.GeoFileWriter;
import org.matsim.core.utils.io.IOUtils;

/**
 * Analyzes the average link travel times and writes them to files (absolute and
 * relative values; txt and shp files).
 *
 * When added as ControlerListener, car travel times are collected and written
 * based on the TravelTimeCalculator provided by the services.
 *
 * For other modes, first call collectTravelTimes(...) and then the writer methods.
 *
 * @author cdobler
 */
public class TravelTimesWriter implements IterationEndsListener {

	public static String travelTimesAbsoluteFile = "travelTimesAbsolute.txt.gz";
	public static String travelTimesRelativeFile = "travelTimesRelative.txt.gz";
	public static String travelTimesAbsoluteSHPFile = "travelTimesAbsolute.shp";
	public static String travelTimesRelativeSHPFile = "travelTimesRelative.shp";

	public static final String newLine = "\n";
	public static final String delimiter = "\t";

	private TravelTime travelTime;
	private Network network;
	private double timeSlice;
	private int numSlots;

	private boolean writeTXTFiles = true;
	private boolean writeSHPFiles = true;

	private final Map<Link, double[]> travelTimes = new HashMap<Link, double[]>();

	private String crsString = "EPSG:21781";

	public TravelTimesWriter() {
	}

	public TravelTimesWriter(boolean writeTXTFiles, boolean writeSHPFiles) {
		this.writeTXTFiles = writeTXTFiles;
		this.writeSHPFiles = writeSHPFiles;
	}

	public String getCrsString() {
		return crsString;
	}

	public void setCrsString(String crsString) {
		this.crsString = crsString;
	}

	private void initBuffer(Network network) {

		for (Link link : network.getLinks().values()) {
			double[] travelTimeArray = new double[numSlots];

			double time = 0;
			for (int i = 0; i < numSlots; i++) {
//				travelTimeArray[i] = link.getLength() / link.getFreespeed(time);
				travelTimeArray[i] = Double.NaN;
				time = time + timeSlice;
			}

			travelTimes.put(link, travelTimeArray);
		}
	}

	private void collectTravelTimes(TravelTime travelTime, Network network, double timeSlice, int numSlots) {

		this.initBuffer(network);

		for (Entry<Link, double[]> entry : travelTimes.entrySet()) {
			double[] travelTimeArray = entry.getValue();

			double time = 0;
			for (int i = 0; i < this.numSlots; i++) {
				travelTimeArray[i] = this.travelTime.getLinkTravelTime(entry.getKey(), time, null, null);
				time = time + this.timeSlice;
			}
		}
	}

	public void writeAbsoluteTravelTimes(final String file) {

		try {
			BufferedWriter timesWriter = IOUtils.getBufferedWriter(file);

			writeHeader(timesWriter);
			writeRows(timesWriter, true);

			timesWriter.flush();
			timesWriter.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void writeRelativeTravelTimes(final String file) {

		try {
			BufferedWriter timesWriter = IOUtils.getBufferedWriter(file);

			writeHeader(timesWriter);
			writeRows(timesWriter, false);

			timesWriter.flush();
			timesWriter.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void writeHeader(BufferedWriter timesWriter) throws IOException {
		timesWriter.write("linkId");

		for (int i = 0; i < this.numSlots; i++) {
			timesWriter.write(delimiter);
			timesWriter.write(String.valueOf(i * this.timeSlice));
		}
		timesWriter.write(newLine);
	}

	private void writeRows(BufferedWriter timesWriter, boolean absolute) throws IOException {

		for (Link link : this.network.getLinks().values()) {

			timesWriter.write(link.getId().toString());

			double[] travelTimeArray = this.travelTimes.get(link);
			for (int i = 0; i < this.numSlots; i++) {
				timesWriter.write(delimiter);
				if (absolute) {
					timesWriter.write(String.valueOf(travelTimeArray[i]));
				} else {
					double freeSpeedTravelTime = link.getLength() / link.getFreespeed(i * this.timeSlice);
					double relativeTravelTime = travelTimeArray[i] / freeSpeedTravelTime;
					timesWriter.write(String.valueOf(relativeTravelTime));
				}
			}
			timesWriter.write(newLine);
		}
	}

	public void writeAbsoluteSHPTravelTimes(String file, CoordinateReferenceSystem crs, boolean ignoreExitLinks) {
		try {
			Collection<SimpleFeature> ft = generateSHPFileData(crs, this.network, true, ignoreExitLinks);
			GeoFileWriter.writeGeometries(ft, file);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void writeRelativeSHPTravelTimes(String file, CoordinateReferenceSystem crs, boolean ignoreExitLinks) {
		try {
			Collection<SimpleFeature> ft = generateSHPFileData(crs, this.network, false, ignoreExitLinks);
			GeoFileWriter.writeGeometries(ft, file);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private Collection<SimpleFeature> generateSHPFileData(CoordinateReferenceSystem crs, Network network, boolean absolute, boolean ignoreExitLinks) throws Exception {

		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();

		PolylineFeatureFactory.Builder builder = new PolylineFeatureFactory.Builder()
			.setCrs(crs)
			.setName("links")
			.addAttribute("ID", String.class)
			.addAttribute("fromID", String.class)
			.addAttribute("toID", String.class)
			.addAttribute("length", Double.class)
			.addAttribute("fstt", Double.class);

		for (int i = 0; i < this.numSlots; i++) {
			builder.addAttribute(String.valueOf(i * this.timeSlice), Double.class);
		}

		PolylineFeatureFactory factory = builder.create();
		for (Link link : network.getLinks().values()) {

			if (ignoreExitLinks) {
				String string = link.getId().toString().toLowerCase();
				if (string.contains("rescue") || string.contains("exit")) continue;
			}

			Coordinate[] coordArray = new Coordinate[] {coord2Coordinate(link.getFromNode().getCoord()), coord2Coordinate(link.getCoord()), coord2Coordinate(link.getToNode().getCoord())};

			Object[] attributes = new Object[5 + this.numSlots];
			attributes[0] = link.getId().toString();
			attributes[1] = link.getFromNode().getId().toString();
			attributes[2] = link.getToNode().getId().toString();
			attributes[3] = link.getLength();
			attributes[4] = link.getLength()/link.getFreespeed();

			double[] travelTimeArray = this.travelTimes.get(link);
			for (int i = 0; i < this.numSlots; i++) {

				if (absolute) {
					attributes[5 + i] = travelTimeArray[i];
				} else {
					double freeSpeedTravelTime = link.getLength() / link.getFreespeed(i * this.timeSlice);
					double relativeTravelTime = travelTimeArray[i] / freeSpeedTravelTime;
					attributes[5 + i] = relativeTravelTime;
				}
			}

			SimpleFeature ft = factory.createPolyline(coordArray, attributes, link.getId().toString());
			features.add(ft);
		}

		return features;
	}

	/**
	 * Converts a MATSim {@link org.matsim.api.core.v01.Coord} into a Geotools <code>Coordinate</code>
	 * @param coord MATSim coordinate
	 * @return Geotools coordinate
	 */
	private Coordinate coord2Coordinate(final Coord coord) {
		return new Coordinate(coord.getX(), coord.getY());
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {

		MatsimServices controler = event.getServices();
		Config config = controler.getConfig();
		this.travelTime = controler.getLinkTravelTimes();
        this.network = controler.getScenario().getNetwork();
		this.timeSlice = config.travelTimeCalculator().getTraveltimeBinSize();
		int maxTime = 30 * 3600;	// default value from TravelTimeCalculator
		this.numSlots = TimeBinUtils.getTimeBinCount(maxTime, timeSlice);

		this.collectTravelTimes(travelTime, network, timeSlice, numSlots);

		OutputDirectoryHierarchy controlerIO = event.getServices().getControlerIO();
		int iteration = event.getIteration();

		String absoluteFile = TravelTimesWriter.travelTimesAbsoluteFile;
		String relativeFile = TravelTimesWriter.travelTimesRelativeFile;
		if (absoluteFile.toLowerCase().endsWith(".gz")) {
			absoluteFile = absoluteFile.substring(0, absoluteFile.length() - 3);
		}
		if (relativeFile.toLowerCase().endsWith(".gz")) {
			relativeFile = relativeFile.substring(0, relativeFile.length() - 3);
		}

		if (writeTXTFiles) {
			String absoluteTravelTimesFile = controlerIO.getIterationFilename(iteration, absoluteFile);
			String relativeTravelTimesFile = controlerIO.getIterationFilename(iteration, relativeFile);
			this.writeAbsoluteTravelTimes(absoluteTravelTimesFile);
			this.writeRelativeTravelTimes(relativeTravelTimesFile);
		}

		if (writeSHPFiles) {
			String absoluteSHPTravelTimesFile = controlerIO.getIterationFilename(0, TravelTimesWriter.travelTimesAbsoluteSHPFile);
			String relativeSHPTravelTimesFile = controlerIO.getIterationFilename(0, TravelTimesWriter.travelTimesRelativeSHPFile);
			this.writeAbsoluteSHPTravelTimes(absoluteSHPTravelTimesFile, MGC.getCRS(crsString), true);
			this.writeRelativeSHPTravelTimes(relativeSHPTravelTimesFile, MGC.getCRS(crsString), true);
		}

		this.travelTimes.clear();
	}

}
