/* *********************************************************************** *
 * project: org.matsim.*
 * Trips2PolygonGraphNormal.java
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

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PolygonFeatureFactory;
import org.opengis.feature.simple.SimpleFeature;
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
public class Trips2PolygonGraphNormal extends X2GraphImpl {
	private Map<Tuple<Coord, Coord>, TripsWithUtilOffset> tripsWithUtilOffsetMap;
	private int tripBundleLowLimit = 1;
	private double barWidthScale = 5;
	private PolygonFeatureFactory.Builder factoryBuilder;
	
	public Trips2PolygonGraphNormal(
			Map<Tuple<Coord, Coord>, TripsWithUtilOffset> tripsWithUtilOffsetMap,
			CoordinateReferenceSystem crs, int tripBundleLowLimit) {
		this(tripsWithUtilOffsetMap, crs);
		this.tripBundleLowLimit = tripBundleLowLimit;
	}

	public Trips2PolygonGraphNormal(
			Map<Tuple<Coord, Coord>, TripsWithUtilOffset> childTripsWithUtilOffsetMap,
			CoordinateReferenceSystem crs) {
		tripsWithUtilOffsetMap = childTripsWithUtilOffsetMap;
		this.crs = crs;

		geofac = new GeometryFactory();

		features = new ArrayList<SimpleFeature>();

		this.factoryBuilder = new PolygonFeatureFactory.Builder().
				setCrs(this.crs).
				setName("trips").
				addAttribute("ODcoord", String.class).
				addAttribute("fromGridCoord", String.class).
				addAttribute("toGridCoord", String.class).
				addAttribute("avgUtilOffset", Double.class).
				addAttribute("stddev", Double.class).
				addAttribute("volume", Double.class);
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
		Coordinate from = MGC.coord2Coordinate(trips.getOrig());
		Coordinate to = MGC.coord2Coordinate(trips.getDest());

		double xdiff = to.x - from.x, ydiff = to.y - from.y;
		double denominator = Math.sqrt(xdiff * xdiff + ydiff * ydiff);
		double xwidth = width * ydiff / denominator, ywidth = -width * xdiff
				/ denominator;

		Coordinate fromB = new Coordinate(from.x + xwidth, from.y + ywidth, 0);
		// Coordinate toB = new Coordinate(to.x + xwidth, to.y + ywidth, 0);

		Coordinate toC = new Coordinate(0.2 * fromB.x + 0.8 * (to.x + xwidth),
				0.2 * fromB.y + 0.8 * (to.y + ywidth), 0);
		Coordinate toD = new Coordinate(toC.x + xwidth, toC.y + ywidth, 0);
		// ////////////////////////////////////////////////////////////////////////
		return new LinearRing(new CoordinateArraySequence(new Coordinate[] {
				from, to, toD, toC, fromB, from }), geofac);
	}

	public Collection<SimpleFeature> getFeatures() {
		for (int i = 0; i < attrTypes.size(); i++) {
			Tuple<String, Class<?>> att = attrTypes.get(i);
			factoryBuilder.addAttribute(att.getFirst(), att.getSecond());
		}
		PolygonFeatureFactory factory = factoryBuilder.create();
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

			if (
			// Math.abs(avgUtilOffset) > // 2d * standardDeviation &&
			trips.getVolume() >= tripBundleLowLimit
					&& !origGrid.equals(destGrid)) {
				LinearRing lr = getLinearRing(trips);
				Polygon p = new Polygon(lr, null, geofac);
				MultiPolygon mp = new MultiPolygon(new Polygon[] { p }, geofac);
				int size = 6 + parameters.size();
				Object[] o = new Object[size];
				String id = trips.getId();
				o[0] = id;
				o[1] = origGrid.toString();
				o[2] = destGrid.toString();
				o[3] = avgUtilOffset;
				o[4] = standardDeviation;
				o[5] = trips.getVolume();
				for (int i = 0; i < parameters.size(); i++) {
					o[i + 6] = parameters.get(i).get(id);
				}
				SimpleFeature ft = factory.createPolygon(mp, o, null);
				features.add(ft);
			}
		}
		System.out.println("features size\t" + features.size());
		return features;
	}
}
