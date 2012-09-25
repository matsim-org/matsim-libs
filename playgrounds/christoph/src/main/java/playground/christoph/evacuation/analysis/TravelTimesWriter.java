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

package playground.christoph.evacuation.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeBuilder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorConfigGroup;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

public class TravelTimesWriter {

	public static final String travelTimesAbsoluteFile = "travelTimesAbsolute.txt.gz";
	public static final String travelTimesRelativeFile = "travelTimesRelative.txt.gz";
	public static final String travelTimesAbsoluteSHPFile = "travelTimesAbsolute.shp";
	public static final String travelTimesRelativeSHPFile = "travelTimesRelative.shp";
	
	public static final String newLine = "\n";
	public static final String delimiter = "\t";
	
	private final TravelTime travelTime;
	private final Network network;
	private final Map<Link, double[]> travelTimes;
	private final int timeSlice;
	private final int numSlots;
	
	public TravelTimesWriter(TravelTime travelTime, Network network, TravelTimeCalculatorConfigGroup ttconfigGroup) {
		this(travelTime, network, ttconfigGroup.getTraveltimeBinSize(), 30*3600, ttconfigGroup); // default: 30 hours at most
	}

	public TravelTimesWriter(TravelTime travelTime, Network network, int timeslice, int maxTime, 
			TravelTimeCalculatorConfigGroup ttconfigGroup) {
		this.travelTime = travelTime;
		this.network = network;
		this.timeSlice = timeslice;
		this.numSlots = (maxTime / this.timeSlice) + 1;
		
		this.travelTimes = new HashMap<Link, double[]>();
		initBuffer(network);
	}

	private void initBuffer(Network network) {


		for (Link link : network.getLinks().values()) {
			double[] travelTimeArray = new double[numSlots];

			int time = 0;
			for (int i = 0; i < numSlots; i++) {
//				travelTimeArray[i] = link.getLength() / link.getFreespeed(time);
				travelTimeArray[i] = Double.NaN;
				time = time + timeSlice;
			}

			travelTimes.put(link, travelTimeArray);
		}
	}

	public void collectTravelTimes() {

		for (Entry<Link, double[]> entry : travelTimes.entrySet()) {
			double[] travelTimeArray = entry.getValue();

			int time = 0;
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
			Gbl.errorMsg(e);
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
			Gbl.errorMsg(e);
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
			Collection<Feature> ft = generateSHPFileData(crs, this.network, true, ignoreExitLinks);
			ShapeFileWriter.writeGeometries(ft, file);			
		} catch (Exception e) {
			Gbl.errorMsg(e);
		}
	}

	public void writeRelativeSHPTravelTimes(String file, CoordinateReferenceSystem crs, boolean ignoreExitLinks) {
		try {
			Collection<Feature> ft = generateSHPFileData(crs, this.network, false, ignoreExitLinks);
			ShapeFileWriter.writeGeometries(ft, file);			
		} catch (Exception e) {
			Gbl.errorMsg(e);
		}
	}
	
	private Collection<Feature> generateSHPFileData(CoordinateReferenceSystem crs, Network network, boolean absolute, boolean ignoreExitLinks) throws Exception {

		GeometryFactory geoFac = new GeometryFactory();
		Collection<Feature> features = new ArrayList<Feature>();
		
		AttributeType geom = DefaultAttributeTypeFactory.newAttributeType("LineString", Geometry.class, true, null, null, crs);
		AttributeType id = AttributeTypeFactory.newAttributeType("ID", String.class);
		AttributeType fromNode = AttributeTypeFactory.newAttributeType("fromID", String.class);
		AttributeType toNode = AttributeTypeFactory.newAttributeType("toID", String.class);
		AttributeType length = AttributeTypeFactory.newAttributeType("length", Double.class);
		AttributeType type = AttributeTypeFactory.newAttributeType("fstt", Double.class);
		
		AttributeType[] attributes = new AttributeType[6 + this.numSlots];
		attributes[0] = geom;
		attributes[1] = id;
		attributes[2] = fromNode;
		attributes[3] = toNode;
		attributes[4] = length;
		attributes[5] = type;
		for (int i = 0; i < this.numSlots; i++) {
			AttributeType slotTravelTime = AttributeTypeFactory.newAttributeType(String.valueOf(i * this.timeSlice), Double.class);
			attributes[6 + i] = slotTravelTime;
		}
		
		FeatureType ftRoad = FeatureTypeBuilder.newFeatureType(attributes, "link");
		for (Link link : network.getLinks().values()) {
			
			if (ignoreExitLinks) {
				String string = link.getId().toString().toLowerCase();
				if (string.contains("rescue") || string.contains("exit")) continue;
			}
			
			Coordinate[] coordArray = new Coordinate[] {coord2Coordinate(link.getFromNode().getCoord()), coord2Coordinate(link.getCoord()), coord2Coordinate(link.getToNode().getCoord())};
			LineString ls = new LineString(new CoordinateArraySequence(coordArray), geoFac);

			Object[] object = new Object[attributes.length];
			object[0] = ls;
			object[1] = link.getId().toString();
			object[2] = link.getFromNode().getId().toString();
			object[3] = link.getToNode().getId().toString();
			object[4] = link.getLength();
			object[5] = link.getLength()/link.getFreespeed();

//			System.out.println(link.getId().toString());
			double[] travelTimeArray = this.travelTimes.get(link);
			for (int i = 0; i < this.numSlots; i++) {
				
				if (absolute) {
					object[6 + i] = travelTimeArray[i];
				} else {
					double freeSpeedTravelTime = link.getLength() / link.getFreespeed(i * this.timeSlice);
					double relativeTravelTime = travelTimeArray[i] / freeSpeedTravelTime;
//					if (relativeTravelTime > 50) {
//						Log.warn("bla");
//					} else if (relativeTravelTime == Double.NaN) {
//						Log.warn("bla");
//					} else if (relativeTravelTime == Double.POSITIVE_INFINITY) {
//						Log.warn("bla");
//					} else if (relativeTravelTime == Double.NEGATIVE_INFINITY) {
//						Log.warn("bla");
//					}
					object[6 + i] = relativeTravelTime;
				}
			}

			Feature ft = ftRoad.create(object, "links");
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
}