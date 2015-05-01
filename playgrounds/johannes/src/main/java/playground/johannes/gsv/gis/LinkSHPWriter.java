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

package playground.johannes.gsv.gis;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.scenario.ScenarioUtils;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.johannes.gsv.sim.LinkOccupancyCalculator;
import playground.johannes.sna.gis.CRSUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

/**
 * @author johannes
 *
 */
public class LinkSHPWriter {

	public void write(Collection<? extends Link> links, LinkOccupancyCalculator calculator, String filename, double factor) throws IOException {
		CoordinateReferenceSystem crs = CRSUtils.getCRS(31467);
		
		SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
		typeBuilder.setCRS(crs);
		typeBuilder.setName("link flows");
		typeBuilder.add("the_geom", LineString.class);
		typeBuilder.add("capacity", Double.class);
		typeBuilder.add("flow", Double.class);
		typeBuilder.add("load", Double.class);
		typeBuilder.add("speed", Double.class);
		
		SimpleFeatureType featureType = typeBuilder.buildFeatureType();

		DefaultFeatureCollection collection = new DefaultFeatureCollection();
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
        
        GeometryFactory factory = JTSFactoryFinder.getGeometryFactory(null);
        for(Link link : links) {
        	Coord toCoord = link.getToNode().getCoord();
        	Coord fromCoord = link.getFromNode().getCoord();
        	
        	Coordinate[] coordinates = new Coordinate[2];
        	coordinates[0] = new Coordinate(toCoord.getX(), toCoord.getY());
        	coordinates[1] = new Coordinate(fromCoord.getX(), fromCoord.getY());
        	LineString lineString = factory.createLineString(coordinates);
        	
        	featureBuilder.add(lineString);
        	double cap = link.getCapacity() * 24;
        	featureBuilder.add(cap);
        	double occup = 0;//calculator.getOccupancy(link.getId()) * factor;
        	featureBuilder.add(occup);
        	featureBuilder.add(occup/cap);
        	featureBuilder.add(link.getFreespeed());
        	
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
	
	public static void main(String[] args) throws IOException {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		NetworkReaderMatsimV1 reader = new NetworkReaderMatsimV1(scenario);
		reader.parse("/home/johannes/gsv/osm/network/germany-20140909.5.xml");
		
		Network network = scenario.getNetwork();
		LinkOccupancyCalculator calc = new LinkOccupancyCalculator(null);
//		calc.reset(0);
		
		new LinkSHPWriter().write(network.getLinks().values(), calc, "/home/johannes/gsv/osm/network/germany-20140909.5.shp", 1);
	}
}
