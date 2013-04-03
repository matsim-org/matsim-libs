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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.gis.PolygonFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsData;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;


/**
 * 
 * 
 * @author dgrether
 *
 */
public class DgSignalsBoundingBox {

	private static final Logger log = Logger.getLogger(DgSignalsBoundingBox.class);

	private Envelope boundingBox;

	private CoordinateReferenceSystem networkSrs;

	public DgSignalsBoundingBox(CoordinateReferenceSystem crs){
		this.networkSrs = crs;
	}
	
	public SimpleFeature calculateBoundingBoxForSignals(Network net, SignalSystemsData signalSystemsData, double offset){
		//get all signalized link ids
		Map<Id, Set<Id>> signalizedLinkIdsBySystemIdMap = DgSignalsUtils.calculateSignalizedLinksPerSystem(signalSystemsData); 
		Set<Id> signalizedLinkIds = new HashSet<Id>();
		for (Set<Id> set : signalizedLinkIdsBySystemIdMap.values()){
			signalizedLinkIds.addAll(set);
		}
		SimpleFeature boundingboxFeature = calcBoundingBox(net, signalizedLinkIds, offset);
		return boundingboxFeature;
	}
	
	private SimpleFeature calcBoundingBox(Network net, Set<Id> signalizedLinkIds, double offset) {
		Link l = null;
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		for (Id linkId : signalizedLinkIds){
			l = net.getLinks().get(linkId);
			if (l.getCoord().getX() < minX) {
				minX = l.getCoord().getX();
			}
			if (l.getCoord().getX() > maxX) {
				maxX = l.getCoord().getX();
			}
			if (l.getCoord().getY() > maxY) {
				maxY = l.getCoord().getY();
			}
			if (l.getCoord().getY() < minY) {
				minY = l.getCoord().getY();
			}
		}

		log.info("Found bounding box: "  + minX + " " + minY + " " + maxX + " " + maxY);

		minX = minX - offset;
		minY = minY - offset;
		maxX = maxX + offset;
		maxY = maxY + offset;
		log.info("Found bounding box: "  + minX + " " + minY + " " + maxX + " " + maxY + " offset used: " + offset);

		Coordinate[] coordinates = new Coordinate[5];
		coordinates[0] = new Coordinate(minX, minY);
		coordinates[1] = new Coordinate(minX, maxY);
		coordinates[2] = new Coordinate(maxX, maxY);
		coordinates[3] = new Coordinate(maxX, minY);
		coordinates[4] = coordinates[0];


		this.boundingBox = new Envelope(coordinates[0], coordinates[2]);
		
		PolygonFeatureFactory factory = new PolygonFeatureFactory.Builder().
				setCrs(networkSrs).
				setName("link").
				create();
		return factory.createPolygon(coordinates);
	}
	
	public void writeBoundingBox(SimpleFeature ft, String outputDirectory){
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
