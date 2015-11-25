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

package playground.boescpa.analysis.trips.tripCreation.spatialCuttings;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import java.util.HashSet;
import java.util.Set;

/**
 * Spatial cutting strategy for trip processing.
 * 
 * Shp File returns TRUE for all trips with start and/or end link
 * inside the shape provided with the "cuttingShpFile".
 * 
 * @author pboesch
 */
public class ShpFileCutting implements SpatialCuttingStrategy {

	// TODO-boescpa Write tests...

	private static final GeometryFactory factory = new GeometryFactory();
	private Geometry area = null;

	public ShpFileCutting(String cuttingShpFile) {
		Set<SimpleFeature> features = new HashSet<SimpleFeature>();
		features.addAll(ShapeFileReader.getAllFeatures(cuttingShpFile));
		for (SimpleFeature feature : features) {
			if (this.area == null) {
				this.area = (Geometry) feature.getDefaultGeometry();
			} else {
				this.area = this.area.union((Geometry) feature.getDefaultGeometry());
			}
		}
	}

	@Override
	public boolean spatiallyConsideringTrip(Network network, Id startLink, Id endLink) {
		boolean contains = false;

		// Check startLink
		double startXCoord = network.getLinks().get(startLink).getCoord().getX();
		double startYCoord = network.getLinks().get(startLink).getCoord().getY();
		Point start = factory.createPoint(new Coordinate(startXCoord, startYCoord));
		contains = area.contains(start);

		// Check endLink
		if (!contains && endLink != null) {
			double endXCoord = network.getLinks().get(endLink).getCoord().getX();
			double endYCoord = network.getLinks().get(endLink).getCoord().getY();
			Point end = factory.createPoint(new Coordinate(endXCoord, endYCoord));
			contains = area.contains(end);
		}

		return contains;
	}

}
