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

import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.DefaultFeatureTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.api.core.v01.Coord;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.yu.utils.qgis.X2GraphImpl;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

public class Grid2Graph4Distribution extends X2GraphImpl {
	private double gridSideLength_m;
	private Map<Coord/* actLoc */, AreaUtilityOffsets> gridUtilOffsetMap = new HashMap<Coord, AreaUtilityOffsets>();

	public Grid2Graph4Distribution(CoordinateReferenceSystem crs,
			double gridSideLength_m,
			Map<Coord, AreaUtilityOffsets> gridUtilOffsetMap) {
		this.gridSideLength_m = gridSideLength_m;
		this.gridUtilOffsetMap = gridUtilOffsetMap;

		geofac = new GeometryFactory();
		features = new ArrayList<Feature>();

		AttributeType geom = DefaultAttributeTypeFactory.newAttributeType(
				"MultiPolygon", MultiPolygon.class, true, null, null, crs);
		AttributeType ctX = AttributeTypeFactory.newAttributeType("centerX",
				Double.class);
		AttributeType ctY = AttributeTypeFactory.newAttributeType("centerY",
				Double.class);
		AttributeType avgUtilOffset = AttributeTypeFactory.newAttributeType(
				"avgUtilOffset", Double.class);
		AttributeType nonzeroUtilOffsetscnt = AttributeTypeFactory
				.newAttributeType("#noZeroUOLegs", Integer.class);
		AttributeType nonzeroUtilOffsetsRatio = AttributeTypeFactory
				.newAttributeType("uoRatio", Double.class);
		AttributeType legCnt = AttributeTypeFactory.newAttributeType("#Legs",
				Integer.class);
		AttributeType inOneSigma = AttributeTypeFactory.newAttributeType(
				"in1sigma", Boolean.class);

		defaultFeatureTypeFactory = new DefaultFeatureTypeFactory();
		defaultFeatureTypeFactory.setName("grid");
		defaultFeatureTypeFactory.addTypes(new AttributeType[] { geom, ctX,
				ctY, avgUtilOffset, nonzeroUtilOffsetscnt,
				nonzeroUtilOffsetsRatio, legCnt, inOneSigma });
	}

	public double getGridSideLength_m() {
		return gridSideLength_m;
	}

	public void setGridSideLength_m(double gridSideLengthM) {
		gridSideLength_m = gridSideLengthM;
	}

	public Map<Coord, AreaUtilityOffsets> getGridUtilOffsetMap() {
		return gridUtilOffsetMap;
	}

	public void setGridUtilOffsetMap(
			Map<Coord, AreaUtilityOffsets> gridUtilOffsetMap) {
		this.gridUtilOffsetMap = gridUtilOffsetMap;
	}

	public Collection<Feature> getFeatures() throws SchemaException,
			NumberFormatException, IllegalAttributeException {
		for (int i = 0; i < attrTypes.size(); i++) {
			defaultFeatureTypeFactory.addType(attrTypes.get(i));
		}
		FeatureType ftRoad = defaultFeatureTypeFactory.getFeatureType();

		for (Coord gridCenter : gridUtilOffsetMap.keySet()) {
			LinearRing ls = getGridRing(gridCenter);
			Polygon p = new Polygon(ls, null, geofac);
			MultiPolygon mp = new MultiPolygon(new Polygon[] { p }, geofac);

			int size = 8 + parameters.size();
			Object[] o = new Object[size];
			o[0] = mp;
			o[1] = gridCenter.getX();
			o[2] = gridCenter.getY();
			AreaUtilityOffsets utilOffsets = gridUtilOffsetMap.get(gridCenter);
			int nonzeroUtilOffsetsCnt = utilOffsets
					.getNonzeroUtilityOffsetsCnt();
			o[3] = utilOffsets.getAverageNonzeroUtilityOffset() /*
																 * avg.
																 * LegUtilOffset
																 */;
			o[4] = nonzeroUtilOffsetsCnt/* #Legs */;
			int cnt = nonzeroUtilOffsetsCnt
					+ utilOffsets.getZeroUtilOffsetCnt();
			o[5] = (double) nonzeroUtilOffsetsCnt / (double) cnt;
			o[6] = cnt;
			o[7] = utilOffsets.isInOneSigma()
			// ? 1 : -1
			;
			for (int i = 0; i < parameters.size(); i++) {
				o[i + 8] = parameters.get(i).get(gridCenter);
			}

			Feature ft = ftRoad.create(o, gridCenter.toString()
					+ "-UtilOffsets");
			features.add(ft);
		}

		return features;
	}

	protected LinearRing getGridRing(Coord center) {
		Coordinate ct = getCoordinate(center);

		Coordinate A = new Coordinate(ct.x - gridSideLength_m / 2d, ct.y
				+ gridSideLength_m / 2d);
		Coordinate B = new Coordinate(ct.x + gridSideLength_m / 2d, ct.y
				+ gridSideLength_m / 2d);
		Coordinate C = new Coordinate(ct.x + gridSideLength_m / 2d, ct.y
				- gridSideLength_m / 2d);
		Coordinate D = new Coordinate(ct.x - gridSideLength_m / 2d, ct.y
				- gridSideLength_m / 2d);

		return new LinearRing(new CoordinateArraySequence(new Coordinate[] { A,
				B, C, D, A }), geofac);
	}
}
