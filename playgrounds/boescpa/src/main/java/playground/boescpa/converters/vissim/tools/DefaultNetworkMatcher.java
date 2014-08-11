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

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import com.vividsolutions.jts.util.GeometricShapeFactory;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import playground.boescpa.converters.vissim.ConvEvents2Anm;
import playground.wrashid.msimoni.analyses.*;

import java.util.*;

/**
 * Creates a square grid around considered area. The grid is represented by nodes in the network.
 *
 * @author boescpa
 */
public class DefaultNetworkMatcher implements ConvEvents2Anm.NetworkMatcher {

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
				mutualRepresentation.addNode(networkFactory.createNode(new IdImpl(cellId++), new CoordImpl(longitude, latitude)));
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

	@Override
	public HashMap<Id, Id[]> mapMsNetwork(String path2MATSimNetwork, Network mutualBaseGrid, String path2VissimZoneShp) {
		Network network = readAndCutMsNetwork(path2MATSimNetwork, path2VissimZoneShp);
		Map<Id, Geometry> zones = prepareMutualBaseGrid(mutualBaseGrid);
		HashMap<Id, Id[]> mapKey = new HashMap<Id, Id[]>();
		// follow all links and check which "zones" of mutual base grid are passed
		for (Link link : network.getLinks().values()) {
			List<Id> passedZones = new LinkedList<Id>();
			Coordinate start = new Coordinate(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY());
			Coordinate end = new Coordinate(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY());
			double[] deltas = calculateDeltas(start, end);
			for (int i = 0; i < (int)deltas[2]; i++) {
				Point point = new Point(new CoordinateArraySequence(new Coordinate[]{new Coordinate(i * deltas[0], i * deltas[1])}), new GeometryFactory());
				for (Id zoneId : zones.keySet()) {
					Geometry zone = zones.get(zoneId);
					if (zone.contains(point)) {
						if (passedZones.get(passedZones.size()-1) != zoneId) {
							passedZones.add(zoneId);
						}
						break;
					}
				}
			}
			/*Geometry vividLink = new LineString(new CoordinateArraySequence(new Coordinate[]{start, end}), new GeometryFactory());
			for (Id zoneId : zones.keySet()) {
				Geometry zone = zones.get(zoneId);
				if (vividLink.crosses(zone)) {
					passedZones.add(Long.parseLong(zoneId.toString()));
				}
			}*/
			mapKey.put(link.getId(), passedZones.toArray(new Id[]{}));
		}
		return mapKey;
	}

	private double[] calculateDeltas(Coordinate start, Coordinate end) {
		double factor = 1;
		double[] delta = new double[3];
		do {
			factor *= 10;
			delta[0] = Math.abs((start.x - end.x)/factor);
			delta[1] = Math.abs((start.y - end.y)/factor);
		} while (delta[0] >= (gridcellsize/10) && delta[1] >= (gridcellsize/10));
		delta[0] = (start.x - end.x)/factor;
		delta[1] = (start.y - end.y)/factor;
		delta[2] = factor;
		return delta;
	}

	/**
	 * Creates a square around each node.
	 *
	 * @param mutualBaseGrid
	 * @return Zones derived from the mutualBaseGrid.
	 */
	private Map<Id, Geometry> prepareMutualBaseGrid(Network mutualBaseGrid) {
		Map<Id, Geometry> zones = new HashMap<Id, Geometry>();
		GeometricShapeFactory factory = new GeometricShapeFactory();
		factory.setHeight(gridcellsize);
		factory.setWidth(gridcellsize);
		for (Node node : mutualBaseGrid.getNodes().values()) {
			factory.setCentre(new Coordinate(node.getCoord().getX(), node.getCoord().getY()));
			zones.put(node.getId(), factory.createRectangle());
		}
		return zones;
	}

	/**
	 * Read network and cut it to zones.
	 *
	 * @param path2MATSimNetwork
	 * @param path2VissimZoneShp
	 * @return
	 */
	protected Network readAndCutMsNetwork(String path2MATSimNetwork, String path2VissimZoneShp) {
		return null;
	}

	@Override
	public HashMap<Id, Long[]> mapAmNetwork(String path2VissimNetworkLinks, Network mutualBaseGrid) {
		return null;
	}
}
