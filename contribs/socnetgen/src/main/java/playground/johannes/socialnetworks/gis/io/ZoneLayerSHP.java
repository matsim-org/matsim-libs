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

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.matsim.contrib.common.gis.EsriShapeIO;
import org.matsim.contrib.socnetgen.sna.gis.Zone;
import org.matsim.contrib.socnetgen.sna.gis.ZoneLayer;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

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
//	public static <T> void write(ZoneLayer<T> layer, String filename) throws IOException {
//		/*
//		 * create feature type from zones
//		 */
//		CoordinateReferenceSystem crs = layer.getCRS();
//		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
//		b.setCRS(crs);
//		b.setName("zone");
//		b.add("location", Polygon.class);
//		b.add("value", Double.class);
//		SimpleFeatureType featureType = b.buildFeatureType();
//		
//		/*
//		 * Create a data store
//		 */
//		URL url = new File(filename).toURI().toURL();
//		ShapefileDataStore datastore = new ShapefileDataStore(url);
//		/*
//		 * Retrieve one feature to get the FeatureType for schema creation
//		 */
//		datastore.createSchema(featureType);
//		/*
//		 * Create a FeatureWriter and write the attributes
//		 */
//		FeatureWriter<SimpleFeatureType, SimpleFeature> writer = datastore.getFeatureWriter(Transaction.AUTO_COMMIT);
//		
//		for(Zone<T> zone : layer.getZones()) {
//			SimpleFeature feature = writer.next();
//			try {
//				feature.setAttribute(0, zone.getGeometry());
//				feature.setAttribute(1, zone.getAttribute());
//			} catch (ArrayIndexOutOfBoundsException e) {
//				e.printStackTrace();
//			} catch (IllegalArgumentException e) {
//				e.printStackTrace();
//			}
//			writer.write();
//		}
////		datastore.dispose();
//		writer.close();
//		/*
//		 * It seems that in some cases the .prj file is not written. This is a
//		 * workaround to manually write the .prj file.
//		 */
//		int idx = filename.lastIndexOf(".");
//		if(idx >= 0) 
//			filename = filename.substring(0,  idx + 1) + "prj";
//		else
//			filename = filename + ".prj";
//		File file = new File(filename);
//		if(!file.exists()) {
//			PrintWriter pwriter = new PrintWriter(new File(filename));
//			String wkt = featureType.getCoordinateReferenceSystem().toWKT();
//			pwriter.write(wkt);
//			pwriter.close();
//		}
//	}

	/**
	 * Reads a zone layer from a shape file. The shape file should be a
	 * collection features of type "Zone".
	 * 
	 * @param filename the path to the shape file
	 * @return a zone layer initialized with the features read from the shape file.
	 * @throws IOException
	 */
	public static ZoneLayer<Map<String, Object>> read(String filename) throws IOException {
		Set<Zone<Map<String, Object>>> zones = new HashSet<Zone<Map<String, Object>>>();
		for(SimpleFeature feature : EsriShapeIO.readFeatures(filename)) {
			Zone<Map<String, Object>> zone = new Zone<Map<String, Object>>((Geometry) feature.getDefaultGeometry());
			Map<String, Object> map = new HashMap<String, Object>(feature.getAttributeCount());
			for(Property prop : feature.getProperties()) {
				map.put(prop.getName().getLocalPart(), prop.getValue());
			}
			zone.setAttribute(map);
			zones.add(zone);
		}
		
		return new ZoneLayer<Map<String, Object>>(zones);
	}
	
	public static ZoneLayer<Double> read(String filename, String key) throws IOException {
		Set<Zone<Double>> zones = new HashSet<Zone<Double>>();
		for(SimpleFeature feature : EsriShapeIO.readFeatures(filename)) {
			Zone<Double> zone = new Zone<Double>((Geometry) feature.getDefaultGeometry());
//			feature.getFeatureType().
			Object obj = feature.getAttribute(key);
			if (obj != null) {
				double val = Double.parseDouble(obj.toString()); // FIXME
				zone.setAttribute(val);
			}
			zones.add(zone);
		}
		
		return new ZoneLayer<Double>(zones);
	}
	
	public static <T> void write(ZoneLayer<T> layer, String filename) throws IOException {
		CoordinateReferenceSystem crs = layer.getCRS();
		SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
		typeBuilder.setCRS(crs);
		typeBuilder.setName("zone");
		typeBuilder.add("the_geom", MultiPolygon.class);
		typeBuilder.add("value", Double.class);
		SimpleFeatureType featureType = typeBuilder.buildFeatureType();
        
		DefaultFeatureCollection collection = new DefaultFeatureCollection();
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);

        for(Zone<T> zone : layer.getZones()) {
        	featureBuilder.add(zone.getGeometry());
        	featureBuilder.add(zone.getAttribute());
        	SimpleFeature feature = featureBuilder.buildFeature(null);
        	collection.add(feature);
        }

        File newFile = new File(filename);

        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();

        Map<String, Serializable> params = new HashMap<String, Serializable>();
        params.put("url", newFile.toURI().toURL());
        params.put("create spatial index", Boolean.TRUE);

        ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
        newDataStore.createSchema(featureType);

//        newDataStore.forceSchemaCRS(layer.getCRS());
		
        Transaction transaction = new DefaultTransaction("create");

		String typeName = newDataStore.getTypeNames()[0];
		SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);

		SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;

		featureStore.setTransaction(transaction);
		try {
			featureStore.addFeatures(collection);
			transaction.commit();

		} catch (Exception problem) {
			problem.printStackTrace();
			transaction.rollback();

		} finally {
			transaction.close();
		}
	}
	
	public static void writeWithAttributes(ZoneLayer<Map<String, Object>> layer, String filename) throws IOException {
		CoordinateReferenceSystem crs = layer.getCRS();
		SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
		typeBuilder.setCRS(crs);
		typeBuilder.setName("zone");
		typeBuilder.add("the_geom", MultiPolygon.class);
		/*
		 * TODO: Only those attributes of this zone are written.
		 */
		Zone<Map<String, Object>> azone = layer.getZones().iterator().next();
		List<String> keys = new ArrayList<>(azone.getAttribute().keySet());
		for(String key : keys) {
			typeBuilder.add(key, azone.getAttribute().get(key).getClass());
		}
		
		SimpleFeatureType featureType = typeBuilder.buildFeatureType();

		DefaultFeatureCollection collection = new DefaultFeatureCollection();
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);

        for(Zone<Map<String, Object>> zone : layer.getZones()) {
        	featureBuilder.add(zone.getGeometry());
        	for(String key : keys) {
        		featureBuilder.add(zone.getAttribute().get(key));
        	}
        	SimpleFeature feature = featureBuilder.buildFeature(null);
        	collection.add(feature);
        }

        File newFile = new File(filename);

        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();

        Map<String, Serializable> params = new HashMap<String, Serializable>();
        params.put("url", newFile.toURI().toURL());
        params.put("create spatial index", Boolean.TRUE);

        ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
        newDataStore.createSchema(featureType);

//        newDataStore.forceSchemaCRS(layer.getCRS());
		
        Transaction transaction = new DefaultTransaction("create");

		String typeName = newDataStore.getTypeNames()[0];
		SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);

		SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;

		featureStore.setTransaction(transaction);
		try {
			featureStore.addFeatures(collection);
			transaction.commit();

		} catch (Exception problem) {
			problem.printStackTrace();
			transaction.rollback();

		} finally {
			transaction.close();
		}
	}
}
