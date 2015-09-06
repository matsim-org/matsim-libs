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

package playground.boescpa.converters.vissim.tools;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import playground.boescpa.converters.vissim.ConvEvents;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Creates a square grid around considered area. The grid is represented by nodes in the network.
 *
 * @author boescpa
 */
public class BaseGridCreator implements ConvEvents.BaseGridCreator {
	/**
	 * Side length [m] of one square of the grid.
	 * Default value: 100
	 */
	private static int gridcellsize = 100;
	public static void setGridcellsize(int newSize) {
		gridcellsize = newSize;
	}
	public static int getGridcellsize() {
		return gridcellsize;
	}

	/**
	 * Finds the most western, the most northern, the most eastern and the most southern point of the zones given.
	 * 	[CH1903/LV03-Coordinates expected]
	 * Creates nodes covering this area. Each node represents a square cell.
	 *
	 * @param path2ZonesFile Shp-File with the zones to consider as the area.
	 * @return Square-Grid covering the squares.
	 */
	@Override
	public Network createMutualBaseGrid(String path2ZonesFile) {

		Network mutualRepresentation = NetworkUtils.createNetwork();
		NetworkFactoryImpl networkFactory = new NetworkFactoryImpl(mutualRepresentation);
		Long[] sides = boundingBoxOfZones(path2ZonesFile);

		long maxLongitude = sides[1];
		long maxLatitude = sides[3];
		long cellId = 1;

		long latitude = sides[2];
		do {
			long longitude = sides[0];
			do {
				mutualRepresentation.addNode(networkFactory.createNode(Id.create(cellId++, Node.class), new Coord((double) longitude, (double) latitude)));
				longitude += gridcellsize;
			} while (longitude < (maxLongitude + gridcellsize));
			latitude += gridcellsize;
		} while (latitude < (maxLatitude + gridcellsize));

		return mutualRepresentation;
	}

	/**
	 * Unites the provided zones and finds the bounding box of the zones.
	 *
	 * @param path2ZonesFile
	 * @return The Long-Array has four values:
	 *	Long[0] = min longitude of shp-file
	 *	Long[1] = max longitude of shp-file
	 *	Long[2] = min latitude of shp-file
	 *	Long[3] = max latitude of shp-file
	 */
	protected Long[] boundingBoxOfZones(String path2ZonesFile) {
		Set<SimpleFeature> features = new HashSet<SimpleFeature>();
		features.addAll(ShapeFileReader.getAllFeatures(path2ZonesFile));
		Long[] boundings = new Long[4];
		boolean first = true;
		for (SimpleFeature feature : features) {
			Geometry geometry = (Geometry) feature.getDefaultGeometry();
			Envelope boundingBox = geometry.getEnvelopeInternal();
			if (first) {
				boundings[0] = (long) boundingBox.getMinX();
				boundings[1] = (long) boundingBox.getMaxX();
				boundings[2] = (long) boundingBox.getMinY();
				boundings[3] = (long) boundingBox.getMaxY();
				first = false;
			} else {
				if (boundings[0] > (long) boundingBox.getMinX()) boundings[0] = (long) boundingBox.getMinX();
				if (boundings[1] < (long) boundingBox.getMaxX()) boundings[1] = (long) boundingBox.getMaxX();
				if (boundings[2] > (long) boundingBox.getMinY()) boundings[2] = (long) boundingBox.getMinY();
				if (boundings[3] < (long) boundingBox.getMaxY()) boundings[3] = (long) boundingBox.getMaxY();
			}
		}
		return boundings;
	}
}
