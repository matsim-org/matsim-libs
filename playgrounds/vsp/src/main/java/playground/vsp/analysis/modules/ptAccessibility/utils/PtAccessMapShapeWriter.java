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
import java.util.LinkedList;
import java.util.Map;
import java.util.ServiceConfigurationError;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;

import playground.vsp.analysis.modules.ptAccessibility.stops.PtStopMap;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;

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
		ArrayList<Integer> distances = new ArrayList<Integer>();
		for (String distanceString : cluster2mode2area.keySet()) {
			distances.add(Integer.parseInt(distanceString));
		}
		Collections.sort(distances);

		ArrayList<Geometry> buffersSmallestFirst = new ArrayList<Geometry>();
		
		// Calculate buffer for all Multipolygons
		Geometry lastBuffer = null;
		int lastDistance = 0;
		
		for (Integer distance : distances) {
			if (lastBuffer == null) {
				// it's the frist and smallest one
				for (Geometry multipolygon : cluster2mode2area.get(distances.get(0).toString()).values()) {
					lastBuffer = multipolygon.buffer(0.0, quadrantSegments);
					lastDistance = distance.intValue();
				}
			} else {
				lastBuffer = lastBuffer.buffer(distance.intValue() - lastDistance);
				lastDistance = distance.intValue();
			}
			
			buffersSmallestFirst.add(lastBuffer);
		}
		
		writeGeometries(outputFolder + PtStopMap.FILESUFFIX + "_buffer", distances, buffersSmallestFirst, targetCoordinateSystem);
		
		// resort
		LinkedList<Geometry> buffersLargestFirst = new LinkedList<Geometry>();
		for (int i = 0; i < buffersSmallestFirst.size(); i++) {
			buffersLargestFirst.addFirst(buffersSmallestFirst.get(i));
		}
		
		// calculate Diff for all buffers
		LinkedList<Geometry> diffBuffers = new LinkedList<Geometry>();
		lastBuffer = null;
		
		for (Geometry buffer : buffersLargestFirst) {
			if (lastBuffer == null) {
				lastBuffer = buffer;
			} else {
				// diff
				Geometry diffBuffer = lastBuffer.difference(buffer);
				diffBuffers.addFirst(diffBuffer);
				lastBuffer = buffer;
			}
		}
		// add last (smallest) one as well
		diffBuffers.addFirst(buffersLargestFirst.get(buffersLargestFirst.size() - 1));
		
		// repack
		ArrayList<Geometry> buffersToWrite = new ArrayList<Geometry>();
		for (Geometry geometry : diffBuffers) {
			buffersToWrite.add(geometry);
		}
		
		writeGeometries(outputFolder + PtStopMap.FILESUFFIX + "_diffBuffer", distances, buffersToWrite, targetCoordinateSystem);
	}

	private static void writeGeometries(String outputFolderAndFileName, ArrayList<Integer> distances, ArrayList<Geometry> geometries, String targetCoordinateSystem) {
		// write all to file
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setCRS(MGC.getCRS(targetCoordinateSystem));
		b.setName("name");
		b.add("location", MultiPolygon.class);
		b.add("name", String.class);
		SimpleFeatureBuilder builder = new SimpleFeatureBuilder(b.buildFeatureType());
		
		Collection<SimpleFeature> bufferFeatures = new ArrayList<SimpleFeature>();
		Object[] bufferFeatureAttribs;
		
		for (int i = 0; i < geometries.size(); i++) {
			Geometry geometry = geometries.get(i);

			bufferFeatures = new ArrayList<SimpleFeature>();
			bufferFeatureAttribs = new Object[2];
			bufferFeatureAttribs[0] = geometry;
			String distance = distances.get(i).toString();
			bufferFeatureAttribs[1] = distance;
			try {
				bufferFeatures.add(builder.buildFeature(null, bufferFeatureAttribs));
			} catch (IllegalArgumentException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			try{
				ShapeFileWriter.writeGeometries(bufferFeatures, outputFolderAndFileName + "_" + distance + ".shp");
			}catch(ServiceConfigurationError e){
				e.printStackTrace();
			}
		}
		
	}
}
