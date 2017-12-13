/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.contrib.accessibility.utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.accessibility.interfaces.FacilityDataExchangeInterface;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.facilities.ActivityFacility;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;

public class GeoserverUpdater implements FacilityDataExchangeInterface {

	static Logger LOG = Logger.getLogger(GeoserverUpdater.class);

	private String crs;
	private String name;
	private long cellSize;

	public GeoserverUpdater (String crs, String name, long cellSize) {
		this.crs = crs;
		this.name = name;
		this.cellSize = cellSize;
	}
	
	private Map<Tuple<ActivityFacility, Double>, Map<String,Double>> accessibilitiesMap = new HashMap<>() ;

	@Override
	public void setFacilityAccessibilities(ActivityFacility measurePoint, Double timeOfDay,	Map<String, Double> accessibilities) {
		accessibilitiesMap.put( new Tuple<>(measurePoint, timeOfDay), accessibilities ) ;
	}

	@Override
	public void finish() {
		GeometryFactory geometryFactory = new GeometryFactory();
		SimpleFeatureTypeBuilder featureTypeBuilder = createFeatureTypeBuilder();
		SimpleFeatureType featureType = featureTypeBuilder.buildFeatureType();
		DefaultFeatureCollection featureCollection = createFeatureCollection(geometryFactory, featureType);
		updateOnGeoserver(featureType, featureCollection);
	}

	private SimpleFeatureTypeBuilder createFeatureTypeBuilder() {
			SimpleFeatureTypeBuilder featureTypeBuilder = new SimpleFeatureTypeBuilder();
			featureTypeBuilder.setName(name);
			featureTypeBuilder.setCRS(MGC.getCRS(TransformationFactory.WGS84));
			featureTypeBuilder.add("the_geom", Polygon.class);
			featureTypeBuilder.add("id", Integer.class);
			featureTypeBuilder.add("time", Double.class);
			for (Modes4Accessibility mode : Modes4Accessibility.values()) {
				featureTypeBuilder.add(mode.toString(), Double.class);
			}
	//		for (ActivityFacilities facilities : additionalFacilityData) {
	//			b.add(facilities.getName(), Double.class);
	//		}
			// yyyyyy add population here
			return featureTypeBuilder;
		}

	private DefaultFeatureCollection createFeatureCollection(GeometryFactory geometryFactory, SimpleFeatureType featureType) {
		LOG.info("Start creating features from accessibility data.");
		DefaultFeatureCollection featureCollection = new DefaultFeatureCollection("internal", featureType);
		SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
		
		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(this.crs, TransformationFactory.WGS84);
		int id = 0;
		
		for (Entry<Tuple<ActivityFacility, Double>, Map<String, Double>> entry : accessibilitiesMap.entrySet()) {
			id++;
			Coord coord = entry.getKey().getFirst().getCoord() ;
			Coordinate coord1 = MGC.coord2Coordinate(transformation.transform(CoordUtils.createCoord(coord.getX() - (cellSize / 2), coord.getY() - (cellSize / 2))));
			Coordinate coord2 = MGC.coord2Coordinate(transformation.transform(CoordUtils.createCoord(coord.getX() + (cellSize / 2), coord.getY() - (cellSize / 2))));
			Coordinate coord3 = MGC.coord2Coordinate(transformation.transform(CoordUtils.createCoord(coord.getX() + (cellSize / 2), coord.getY() + (cellSize / 2))));
			Coordinate coord4 = MGC.coord2Coordinate(transformation.transform(CoordUtils.createCoord(coord.getX() - (cellSize / 2), coord.getY() + (cellSize / 2))));
			
			featureBuilder.add(geometryFactory.createPolygon(new Coordinate[]{coord1, coord2, coord3, coord4, coord1}));
			featureBuilder.add(id);
			featureBuilder.add(entry.getKey().getSecond());
			
			for (Modes4Accessibility modeEnum : Modes4Accessibility.values()) {
				String mode = modeEnum.toString();
				Double accessibility = entry.getValue().get(mode);
				if (accessibility != null && !Double.isNaN(accessibility)) {
					featureBuilder.add(accessibility);
				} else {
					featureBuilder.add(null);
				}
			}
			// yyyyyy write population density here. Probably not aggregated to grid.

			SimpleFeature feature = featureBuilder.buildFeature(null);
			featureCollection.add(feature);
		}
		LOG.info("Finished creating features from accessibility data.");
		return featureCollection;
	}

	private void updateOnGeoserver(SimpleFeatureType featureType, DefaultFeatureCollection featureCollection) {
		LOG.info("Start pushing accessibility data into PostGIS database.");
		try {
			Map<String,Object> params = new HashMap<>();
			params.put( "dbtype", "postgis");
			params.put( "host", "geo.vsp.tu-berlin.de");
			params.put( "port", 5432);
			params.put( "schema", "public");
			params.put( "database", "vspgeodb");
			params.put( "user", "vsppostgres");
			params.put( "passwd", "jafs30_A");
			DataStore dataStore = DataStoreFinder.getDataStore(params);
			System.out.println("dataStore = " + dataStore);
			
			// Remove schema in case it already exists
			try {
				dataStore.removeSchema(name);
			} catch (IllegalArgumentException e) {
				LOG.warn("Could not remove schema. Probably, it has not existed yet.");
			}
			
			dataStore.createSchema(featureType);
			SimpleFeatureStore featureStore = (SimpleFeatureStore) dataStore.getFeatureSource(name);
			featureStore.addFeatures(featureCollection);
			dataStore.dispose() ;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		LOG.info("Finished pushing accessibility data into PostGIS database.");
		// Re-publish layer using the REST api (of geoserver; the above is the postgis db) if we want to automatically recompute the bounding box.  mz & kai, nov'15
	}
}