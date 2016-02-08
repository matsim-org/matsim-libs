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
package playground.agarwalamit.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier;

/**
 * @author amit
 */

public final class GeometryUtils {

	private GeometryUtils(){}
	private static final Random RAND = MatsimRandom.getRandom(); // matsim random will return same coord.
	private static final GeometryFactory GF = new GeometryFactory();

	public static Point getRandomPointsInsideFeature (SimpleFeature feature) {
		Point p = null;
		double x,y;
		do {
			x = feature.getBounds().getMinX()+RAND.nextDouble()*(feature.getBounds().getMaxX()-feature.getBounds().getMinX());
			y = feature.getBounds().getMinY()+RAND.nextDouble()*(feature.getBounds().getMaxY()-feature.getBounds().getMinY());
			p= MGC.xy2Point(x, y);
		} while (!((Geometry) feature.getDefaultGeometry()).contains(p));
		return p;
	}

	public static boolean isLinkInsideGeometries(Collection<Geometry> features, Link link) {
		Geometry linkGeo = GF.createPoint(new Coordinate(link.getCoord().getX(), link.getCoord().getY()));
		for(Geometry  geo: features){
			if ( geo.contains(linkGeo) ) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean isLinkInsideCity(Collection<SimpleFeature> features, Link link) {
		Geometry geo = GF.createPoint(new Coordinate(link.getCoord().getX(), link.getCoord().getY()));
		for(SimpleFeature sf : features){
			if ( ( getSimplifiedGeom( (Geometry) sf.getDefaultGeometry() ) ).contains(geo) ) {
				return true;
			}
		}
		return false;
	}

	public static boolean isPointInsideCity(Collection<SimpleFeature> features, Point point) {
		Geometry geo = GF.createPoint( new Coordinate( point.getCoordinate() ) );
		for(SimpleFeature sf : features){
			if ( ( getSimplifiedGeom( (Geometry) sf.getDefaultGeometry() ) ).contains(geo) ) {
				return true;
			}
		}
		return false;
	}

	public static Collection<Geometry> getSimplifiedGeometries(Collection<SimpleFeature> features){
		Collection<Geometry> geoms = new ArrayList<>();
		for(SimpleFeature sf:features){
			geoms.add(getSimplifiedGeom( (Geometry) sf.getDefaultGeometry()));
		}
		return geoms;
	}

	/**
	 * @param geom
	 * @return A simplified geometry by increasing tolerance until number of vertices are less than 1000.
	 */
	public static Geometry getSimplifiedGeom(final Geometry geom){
		Geometry outGeom = geom;
		double distanceTolerance = 1;
		int numberOfVertices = getNumberOfVertices(geom);
		while (numberOfVertices > 1000){
			outGeom = getSimplifiedGeom(outGeom, distanceTolerance);
			numberOfVertices = getNumberOfVertices(outGeom);
			distanceTolerance *= 10;
		}
		return outGeom;
	}


	public static Geometry getSimplifiedGeom(final Geometry geom, final double distanceTolerance){
		return TopologyPreservingSimplifier.simplify(geom, distanceTolerance);
	}

	public static int getNumberOfVertices(final Geometry geom){
		return geom.getNumPoints();
	}

	public static Point getRandomPointsInsideFeatures (List<SimpleFeature> features) {
		Tuple<Double,Double> xs = getMaxMinXFromFeatures(features);
		Tuple<Double,Double> ys = getMaxMinYFromFeatures(features);
		Geometry combinedGeometry = getGemetryFromListOfFeatures(features);
		Point p = null;
		double x,y;
		do {
			x = xs.getFirst()+RAND.nextDouble()*(xs.getSecond() - xs.getFirst());
			y = ys.getFirst()+RAND.nextDouble()*(ys.getSecond() - ys.getFirst());
			p= MGC.xy2Point(x, y);
		} while (! (combinedGeometry).contains(p) );
		return p;
	}

	public static Tuple<Double,Double> getMaxMinXFromFeatures (List<SimpleFeature> features){
		double minX = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;

		for (SimpleFeature f : features){
			if (minX > f.getBounds().getMinX()) minX =  f.getBounds().getMinX();
			if (maxX < f.getBounds().getMaxX()) maxX =  f.getBounds().getMaxX();
		}
		return new Tuple<Double, Double>(minX, maxX);
	}

	public static Tuple<Double,Double> getMaxMinYFromFeatures (List<SimpleFeature> features){
		double minY = Double.POSITIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;

		for (SimpleFeature f : features){
			if (minY > f.getBounds().getMinY()) minY =  f.getBounds().getMinY();
			if (maxY < f.getBounds().getMaxY()) maxY =  f.getBounds().getMaxY();
		}
		return new Tuple<Double, Double>(minY, maxY);
	}

	public static Geometry getGemetryFromListOfFeatures(List<SimpleFeature> featues) {
		List<Geometry> geoms = new ArrayList<>();
		for(SimpleFeature sf : featues){
			geoms.add( (Geometry) sf.getDefaultGeometry() );
		}
		return combine(geoms);
	}

	public static Geometry combine(List<Geometry> geoms){
		Geometry geom = null;
		for(Geometry g : geoms){
			if(geom==null) geom = g;
			else {
				geom.union(g);
			}
		}
		return geom;
	}

	public static ReferencedEnvelope getBoundingBox(String shapeFile){
		ShapeFileReader shapeFileReader = new ShapeFileReader();
		shapeFileReader.readFileAndInitialize(shapeFile);
		return shapeFileReader.getBounds();
	}
}