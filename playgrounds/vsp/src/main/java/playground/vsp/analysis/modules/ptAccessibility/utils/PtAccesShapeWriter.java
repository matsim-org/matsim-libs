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
import java.util.HashMap;
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
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;

import playground.vsp.analysis.modules.ptAccessibility.activity.ActivityLocation;
import playground.vsp.analysis.modules.ptAccessibility.activity.LocationMap;
import playground.vsp.analysis.utils.GridNode;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

/**
 * @author droeder, aneumann
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
	public static void writeMultiPolygons(Map<String, MultiPolygon> mps, String filename, String name, String targetCoordinateSystem) {
		AttributeType[] attribs = new AttributeType[2];
		attribs[0] = DefaultAttributeTypeFactory.newAttributeType("MultiPolygon", MultiPolygon.class, true, null, null, MGC.getCRS(targetCoordinateSystem));
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
	
	public static void writeActivityLocations(LocationMap locationMap, String outputFolder, String name, String targetCoordinateSystem, double gridSize){
		AttributeType[] attribs = new AttributeType[3];
		attribs[0] = DefaultAttributeTypeFactory.newAttributeType("Point", Point.class, true, null, null, MGC.getCRS(targetCoordinateSystem));
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
		
		AttributeType[] clusterAttribs = new AttributeType[2];
		clusterAttribs[0] = DefaultAttributeTypeFactory.newAttributeType("Point", Point.class, true, null, null, MGC.getCRS(targetCoordinateSystem));
		clusterAttribs[1] = AttributeTypeFactory.newAttributeType("count", Integer.class);
		FeatureType clusterFeatureType = null ;
		try {
			clusterFeatureType = FeatureTypeBuilder.newFeatureType(clusterAttribs, name);
		} catch (FactoryRegistryException e) {
			e.printStackTrace();
		} catch (SchemaException e) {
			e.printStackTrace();
		}
		
		GeometryFactory factory = new GeometryFactory();
		for(Entry<String, List<ActivityLocation>> type2LocationEntry: locationMap.getType2Locations().entrySet()){
			if (!type2LocationEntry.getValue().isEmpty()) {
				try {
					Collection<Feature> features = new ArrayList<Feature>();
					Object[] featureAttribs ;
					for(int i  = 0; i < type2LocationEntry.getValue().size(); i++){
						featureAttribs = new Object[3];
						featureAttribs[0] = factory.createPoint(type2LocationEntry.getValue().get(i).getCoord());
						featureAttribs[1] = type2LocationEntry.getKey() + "_" + String.valueOf(i);
						featureAttribs[2] = type2LocationEntry.getKey();
						features.add(featureType.create(featureAttribs));
					}
					
					ShapeFileWriter.writeGeometries(features, outputFolder + "activityLocations_" + type2LocationEntry.getKey() + ".shp");
				} catch (IllegalAttributeException e) {
					e.printStackTrace();
				} catch(ServiceConfigurationError e){
					e.printStackTrace();
				}
				
				// cluster first write then
				
				try {
					Collection<Feature> features = new ArrayList<Feature>();
					Object[] featureAttribs ;
					
					HashMap<String, GridNode> gridNodeId2GridNode = new HashMap<String, GridNode>();
					
					// go through all acts of this type
					for(int i  = 0; i < type2LocationEntry.getValue().size(); i++){
						Coord coord = new CoordImpl(type2LocationEntry.getValue().get(i).getCoord().x, type2LocationEntry.getValue().get(i).getCoord().y);
						String gridNodeId = GridNode.getGridNodeIdForCoord(coord, gridSize);
						
						if (gridNodeId2GridNode.get(gridNodeId) == null) {
							gridNodeId2GridNode.put(gridNodeId, new GridNode(gridNodeId));
						}
						
						gridNodeId2GridNode.get(gridNodeId).addPoint(type2LocationEntry.getKey(), coord);
					}
					
					for (GridNode gridNode : gridNodeId2GridNode.values()) {
						featureAttribs = new Object[2];
						featureAttribs[0] = factory.createPoint(new Coordinate(gridNode.getX(), gridNode.getY(), 0.));
						featureAttribs[1] = gridNode.getCountForType(type2LocationEntry.getKey());
						features.add(clusterFeatureType.create(featureAttribs));
					}
					
					ShapeFileWriter.writeGeometries(features, outputFolder + "activityLocations_clustered_" + type2LocationEntry.getKey() + ".shp");
				} catch (IllegalAttributeException e) {
					e.printStackTrace();
				} catch(ServiceConfigurationError e){
					e.printStackTrace();
				}
				
			} else {
				log.info("No activities found for cluster " + type2LocationEntry.getKey());
			}
		}
	}

}

