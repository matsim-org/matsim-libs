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

package playground.johannes.gsv.gis;

import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.common.gis.CRSUtils;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.LineString;
import org.wololo.jts2geojson.GeoJSONWriter;
import playground.johannes.gsv.sim.LinkOccupancyCalculator;
import playground.johannes.gsv.sim.cadyts.ODCalibrator;
import playground.johannes.sna.graph.spatial.io.ColorUtils;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author johannes
 * 
 */
public class NetworkLoad2GeoJSON {

	public static void write(Network network, LinkOccupancyCalculator linkVols, double factor, String file) {
		Map<Link, Double> volumes = new HashMap<>(network.getLinks().size());
		Map<Link, Double> loads = new HashMap<>(network.getLinks().size());

		double maxVolume = 0;
		double minVolume = Double.MAX_VALUE;

		for (Link link : network.getLinks().values()) {
			String linkId = link.getId().toString();
			if (!(linkId.startsWith(ODCalibrator.VIRTUAL_ID_PREFIX) || linkId.contains(".l"))) {
				double vol = linkVols.getOccupancy(link.getId()) * factor;
				double load = vol / (link.getCapacity() * 24);
				volumes.put(link, vol);
				loads.put(link, load);

				maxVolume = Math.max(maxVolume, vol);
				minVolume = Math.min(minVolume, vol);
			}
		}

		MathTransform transform = null;
		try {
			transform = CRS.findMathTransform(CRSUtils.getCRS(31467), CRSUtils.getCRS(4326));
		} catch (FactoryException e) {
			e.printStackTrace();
		}

		GeoJSONWriter jsonWriter = new GeoJSONWriter();
		List<Feature> features = new ArrayList<>(network.getLinks().size());

		for (Link link : volumes.keySet()) {
			double coord1[] = new double[] {link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY()};
			double coord2[] = new double[] {link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY()};
			
			try {
				transform.transform(coord1, 0, coord1, 0, 1);
				transform.transform(coord2, 0, coord2, 0, 1);
			} catch (TransformException e) {
				e.printStackTrace();
			}
			
			double coords[][] = new double[2][2];
			coords[0][0] = coord1[0];
			coords[0][1] = coord1[1];
			coords[1][0] = coord2[0];
			coords[1][1] = coord2[1];

			LineString lineString = new LineString(coords);

			Map<String, Object> properties = new HashMap<>();
			double volume = volumes.get(link);
			double load = loads.get(link);
			properties.put("volume", volume);
			properties.put("load", load);
			properties.put("capacity", link.getCapacity());
			double width = (volume - minVolume) / (maxVolume - minVolume);
			properties.put("width", width);
			Color color = ColorUtils.getRedGreenColor(load);
			properties.put("color", "#" + String.format("%06x", color.getRGB() & 0x00FFFFFF));

			Feature feature = new Feature(lineString, properties);
			features.add(feature);
		}
		
		try {
			FeatureCollection fCollection = jsonWriter.write(features);
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			writer.write(fCollection.toString());
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
