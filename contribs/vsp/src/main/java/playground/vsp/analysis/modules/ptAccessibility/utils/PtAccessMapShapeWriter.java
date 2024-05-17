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
package playground.vsp.analysis.modules.ptAccessibility.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceConfigurationError;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.GeoFileWriter;

import playground.vsp.analysis.modules.ptAccessibility.stops.PtStopMap;

/**
 * Calculates and writes buffers to shapes
 *
 * @author aneumann
 */
public class PtAccessMapShapeWriter {

	private PtAccessMapShapeWriter() {

	}

	public static void writeAccessMap(Map<String, Map<String, MultiPolygon>> cluster2mode2area, int quadrantSegments, String outputFolder, String targetCoordinateSystem) {
		// Sort distance clusters
		ArrayList<Integer> distancesSmallestFirst = new ArrayList<Integer>();
		for (String distanceString : cluster2mode2area.keySet()) {
			distancesSmallestFirst.add(Integer.parseInt(distanceString));
		}
		Collections.sort(distancesSmallestFirst);

		HashMap<Integer, HashMap<String, Geometry>> distance2mode2buffer = new HashMap<Integer, HashMap<String, Geometry>>();

		// Calculate buffer for all Multipolygons
		HashMap<String, Geometry> mode2buffer = null;
		int lastDistance = 0;

		for (Integer distance : distancesSmallestFirst) {
			if (mode2buffer == null) {
				// it's the frist and smallest one
				mode2buffer = new HashMap<String, Geometry>();
				for (Entry<String, MultiPolygon> multipolygonEntry : cluster2mode2area.get(distancesSmallestFirst.get(0).toString()).entrySet()) {
					mode2buffer.put(multipolygonEntry.getKey(), multipolygonEntry.getValue().buffer(0.0, quadrantSegments));
					lastDistance = distance.intValue();
				}
			} else {
				HashMap<String, Geometry> tempBuffers = new HashMap<String, Geometry>();
				for (Entry<String, Geometry> bufferEntry : mode2buffer.entrySet()) {
					tempBuffers.put(bufferEntry.getKey(), bufferEntry.getValue().buffer(distance.intValue() - lastDistance));
				}
				mode2buffer = tempBuffers;
				lastDistance = distance.intValue();
			}

			distance2mode2buffer.put(distance, mode2buffer);
		}

		writeGeometries(outputFolder + PtStopMap.FILESUFFIX + "_buffer", distance2mode2buffer, targetCoordinateSystem);



		// resort distances - largest first
		ArrayList<Integer> distancesLargestFirst = new ArrayList<Integer>();
		for (Integer distance : distancesSmallestFirst) {
			distancesLargestFirst.add(0, distance);
		}


		HashMap<Integer, HashMap<String, Geometry>> distance2mode2diffBuffer = new HashMap<Integer, HashMap<String, Geometry>>();
		HashMap<String, Geometry> lastMode2Buffer = null;
		Integer lastDist = null;

		// calculate Diff for all buffers
		for (Integer distance : distancesLargestFirst) {
			distance2mode2diffBuffer.put(distance, new HashMap<String, Geometry>());

			if (lastMode2Buffer == null) {
				lastMode2Buffer = distance2mode2buffer.get(distance);
				lastDist = distance;
			} else {
				// diff
				for (String mode : distance2mode2buffer.get(distance).keySet()) {
					Geometry diffBuffer = lastMode2Buffer.get(mode).difference(distance2mode2buffer.get(distance).get(mode));
					distance2mode2diffBuffer.get(lastDist).put(mode, diffBuffer);
				}
				lastMode2Buffer = distance2mode2buffer.get(distance);
				lastDist = distance;
			}
		}

		// add last (smallest) one as well
		for (Entry<String, Geometry> mode2BufferEntry : lastMode2Buffer.entrySet()) {
			distance2mode2diffBuffer.get(lastDist).put(mode2BufferEntry.getKey(), mode2BufferEntry.getValue());
		}

		writeGeometries(outputFolder + PtStopMap.FILESUFFIX + "_diffBuffer", distance2mode2diffBuffer, targetCoordinateSystem);
	}

	private static void writeGeometries(String outputFolderAndFileName, HashMap<Integer, HashMap<String, Geometry>> distance2mode2buffer, String targetCoordinateSystem) {
		// write all to file
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setCRS(MGC.getCRS(targetCoordinateSystem));
		b.setName("name");
		b.add("location", MultiPolygon.class);
		b.add("mode", String.class);
		SimpleFeatureBuilder builder = new SimpleFeatureBuilder(b.buildFeatureType());

		Collection<SimpleFeature> bufferFeatures;
		Object[] bufferFeatureAttribs;

		for (Entry<Integer, HashMap<String, Geometry>> distance2mode2bufferEntry : distance2mode2buffer.entrySet()) {
			bufferFeatures = new ArrayList<>();
			HashMap<String, Geometry> mode2buffer = distance2mode2bufferEntry.getValue();
			for (Entry<String, Geometry> mode2BufferEntry : mode2buffer.entrySet()) {
				bufferFeatureAttribs = new Object[2];
				bufferFeatureAttribs[0] = mode2BufferEntry.getValue();
				bufferFeatureAttribs[1] = mode2BufferEntry.getKey();
				try {
					bufferFeatures.add(builder.buildFeature(null, bufferFeatureAttribs));
				} catch (IllegalArgumentException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}

			try{
				GeoFileWriter.writeGeometries(bufferFeatures, outputFolderAndFileName + "_" + distance2mode2bufferEntry.getKey() + ".shp");
			}catch(ServiceConfigurationError e){
				e.printStackTrace();
			}
		}

	}
}
