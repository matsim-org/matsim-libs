/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.vsp.analysis.modules.ptAccessibility.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceConfigurationError;

import org.apache.log4j.Logger;
import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeBuilder;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;

import playground.vsp.analysis.modules.ptAccessibility.activity.ActivityLocation;
import playground.vsp.analysis.modules.ptAccessibility.activity.LocationMap;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

/**
 * @author droeder
 * just a helper-class 
 *
 */
public class PtAccesShapeWriter {

	@SuppressWarnings("unused")
	private static final Logger log = Logger
			.getLogger(PtAccesShapeWriter.class);

	private PtAccesShapeWriter() {
		
	}

	/**
	 * @param mps
	 * @param string
	 */
	public static void writeMultiPolygons(Map<String, MultiPolygon> mps, String filename, String name) {
		AttributeType[] attribs = new AttributeType[2];
		attribs[0] = DefaultAttributeTypeFactory.newAttributeType("MultiPolygon", MultiPolygon.class, true, null, null, MGC.getCRS(TransformationFactory.WGS84_UTM35S));
		attribs[1] = AttributeTypeFactory.newAttributeType("name", String.class);
		FeatureType featureType = null ;
		try {
			featureType = FeatureTypeBuilder.newFeatureType(attribs, name);
		} catch (FactoryRegistryException e) {
			e.printStackTrace();
		} catch (SchemaException e) {
			e.printStackTrace();
		}
		Collection<Feature> features = new ArrayList<Feature>();
		
		Object[] featureAttribs ;
		
		for(Entry<String, MultiPolygon> e: mps.entrySet()){
			featureAttribs = new Object[2];
			featureAttribs[0] = e.getValue();
			featureAttribs[1] = e.getKey();
			try {
				features.add(featureType.create(featureAttribs));
			} catch (IllegalAttributeException e1) {
				e1.printStackTrace();
			}
		}
		try{
			ShapeFileWriter.writeGeometries(features, filename);
		}catch(ServiceConfigurationError e){
			e.printStackTrace();
		}
	}
	
	public static void writeActivityLocations(LocationMap locationMap, String filename, String name){
		AttributeType[] attribs = new AttributeType[3];
		attribs[0] = DefaultAttributeTypeFactory.newAttributeType("Point", Point.class, true, null, null, MGC.getCRS(TransformationFactory.WGS84_UTM35S));
		attribs[1] = AttributeTypeFactory.newAttributeType("name", String.class);
		attribs[2] = AttributeTypeFactory.newAttributeType("type", String.class);
		FeatureType featureType = null ;
		try {
			featureType = FeatureTypeBuilder.newFeatureType(attribs, name);
		} catch (FactoryRegistryException e) {
			e.printStackTrace();
		} catch (SchemaException e) {
			e.printStackTrace();
		}
		Collection<Feature> features = new ArrayList<Feature>();
		
		Object[] featureAttribs ;
		
		GeometryFactory factory = new GeometryFactory();
		Coordinate[] c;
		for(Entry<String, List<ActivityLocation>> e: locationMap.getType2Locations().entrySet()){
			
			try {
				for(int i  = 0; i < e.getValue().size(); i++){
					featureAttribs = new Object[3];
					featureAttribs[0] = factory.createPoint(e.getValue().get(i).getCoord());
					featureAttribs[1] = e.getKey() + "_" + String.valueOf(i);
					featureAttribs[1] = e.getKey();
					features.add(featureType.create(featureAttribs));
				}
			} catch (IllegalAttributeException e1) {
				e1.printStackTrace();
			}
		}
		try{
			ShapeFileWriter.writeGeometries(features, filename);
		}catch(ServiceConfigurationError e){
			e.printStackTrace();
		}
	}

}

