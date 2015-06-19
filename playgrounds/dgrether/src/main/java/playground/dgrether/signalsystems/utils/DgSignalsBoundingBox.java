/* *********************************************************************** *
 * project: org.matsim.*
 * DgSignalsBoundingBox
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.dgrether.signalsystems.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.gis.PolygonFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;


/**
 * 
 * Provides functionality to create a bounding box around all nodes that are controlled by Traffic Signals.
 * @author dgrether
 *
 */
public class DgSignalsBoundingBox {

	private static final Logger log = Logger.getLogger(DgSignalsBoundingBox.class);

	private Envelope boundingBox;

	private CoordinateReferenceSystem networkSrs;

	private Coordinate[] coordinates;

	public DgSignalsBoundingBox(CoordinateReferenceSystem crs){
		this.networkSrs = crs;
	}
	
	public Envelope calculateBoundingBoxForSignals(Network net, SignalSystemsData signalSystemsData, double offset){
		//get all signalized link ids
		Set<Id<Node>> signalizedNodeIds = DgSignalsUtils.calculateSignalizedNodes(signalSystemsData, net);
		calcBoundingBox(net, signalizedNodeIds, offset);
		return this.boundingBox;
	}
	
	private void calcBoundingBox(Network net, Set<Id<Node>> signalizedNodeIds, double offset) {
		Node n = null;
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		for (Id<Node> nodeId : signalizedNodeIds){
			n = net.getNodes().get(nodeId);
			if (n.getCoord().getX() < minX) {
				minX = n.getCoord().getX();
			}
			if (n.getCoord().getX() > maxX) {
				maxX = n.getCoord().getX();
			}
			if (n.getCoord().getY() > maxY) {
				maxY = n.getCoord().getY();
			}
			if (n.getCoord().getY() < minY) {
				minY = n.getCoord().getY();
			}
		}

		log.info("Found bounding box: "  + minX + " " + minY + " " + maxX + " " + maxY);

		minX = minX - offset;
		minY = minY - offset;
		maxX = maxX + offset;
		maxY = maxY + offset;
		log.info("Found bounding box: "  + minX + " " + minY + " " + maxX + " " + maxY + " offset used: " + offset);

		this.coordinates = new Coordinate[5];
		coordinates[0] = new Coordinate(minX, minY);
		coordinates[1] = new Coordinate(minX, maxY);
		coordinates[2] = new Coordinate(maxX, maxY);
		coordinates[3] = new Coordinate(maxX, minY);
		coordinates[4] = coordinates[0];


		this.boundingBox = new Envelope(coordinates[0], coordinates[2]);
		
	}
	
	public void writeBoundingBox(String outputDirectory){
		PolygonFeatureFactory factory = new PolygonFeatureFactory.Builder().
				setCrs(networkSrs).
				setName("boundingbox").
				create();
		SimpleFeature ft = factory.createPolygon(coordinates);
		Collection<SimpleFeature> boundingBoxCollection = new ArrayList<SimpleFeature>();
		boundingBoxCollection.add(ft);
		ShapeFileWriter.writeGeometries(boundingBoxCollection, outputDirectory + "bounding_box.shp");
	}

	public CoordinateReferenceSystem getCrs(){
		return this.networkSrs;
	}

	public Envelope getBoundingBox() {
		return this.boundingBox;
	}

	
}
