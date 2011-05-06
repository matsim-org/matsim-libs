/* *********************************************************************** *
 * project: org.matsim.*
 * ShapeFileMerger.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.gregor.gis.shapefilemerger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.geotools.data.FeatureSource;
import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeFactory;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.geotools.referencing.CRS;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class ShapeFileMerger {


	private static ArrayList<Feature> features;
	private static AttributeType mp;
	private static FeatureType ftMultiPolygon;
	private static FeatureType ftPolygon;
	private static FeatureType ftLineString;
	private static FeatureType ftMultiLineString;
	private static FeatureType ftPoint;
	private static FeatureType ftMultiPoint;
	private final static String WGS84_UTM47S = "PROJCS[\"WGS_1984_UTM_Zone_47S\",GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137.0,298.257223563]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"False_Easting\",500000.0],PARAMETER[\"False_Northing\",10000000.0],PARAMETER[\"Central_Meridian\",99.0],PARAMETER[\"Scale_Factor\",0.9996],PARAMETER[\"Latitude_Of_Origin\",0.0],UNIT[\"Meter\",1.0]]";

	public static void main(String [] args) {


		String zone_0_5 = "./padang/zona_0_5.shp";
		String zone_5_10 = "./padang/zona_5_1.shp";
		String output = "./padang/scenario_v20080716/input/evacarea.shp";


		FeatureSource fz_0_5 = null;
		try {
			fz_0_5 = ShapeFileReader.readDataFile(zone_0_5);
		} catch (Exception e) {
			e.printStackTrace();
		}



		FeatureSource fz_5_10 = null;
		try {
			fz_5_10 = ShapeFileReader.readDataFile(zone_5_10);
		} catch (Exception e) {
			e.printStackTrace();
		}


		Collection<Feature> evac_zone = new ArrayList<Feature>();
		readPolygons(evac_zone,fz_0_5);
		readPolygons(evac_zone,fz_5_10);
		try {
			initFeatureCollection();
		} catch (FactoryRegistryException e) {
			e.printStackTrace();
		} catch (SchemaException e) {
			e.printStackTrace();
		} catch (FactoryException e) {
			e.printStackTrace();
		}
		Collection<Feature> ft = null;
		try {
			ft = getFeatures(evac_zone);
		} catch (IllegalAttributeException e) {
			e.printStackTrace();
		}

		ShapeFileWriter.writeGeometries(ft, output);

	}

	private static Collection<Feature> getFeatures(Collection<Feature> evac_zone) throws IllegalAttributeException {

		FeatureType ftGeometry = null;
		Collection<Feature> features = new ArrayList<Feature>();
		Geometry geo = evac_zone.iterator().next().getDefaultGeometry();
		if (geo instanceof MultiPolygon) {
			ftGeometry = ftMultiPolygon;
		} else if (geo instanceof Polygon) {
			ftGeometry = ftPolygon;
		} else if (geo instanceof MultiLineString) {
			ftGeometry = ftMultiLineString;
		} else if (geo instanceof LineString) {
			ftGeometry = ftLineString;
		} else if (geo instanceof MultiPoint) {
			ftGeometry = ftMultiPoint;
		} else if (geo instanceof Point) {
			ftGeometry = ftPoint;
		}
		int id = 0;
		for (Feature ft : evac_zone) {
			Geometry g = (Geometry) ft.getAttribute(0);
			features.add(ftGeometry.create(new Object [] {g,id++}));
		}
		return features;
	}

	private static void initFeatureCollection() throws FactoryRegistryException, SchemaException, FactoryException {

		features = new ArrayList<Feature>();
		final CoordinateReferenceSystem targetCRS = CRS.parseWKT( WGS84_UTM47S);
		AttributeType mp = DefaultAttributeTypeFactory.newAttributeType("MultiPolygon",MultiPolygon.class, true, null, null, targetCRS);
		AttributeType p = DefaultAttributeTypeFactory.newAttributeType("Polygon",Polygon.class, true, null, null, targetCRS);
		AttributeType l = DefaultAttributeTypeFactory.newAttributeType("LineString",LineString.class, true, null, null, targetCRS);
		AttributeType ml = DefaultAttributeTypeFactory.newAttributeType("MultiLineString",MultiLineString.class, true, null, null, targetCRS);
		AttributeType point = DefaultAttributeTypeFactory.newAttributeType("Point",Point.class, true, null, null, targetCRS);
		AttributeType mpoint = DefaultAttributeTypeFactory.newAttributeType("MultiPoint",MultiPoint.class, true, null, null, targetCRS);
		AttributeType id = AttributeTypeFactory.newAttributeType("ID", Integer.class);

		ftMultiPolygon = FeatureTypeFactory.newFeatureType(new AttributeType[] {mp, id}, "geometry");
		ftPolygon = FeatureTypeFactory.newFeatureType(new AttributeType[] {p, id}, "geometry");
		ftLineString = FeatureTypeFactory.newFeatureType(new AttributeType[] {l, id}, "geometry");
		ftMultiLineString = FeatureTypeFactory.newFeatureType(new AttributeType[] {ml, id}, "geometry");
		ftPoint = FeatureTypeFactory.newFeatureType(new AttributeType[] {point, id}, "geometry");
		ftMultiPoint = FeatureTypeFactory.newFeatureType(new AttributeType[] {mpoint, id}, "geometry");
	}


	private static void readPolygons(Collection<Feature> evac_zone, FeatureSource fts) {


		FeatureIterator it = null;
		try {
			it = fts.getFeatures().features();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		while (it.hasNext()) {
			Feature feature = it.next();
			evac_zone.add(feature);

		}

	}

}
