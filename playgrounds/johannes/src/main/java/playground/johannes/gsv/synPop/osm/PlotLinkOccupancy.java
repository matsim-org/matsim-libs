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

package playground.johannes.gsv.synPop.osm;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import gnu.trove.iterator.TObjectDoubleIterator;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.apache.log4j.Logger;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.common.gis.CRSUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author johannes
 * 
 */
public class PlotLinkOccupancy {

	private static final Logger logger = Logger.getLogger(PlotLinkOccupancy.class);

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);

		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario);
		netReader.readFile(args[0]);
		Network network = scenario.getNetwork();

		TObjectDoubleHashMap<String> values = new TObjectDoubleHashMap<>();

		BufferedReader reader = new BufferedReader(new FileReader(args[1]));
		String line = reader.readLine();
		while ((line = reader.readLine()) != null) {
			String tokens[] = line.split("\t");
			values.put(tokens[0], Double.parseDouble(tokens[1]));
		}
		reader.close();

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

		TObjectDoubleIterator<String> it = values.iterator();
		for (int i = 0; i < values.size(); i++) {
			it.advance();
			Id<Link> linkId = Id.create(it.key(), Link.class);
			Link link = network.getLinks().get(linkId);
			if (link != null) {
				double value = it.value();
				if (value == 0) {
					logger.warn(String.format("Skipping link %s because of no occupancy.", link.getId().toString()));
				} else {
					Coord toCoord = link.getToNode().getCoord();
					Coord fromCoord = link.getFromNode().getCoord();

					Coordinate[] coordinates = new Coordinate[2];
					coordinates[0] = new Coordinate(toCoord.getX(), toCoord.getY());
					coordinates[1] = new Coordinate(fromCoord.getX(), fromCoord.getY());
					LineString lineString = factory.createLineString(coordinates);

					featureBuilder.add(lineString);
					double cap = link.getCapacity() * 24;
					featureBuilder.add(cap);
					double occup = it.value() * 50;
					featureBuilder.add(occup);
					featureBuilder.add(occup / cap);
					featureBuilder.add(link.getFreespeed());

					SimpleFeature feature = featureBuilder.buildFeature(null);
					collection.add(feature);
				}
			} else {
				logger.warn(String.format("Link with id %s not found.", linkId.toString()));
			}
		}

		File newFile = new File(args[2]);

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

}
