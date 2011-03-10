/* *********************************************************************** *
 * project: org.matsim.*
 * Trips2QGISRigorous.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.yu.integration.cadyts.demandCalibration.withCarCounts.utils.qgis;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.Feature;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.yu.integration.cadyts.demandCalibration.withCarCounts.BseLinkCostOffsetsXMLFileIO;
import playground.yu.integration.cadyts.demandCalibration.withCarCounts.experiments.actLocUtilOffset.TripUtilOffsetExtractor;
import playground.yu.integration.cadyts.demandCalibration.withCarCounts.experiments.actLocUtilOffset.TripUtilOffsetExtractor.TripsWithUtilOffset;
import playground.yu.utils.qgis.X2QGIS;
import playground.yu.utils.qgis.MATSimNet2QGIS.ShapeFileWriter2;
import cadyts.utilities.misc.DynamicData;

public class Trips2QGISNormal implements X2QGIS {
	private CoordinateReferenceSystem crs;
	private Trips2PolygonGraphNormal t2g;

	public Trips2QGISNormal(
			String coordRefSys,
			Map<Tuple<Coord, Coord>, TripsWithUtilOffset> childTripsWithUtilOffsetMap) {
		crs = MGC.getCRS(coordRefSys);
		t2g = new Trips2PolygonGraphNormal(childTripsWithUtilOffsetMap, crs);
	}

	public Trips2QGISNormal(
			String coordRefSys,
			Map<Tuple<Coord, Coord>, TripsWithUtilOffset> childTripsWithUtilOffsetMap,
			int tripBundleLowLimit) {
		this(coordRefSys, childTripsWithUtilOffsetMap);
		t2g.setTripBundleLowLimit(tripBundleLowLimit);
	}

	public void setBarWidthScale(double barWidthScale) {
		t2g.setBarWidthScale(barWidthScale);
	}

	/**
	 * @param ShapeFilename
	 *            where the shapefile will be saved
	 */
	public void writeShapeFile(final String ShapeFilename) {
		try {
			Collection<Feature> features = t2g.getFeatures();
			if (features.size() > 0) {
				ShapeFileWriter2.writeGeometries(features, ShapeFilename);
			}
		} catch (FactoryRegistryException e) {
			e.printStackTrace();
		} catch (SchemaException e) {
			e.printStackTrace();
		} catch (IllegalAttributeException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (FactoryException e) {
			e.printStackTrace();
		}
	}

	public static void main(String args[]) {
		String linkOffsetUtilOffsetFilename = "test/DestinationUtilOffset/1000.linkCostOffsets.xml"//
		, networkFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml"//
		, countsFilename = "../schweiz-ivtch-SVN/baseCase/counts/countsIVTCH.xml"//
		, eventsFilename = "test/DestinationUtilOffset/1000.events.txt.gz"//
		, outputFilenameBase = "test/DestinationUtilOffset2/1000.destUtiloffset."//
		;

		int arStartTime = 7, arEndTime = 20, lowerLimit = 50;
		double gridLength = 1000d;

		Scenario scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network net = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(networkFilename);

		Counts counts = new Counts();
		new MatsimCountsReader(counts).readFile(countsFilename);

		BseLinkCostOffsetsXMLFileIO utilOffsetIO = new BseLinkCostOffsetsXMLFileIO(
				net);
		DynamicData<Link> linkUtilOffsets = utilOffsetIO
				.read(linkOffsetUtilOffsetFilename);
		TripUtilOffsetExtractor tuoExtractor = new TripUtilOffsetExtractor(
				counts, net, linkUtilOffsets, gridLength, arStartTime,
				arEndTime);

		EventsManager events = new EventsManagerImpl();
		// /////////////////////////////////
		events.addHandler(tuoExtractor);
		// /////////////////////////////////
		new MatsimEventsReader(events).readFile(eventsFilename);

		// for (int i = arStartTime; i <= arEndTime; i++) {
		// Map<Tuple<Coord, Coord>, TripsWithUtilOffset>
		// childTripsWithUtilOffsetMap = tuoExtractor
		// .getChildTripsWithUtilOffsetMap(i);
		// int size = childTripsWithUtilOffsetMap.size();
		// System.out.println("timeStep\t" + i + "\tsize\t" + size);
		// if (size > 0) {
		Trips2QGISNormal trips2qgis = new Trips2QGISNormal(ch1903,
		// childTripsWithUtilOffsetMap
				tuoExtractor.getTripsWithUtilOffsetMap(), 5);
		trips2qgis.setBarWidthScale(10);
		trips2qgis.writeShapeFile(outputFilenameBase + "tripsWithUtilOffset" +
		// "."+i +
				".shp");
		// }
		// }
	}
}
