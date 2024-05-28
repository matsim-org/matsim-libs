/* *********************************************************************** *
 * project: org.matsim.*
 * EditRoutesTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2021 by the members listed in the COPYING,        *
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

package playground.vsp.pt.transitRouteTrimmer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.api.data.Transaction;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.*;

public class TransitRouteTrimmerUtils {
    private static final Logger log = LogManager.getLogger(TransitRouteTrimmer.class);

    // This tool creates a LineString for each route in a TransitSchedule, based on the coordinates of the StopFacilities.
    // The collection of LineStrings is then exported to a ESRI shape file.
    public static void transitSchedule2ShapeFile(TransitSchedule tS, String outputFilename, String epsgCode ) throws SchemaException, IOException {

        File newFile = new File(outputFilename);

        final SimpleFeatureType TYPE =
                DataUtilities.createType(
                        "Link",
                        "the_geom:LineString:srid=" + epsgCode + ","
                                + // <- the geometry attribute: Point type
                                "name:String,"
//                                + // <- a String attribute
//                                "number:Integer" // a number attribute
                );
        System.out.println("TYPE:" + TYPE);

        List<SimpleFeature> features = new ArrayList<>();

        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);

        for(TransitLine line : tS.getTransitLines().values()){
            for (TransitRoute route : line.getRoutes().values()) {

                List<TransitRouteStop> stops = route.getStops();
                Coordinate[] coordinates = new Coordinate[stops.size()] ;
                for (int i = 0; i < stops.size(); i++) {
                    TransitRouteStop stop = stops.get(i);
                    Coord coord = stop.getStopFacility().getCoord();
                    Coordinate coordinate = new Coordinate(coord.getX(), coord.getY());
                    coordinates[i]=coordinate;
                }
                if (coordinates.length == 1) {
                    continue;
                }
                LineString routeString = geometryFactory.createLineString(coordinates);
                String routeName = route.getId().toString();
                featureBuilder.add(routeString);
                featureBuilder.add(routeName);
                SimpleFeature feature = featureBuilder.buildFeature(null);
                features.add(feature);
            }
        }


        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();

        Map<String, Serializable> params = new HashMap<>();
        params.put("url", newFile.toURI().toURL());
        params.put("create spatial index", Boolean.TRUE);

        ShapefileDataStore newDataStore =
                (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);


        newDataStore.createSchema(TYPE);

        /*
         * Write the features to the shapefile
         */
        Transaction transaction = new DefaultTransaction("create");

        String typeName = newDataStore.getTypeNames()[0];
        SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);
        SimpleFeatureType SHAPE_TYPE = featureSource.getSchema();

        System.out.println("SHAPE:" + SHAPE_TYPE);

        if (featureSource instanceof SimpleFeatureStore) {
            SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;

            SimpleFeatureCollection collection = new ListFeatureCollection(TYPE, features);
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

    public static Set<Id<TransitLine>> filterTransitLinesForMode(Collection<TransitLine> allLines, Set<String> modes2Trim) {
        Set<Id<TransitLine>> lines2Modify = new HashSet<>();

        for (TransitLine line : allLines) {
            if (allRoutesInList(line, modes2Trim)) {
                lines2Modify.add(line.getId());
            }
        }

        return lines2Modify;
    }

    private static boolean allRoutesInList(TransitLine line, Set<String> modes2Trim) {
        for (TransitRoute route : line.getRoutes().values()) {
            if (!modes2Trim.contains(route.getTransportMode())) {
                return false;
            }
        }
        return true;
    }

    static double pctOfStopsInZone(TransitRoute route, Set<Id<TransitStopFacility>> stopsInZone) {
        double inAreaCount = 0.;
        for (TransitRouteStop stop : route.getStops()) {
            if (stopsInZone.contains(stop.getStopFacility().getId())) {
                inAreaCount++;
            }
        }
        return inAreaCount / route.getStops().size();
    }

    static void countLinesInOut(TransitSchedule tS, Set<Id<TransitStopFacility>> stopsInZone) {
        int inCount = 0;
        int outCount = 0;
        int wrongCount = 0;
        int halfCount = 0;
        int totalCount = 0;

        for (TransitLine line : tS.getTransitLines().values()) {
            for (TransitRoute route : line.getRoutes().values()) {
                totalCount++;
                ArrayList<Boolean> inOutList = new ArrayList<>();
                for (TransitRouteStop stop : route.getStops()) {
                    Id<TransitStopFacility> id = stop.getStopFacility().getId();
                    inOutList.add(stopsInZone.contains(id));
                }
                if (inOutList.contains(true) && inOutList.contains(false)) {
                    halfCount++;
                } else if (inOutList.contains(true)) {
                    inCount++;
                } else if (inOutList.contains(false)) {
                    outCount++;
                } else {
                    wrongCount++;
                }
            }
        }

        System.out.printf("in: %d, out: %d, half: %d, wrong: %d, total: %d %n", inCount, outCount, halfCount, wrongCount, totalCount);

    }

    public static Set<Id<TransitStopFacility>> getStopsInZone(TransitSchedule transitSchedule, URL zoneShpFileUrl) {
        List<PreparedGeometry> geometries = ShpGeometryUtils.loadPreparedGeometries(zoneShpFileUrl);
        Set<Id<TransitStopFacility>> stopsInZone = new HashSet<>();
        for (TransitStopFacility stop : transitSchedule.getFacilities().values()) {
            if (ShpGeometryUtils.isCoordInPreparedGeometries(stop.getCoord(), geometries)) {
                stopsInZone.add(stop.getId());
            }
        }

        return stopsInZone;
    }

    public static Set<Id<TransitStopFacility>> getStopsInZone(TransitSchedule transitSchedule, URL zoneShpFileUrl, CoordinateTransformation stopCoord2ShapeFileCrsTransformer) {

        List<PreparedGeometry> geometries = ShpGeometryUtils.loadPreparedGeometries(zoneShpFileUrl);
        Set<Id<TransitStopFacility>> stopsInZone = new HashSet<>();
        for (TransitStopFacility stop : transitSchedule.getFacilities().values()) {
            Coord transformed = stopCoord2ShapeFileCrsTransformer.transform(stop.getCoord());
            if (ShpGeometryUtils.isCoordInPreparedGeometries(transformed, geometries)) {
                stopsInZone.add(stop.getId());
            }
        }

        return stopsInZone;
    }

}
