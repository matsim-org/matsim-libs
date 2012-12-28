/* *********************************************************************** *
 * project: org.matsim.*
 * Grid2Graph.java
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

package playground.yu.integration.cadyts.demandCalibration.withCarCounts.experiments.actLocUtilOffset;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PolygonFeatureFactory;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.yu.utils.qgis.X2GraphImpl;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

public class Grid2Graph extends X2GraphImpl {
	private double gridSideLength_m;
	private Map<Coord/* actLoc */, Tuple<Integer/* cnt */, Double/* sum */>> gridUtilOffsetMap = new HashMap<Coord, Tuple<Integer, Double>>();
	private final PolygonFeatureFactory.Builder factoryBuilder;
	
	public Grid2Graph(CoordinateReferenceSystem crs, double gridSideLength_m,
			Map<Coord, Tuple<Integer, Double>> gridUtilOffsetMap) {
		this.gridSideLength_m = gridSideLength_m;
		this.gridUtilOffsetMap = gridUtilOffsetMap;

		this.geofac = new GeometryFactory();
		features = new ArrayList<SimpleFeature>();

		factoryBuilder = new PolygonFeatureFactory.Builder().
				setCrs(crs).
				setName("grid").
				addAttribute("centerC", Double.class).
				addAttribute("centerY", Double.class).
				addAttribute("avgUtilOffset", Double.class).
				addAttribute("#Legs", Double.class);
	}

	public double getGridSideLength_m() {
		return gridSideLength_m;
	}

	public void setGridSideLength_m(double gridSideLengthM) {
		gridSideLength_m = gridSideLengthM;
	}

	public Map<Coord, Tuple<Integer, Double>> getGridUtilOffsetMap() {
		return gridUtilOffsetMap;
	}

	public void setGridUtilOffsetMap(
			Map<Coord, Tuple<Integer, Double>> gridUtilOffsetMap) {
		this.gridUtilOffsetMap = gridUtilOffsetMap;
	}

	public Collection<SimpleFeature> getFeatures() {
		for (int i = 0; i < attrTypes.size(); i++) {
			Tuple<String, Class<?>> att = attrTypes.get(i);
			factoryBuilder.addAttribute(att.getFirst(), att.getSecond());
		}
		PolygonFeatureFactory factory = factoryBuilder.create();

		for (Coord gridCenter : this.gridUtilOffsetMap.keySet()) {
			LinearRing ls = this.getGridRing(gridCenter);
			Polygon p = new Polygon(ls, null, this.geofac);
			MultiPolygon mp = new MultiPolygon(new Polygon[] { p }, this.geofac);

			int size = 4 + parameters.size();
			Object[] o = new Object[size];
			o[0] = gridCenter.getX();
			o[1] = gridCenter.getY();
			Tuple<Integer, Double> utilOffsetPair = this.gridUtilOffsetMap
					.get(gridCenter);
			int cnt = utilOffsetPair.getFirst();
			o[2] = utilOffsetPair.getSecond() / (double) cnt /*
															 * avg.
															 * LegUtilOffset
															 */;
			o[3] = cnt/* #Legs */;

			for (int i = 0; i < parameters.size(); i++) {
				o[i + 4] = parameters.get(i).get(gridCenter);
			}

			SimpleFeature ft = factory.createPolygon(mp, o, gridCenter.toString() + "-UtilOffsets");
			features.add(ft);
		}

		return features;
	}

	protected LinearRing getGridRing(Coord center) {
		Coordinate ct = MGC.coord2Coordinate(center);

		Coordinate A = new Coordinate(ct.x - gridSideLength_m / 2d, ct.y
				+ gridSideLength_m / 2d);
		Coordinate B = new Coordinate(ct.x + gridSideLength_m / 2d, ct.y
				+ gridSideLength_m / 2d);
		Coordinate C = new Coordinate(ct.x + gridSideLength_m / 2d, ct.y
				- gridSideLength_m / 2d);
		Coordinate D = new Coordinate(ct.x - gridSideLength_m / 2d, ct.y
				- gridSideLength_m / 2d);

		return new LinearRing(new CoordinateArraySequence(new Coordinate[] { A,
				B, C, D, A }), this.geofac);
	}
}
