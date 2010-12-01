/* *********************************************************************** *
 * project: org.matsim.*
 * Trips2PolygonGraphRigorous.java
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

/**
 * 
 */
package playground.yu.integration.cadyts.demandCalibration.withCarCounts.utils.qgis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.DefaultFeatureTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.Tuple;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.yu.integration.cadyts.demandCalibration.withCarCounts.experiments.actLocUtilOffset.TripUtilOffsetExtractor.TripsWithUtilOffset;
import playground.yu.utils.qgis.X2GraphImpl;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

/**
 * trip/{@code Leg} can be painted as an arrow, route will not painted by this
 * class
 * 
 * @author yu
 * 
 */
public class Trips2PolygonGraphRigorous extends X2GraphImpl {
	private Map<Tuple<Coord, Coord>, TripsWithUtilOffset> tripsWithUtilOffsetMap;
	private int tripBundleLowLimit = 1;
	private double barWidthScale = 5;

	public Trips2PolygonGraphRigorous(
			Map<Tuple<Coord, Coord>, TripsWithUtilOffset> tripsWithUtilOffsetMap,
			CoordinateReferenceSystem crs, int tripBundleLowLimit) {
		this(tripsWithUtilOffsetMap, crs);
		this.tripBundleLowLimit = tripBundleLowLimit;
	}

	public Trips2PolygonGraphRigorous(
			Map<Tuple<Coord, Coord>, TripsWithUtilOffset> childTripsWithUtilOffsetMap,
			CoordinateReferenceSystem crs) {
		tripsWithUtilOffsetMap = childTripsWithUtilOffsetMap;
		this.crs = crs;

		geofac = new GeometryFactory();

		features = new ArrayList<Feature>();

		AttributeType geom = DefaultAttributeTypeFactory.newAttributeType(
				"MultiPolygon", MultiPolygon.class, true, null, null, this.crs);
		AttributeType ODcoord = AttributeTypeFactory.newAttributeType(
				"ODcoord", String.class);
		AttributeType fromGridCoord = AttributeTypeFactory.newAttributeType(
				"fromGridCoord", String.class);
		AttributeType toGridCoord = AttributeTypeFactory.newAttributeType(
				"toGridCoord", String.class);
		AttributeType avgUtilOffset = AttributeTypeFactory.newAttributeType(
				"avgUtilOffset", Double.class);
		AttributeType stddev = AttributeTypeFactory.newAttributeType("stddev",
				Double.class);
		AttributeType volume = AttributeTypeFactory.newAttributeType("volume",
				Integer.class);
		defaultFeatureTypeFactory = new DefaultFeatureTypeFactory();
		defaultFeatureTypeFactory.setName("trips");
		defaultFeatureTypeFactory.addTypes(new AttributeType[] { geom, ODcoord,
				fromGridCoord, toGridCoord, avgUtilOffset, stddev, volume });
	}

	public void setTripBundleLowLimit(int tripBundleLowLimit) {
		this.tripBundleLowLimit = tripBundleLowLimit;
	}

	public void setBarWidthScale(double barWidthScale) {
		this.barWidthScale = barWidthScale;
	}

	protected double getBarWidth(TripsWithUtilOffset trips) {
		return trips.getVolume() * barWidthScale;
	}

	protected LinearRing getLinearRing(TripsWithUtilOffset trips) {

		// //////////////////////////////////////////////////////////////
		double width = getBarWidth(trips);
		// //////////////////////////////////////////////////////////////
		Coordinate from = getCoordinate(trips.getOrig());

		Coordinate to = getCoordinate(trips.getDest());

		double xdiff = to.x - from.x;
		double ydiff = to.y - from.y;
		double denominator = Math.sqrt(xdiff * xdiff + ydiff * ydiff);
		double xwidth = width * ydiff / denominator;
		double ywidth = -width * xdiff / denominator;

		Coordinate fromB = new Coordinate(from.x + xwidth, from.y + ywidth, 0);
		// Coordinate toB = new Coordinate(to.x + xwidth, to.y + ywidth, 0);

		Coordinate toC = new Coordinate(0.2 * fromB.x + 0.8 * (to.x + xwidth),
				0.2 * fromB.y + 0.8 * (to.y + ywidth), 0);
		Coordinate toD = new Coordinate(toC.x + xwidth, toC.y + ywidth, 0);
		// ////////////////////////////////////////////////////////////////////////
		return new LinearRing(new CoordinateArraySequence(new Coordinate[] {
				from, to, toD, toC, fromB, from }), geofac);
	}

	public Collection<Feature> getFeatures() throws SchemaException,
			NumberFormatException, IllegalAttributeException {
		for (int i = 0; i < attrTypes.size(); i++) {
			defaultFeatureTypeFactory.addType(attrTypes.get(i));
		}
		FeatureType ftRoad = defaultFeatureTypeFactory.getFeatureType();
		for (TripsWithUtilOffset trips : tripsWithUtilOffsetMap.values()) {
			double avgUtilOffset = trips.getAverageUtilOffset();
			double standardDeviation = trips.getStandardDeviation();
			Coord origGrid = trips.getOrig(), destGrid = trips.getDest();

			// if (trips.getVolume() > 1) {
			// System.out.println("avgUtilOffset abs\t"
			// + Math.abs(avgUtilOffset) + "\tsigma\t"
			// + standardDeviation + "\ttripVolume\t"
			// + trips.getVolume() + "\ttripBundleLowLimit\t"
			// + this.tripBundleLowLimit);
			// }

			if (Math.abs(avgUtilOffset) >
			// 2d *
			standardDeviation
					&& trips.getVolume() >= tripBundleLowLimit
					&& !origGrid.equals(destGrid)) {
				LinearRing lr = getLinearRing(trips);
				Polygon p = new Polygon(lr, null, geofac);
				MultiPolygon mp = new MultiPolygon(new Polygon[] { p }, geofac);
				int size = 7 + parameters.size();
				Object[] o = new Object[size];
				o[0] = mp;
				String id = trips.getId();
				o[1] = id;
				o[2] = origGrid.toString();
				o[3] = destGrid.toString();
				o[4] = avgUtilOffset;
				o[5] = standardDeviation;
				o[6] = trips.getVolume();
				for (int i = 0; i < parameters.size(); i++) {
					o[i + 7] = parameters.get(i).get(id);
				}
				Feature ft = ftRoad.create(o, id);
				features.add(ft);
			}
		}
		System.out.println("features size\t" + features.size());
		return features;
	}
}
