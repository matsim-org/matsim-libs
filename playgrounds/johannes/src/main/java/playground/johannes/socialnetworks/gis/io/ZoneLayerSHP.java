/* *********************************************************************** *
 * project: org.matsim.*
 * ZoneLayerSHPWriter.java
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
package playground.johannes.socialnetworks.gis.io;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.geotools.data.FeatureWriter;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.factory.FactoryConfigurationError;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeBuilder;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.johannes.sna.gis.Zone;
import playground.johannes.sna.gis.ZoneLayer;

import com.vividsolutions.jts.geom.Polygon;

/**
 * Utility class to read and write zone layers into shape (.shp) files.
 * 
 * @author illenberger
 *
 */
public class ZoneLayerSHP {

	/**
	 * Writes a zone layer as a collection of features into a shape file.
	 * 
	 * @param layer a zone layer
	 * @param filename the path to the shape file
	 * @throws IOException
	 */
	public static <T> void write(ZoneLayer<T> layer, String filename) throws IOException {
		/*
		 * create feature type from zones
		 */
		CoordinateReferenceSystem crs = layer.getCRS();
		AttributeType[] attTypes = new AttributeType[2];
		attTypes[0] = DefaultAttributeTypeFactory.newAttributeType("polygon", Polygon.class, true, null, null, crs);
		attTypes[1] = AttributeTypeFactory.newAttributeType("value", Double.class);
		
		FeatureType featureType = null;
		try {
			featureType = FeatureTypeBuilder.newFeatureType(attTypes, "zone");
		} catch (FactoryConfigurationError e) {
			e.printStackTrace();
		} catch (SchemaException e) {
			e.printStackTrace();
		}
		/*
		 * Create a data store
		 */
		URL url = new File(filename).toURI().toURL();
		ShapefileDataStore datastore = new ShapefileDataStore(url);
		/*
		 * Retrieve one feature to get the FeatureType for schema creation
		 */
		datastore.createSchema(featureType);
		/*
		 * Create a FeatureWriter and write the attributes
		 */
		FeatureWriter writer = datastore.getFeatureWriter(Transaction.AUTO_COMMIT);
		for(Zone<T> zone : layer.getZones()) {
			Feature feature = writer.next();
			try {
				feature.setAttribute(0, zone.getGeometry());
				feature.setAttribute(1, zone.getAttribute());
			} catch (ArrayIndexOutOfBoundsException e) {
				e.printStackTrace();
			} catch (IllegalAttributeException e) {
				e.printStackTrace();
			}
			writer.write();
		}
		writer.close();
		/*
		 * It seems that in some cases the .prj file is not written. This is a
		 * workaround to manually write the .prj file.
		 */
		int idx = filename.lastIndexOf(".");
		if(idx >= 0) 
			filename = filename.substring(0,  idx + 1) + "prj";
		else
			filename = filename + ".prj";
		File file = new File(filename);
		if(!file.exists()) {
			PrintWriter pwriter = new PrintWriter(new File(filename));
			String wkt = featureType.getDefaultGeometry().getCoordinateSystem().toWKT();
			pwriter.write(wkt);
			pwriter.close();
		}
	}

	/**
	 * Reads a zone layer from a shape file. The shape file should be a
	 * collection features of type "Zone".
	 * 
	 * @param filename the path to the shape file
	 * @return a zone layer initialized with the features read from the shape file.
	 * @throws IOException
	 */
	public static <T> ZoneLayer<T> read(String filename) throws IOException {
//		GeometryFactory factory = new GeometryFactory();
		
		Set<Zone<T>> zones = new HashSet<Zone<T>>();
		for(Feature feature : FeatureSHP.readFeatures(filename)) {
//			Coordinate[] coordinates = feature.getDefaultGeometry().getCoordinates();
//			Coordinate[] newCoords = new Coordinate[coordinates.length + 1];
//			for(int i = 0; i < coordinates.length; i++) {
//				newCoords[i] = coordinates[i];
//			}
//			newCoords[newCoords.length - 1] = coordinates[0];
//			
//			LinearRing shell = factory.createLinearRing(newCoords);
//			zones.add(new Zone<T>(factory.createPolygon(shell, null)));
			zones.add(new Zone<T>(feature.getDefaultGeometry().getGeometryN(0)));
		}
		
		return new ZoneLayer<T>(zones);
	}
}
