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

import com.vividsolutions.jts.geom.Point;
import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.common.gis.CRSUtils;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.Volume;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.jts2geojson.GeoJSONWriter;
import playground.johannes.coopsim.util.MatsimCoordUtils;
import playground.johannes.gsv.sim.LinkOccupancyCalculator;
import playground.johannes.gsv.sim.cadyts.ODCalibrator;
import playground.johannes.sna.graph.spatial.io.ColorUtils;

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
public class CountsCompare2GeoJSON {

	public static void write(LinkOccupancyCalculator simCounts, Counts<Link> obsCounts, double factor, Network network, String outDir) {
		MathTransform transform = null;
		try {
			transform = CRS.findMathTransform(CRSUtils.getCRS(31467), CRSUtils.getCRS(4326));
		} catch (FactoryException e) {
			e.printStackTrace();
		}

		GeoJSONWriter jsonWriter = new GeoJSONWriter();
		List<Feature> simFeatures = new ArrayList<>(obsCounts.getCounts().size());
		List<Feature> obsFeatures = new ArrayList<>(obsCounts.getCounts().size());

		for (Count<Link> count : obsCounts.getCounts().values()) {
			Id<Link> linkId = count.getLocId();
			if (!linkId.toString().startsWith(ODCalibrator.VIRTUAL_ID_PREFIX)) {
				Link link = network.getLinks().get(linkId);

				double simVal = simCounts.getOccupancy(linkId) * factor;
				double obsVal = 0;
				for (Volume vol : count.getVolumes().values()) {
					obsVal += vol.getValue();
				}

				double relErr = (simVal - obsVal) / obsVal;

				Coord simPos = link.getCoord();
				Coord obsPos = count.getCoord();

				Point simPoint = MatsimCoordUtils.coordToPoint(simPos);
				Point obsPoint = MatsimCoordUtils.coordToPoint(obsPos);

				simPoint = CRSUtils.transformPoint(simPoint, transform);
				obsPoint = CRSUtils.transformPoint(obsPoint, transform);

				Map<String, Object> properties = new HashMap<>();

				properties.put("simulation", simVal);
				properties.put("observation", obsVal);
				properties.put("error", relErr);
				properties
						.put("color", "#" + String.format("%06x", ColorUtils.getRedGreenColor(Math.min(1, Math.abs(relErr))).getRGB() & 0x00FFFFFF));

				Feature obsFeature = new Feature(jsonWriter.write(obsPoint), properties);
				Feature simFeature = new Feature(jsonWriter.write(simPoint), properties);

				simFeatures.add(simFeature);
				obsFeatures.add(obsFeature);
			}
		}

		FeatureCollection simFCollection = jsonWriter.write(simFeatures);
		FeatureCollection obsFCollection = jsonWriter.write(obsFeatures);

		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(String.format("%s/simCounts.json", outDir)));
			writer.write(simFCollection.toString());
			writer.close();

			writer = new BufferedWriter(new FileWriter(String.format("%s/obsCounts.json", outDir)));
			writer.write(obsFCollection.toString());
			writer.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
