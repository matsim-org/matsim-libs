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

import javafx.util.Pair;
import org.geotools.data.shapefile.shp.ShapeType;
import org.geotools.data.shapefile.shp.ShapefileWriter;
import org.geotools.feature.SchemaException;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.locationtech.jts.geom.*;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.pt.utils.TransitScheduleValidator;
import org.matsim.vehicles.MatsimVehicleWriter;
import org.matsim.vehicles.Vehicles;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static org.locationtech.jts.precision.EnhancedPrecisionOp.buffer;

public class RunTransitRouteTrimmerBerlinExample {

    public static void main(String[] args) throws IOException, SchemaException {
        final String inScheduleFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-transit-schedule.xml.gz";
        final String inVehiclesFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-transit-vehicles.xml.gz";
        final String inNetworkFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network.xml.gz";
        final String zoneShpFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/shp-files/shp-inner-city-area/inner-city-area.shp";
        final String shapeFileCRS = "EPSG:31468";
        String outputPath = "output/";

        final String scenarioCRS = "EPSG:31468";

        // Make config and scenario
        Config config = ConfigUtils.createConfig();
        config.network().setInputFile(inNetworkFile);
        config.transit().setTransitScheduleFile(inScheduleFile);
        config.transit().setVehiclesFile(inVehiclesFile);
        Scenario scenario = ScenarioUtils.loadScenario(config);

        // Designate bus stops as hubs, if they are a certain radius away from rail stations
        designateBusStopsAsHubs(outputPath, 300, 1, scenario);

        // Run Trimmer
        Set<Id<TransitLine>> linesToModify = scenario.getTransitSchedule().getTransitLines().keySet();
        CoordinateTransformation transformer = TransformationFactory.getCoordinateTransformation(scenarioCRS, shapeFileCRS);
        Set<Id<TransitStopFacility>> stopsInZone = TransitRouteTrimmerUtils.getStopsInZone(scenario.getTransitSchedule(), IOUtils.resolveFileOrResource(zoneShpFile), transformer);
        Pair<TransitSchedule, Vehicles> results = TransitRouteTrimmer.splitRoute(scenario.getTransitSchedule(), scenario.getTransitVehicles(), stopsInZone, linesToModify,
                true, Collections.singleton("bus"), 3, true, true, true, 0);

        TransitSchedule transitScheduleNew = results.getKey();
        Vehicles vehiclesNew = results.getValue();

        TransitScheduleValidator.ValidationResult validationResult = TransitScheduleValidator.validateAll(transitScheduleNew, scenario.getNetwork());
        System.out.println(validationResult.getErrors());

        outputPath = outputPath.endsWith("/") ? outputPath : outputPath.concat("/");
        new File(outputPath).mkdirs();
        TransitRouteTrimmerUtils.transitSchedule2ShapeFile(transitScheduleNew, outputPath + "trimmed-transitRoutes.shp", scenarioCRS.substring(scenarioCRS.lastIndexOf(":") + 1));
        new TransitScheduleWriter(transitScheduleNew).writeFile(outputPath + "trimmed-transitSchedule.xml.gz");
        new MatsimVehicleWriter(vehiclesNew).writeFile(outputPath + "vehiclesNew.xml.gz");
        //copy network (SimWrapper needs it next to the schedule)
        new NetworkWriter(scenario.getNetwork()).write(outputPath + inNetworkFile.substring(inNetworkFile.lastIndexOf("/") + 1));
    }

    private static void designateBusStopsAsHubs(String outputPath, int bufferRadius, int hubReach, Scenario scenario) throws IOException {

        TransitSchedule transitScheduleOld = scenario.getTransitSchedule();

        // Collect all rail stations
        Collection<TransitStopFacility> allStations = transitScheduleOld.getFacilities().values();
        List<TransitStopFacility> railStations = allStations.stream()
                .filter(x -> x.getAttributes().getAsMap().containsKey("stopFilter"))
                .filter(x -> x.getAttributes().getAttribute("stopFilter").equals("station_S/U/RE/RB"))
                .collect(Collectors.toList());

        List<Id<TransitStopFacility>> railStationIds = railStations.stream()
                .map(x -> x.getId())
                .collect(Collectors.toList());

        // Generate buffer geometry around rail stations
        GeometryFactory GEOMETRY_FACTORY = JTSFactoryFinder.getGeometryFactory();
        List<Geometry> bufferGeoList = new ArrayList<>();
        List<Geometry> railStopGeoList = new ArrayList<>();
        List<Geometry> busStopGeoList = new ArrayList<>();

        for (TransitStopFacility stop : railStations) {
            double x = stop.getCoord().getX();
            double y = stop.getCoord().getY();
            Coordinate coordinate = new Coordinate(x, y);
            Point point = GEOMETRY_FACTORY.createPoint(coordinate);
            Geometry buffer = buffer(point, bufferRadius);
            bufferGeoList.add(buffer);
        }

        Geometry railBufferGeo = GEOMETRY_FACTORY.buildGeometry(bufferGeoList).union();

        Map<Id<TransitStopFacility>,Coordinate > StopId2CoordMap = new HashMap<>();

        //Generate Geometries for all rail stations and all non-rail stations (assumed to be bus)
        for (TransitStopFacility stop : allStations) {
            Coordinate coordinate = new Coordinate(stop.getCoord().getX(), stop.getCoord().getY());
            Point point = GEOMETRY_FACTORY.createPoint(coordinate);
            StopId2CoordMap.put(stop.getId(),coordinate);
            if (railStationIds.contains(stop.getId())) {
                railStopGeoList.add(point);
            } else {
                busStopGeoList.add(point);
            }
        }

        Geometry busStopsAll = GEOMETRY_FACTORY.buildGeometry(busStopGeoList);
        Geometry railStopsGeo = GEOMETRY_FACTORY.buildGeometry(railStopGeoList);

        // Find bus stop geometries that are within rail buffer
        Geometry busStopsInBuffer = busStopsAll.intersection(railBufferGeo);

        // Find corresponding busStopFacilityIds
        List<Id<TransitStopFacility>> busStopsInBufferIds = new ArrayList<>();
        Coordinate[] coordinates = busStopsInBuffer.getCoordinates();

        for (Coordinate coord : coordinates) {
            busStopsInBufferIds.addAll(StopId2CoordMap.entrySet().stream().filter(x -> x.getValue().equals(coord)).map(x -> x.getKey()).collect(Collectors.toList()));
        }

        // Add hub attribute to bus stops within buffer of rail stops
        for (Id<TransitStopFacility> id : busStopsInBufferIds) {
            scenario.getTransitSchedule().getFacilities().get(id).getAttributes().putAttribute("hub-reach", hubReach);
        }

        { // Write shape files for rail stops, buffers around rail stops, and bus stops within buffer
                    writeGeometryCollection2ShapeFile(outputPath + "railStopGeo-300", ShapeType.POINT,
                            (GeometryCollection) railStopsGeo);

                    writeGeometryCollection2ShapeFile(outputPath +"railBufferGeo-300", ShapeType.POLYGON,
                            (GeometryCollection) railBufferGeo);

                    writeGeometryCollection2ShapeFile(outputPath +"busStopsInBufferGeo-300", ShapeType.POINT,
                            (GeometryCollection) busStopsInBuffer);
        }
    }

    public static void writeGeometryCollection2ShapeFile(String fileName, ShapeType shapeType, GeometryCollection geometryCollection) throws IOException {
        RandomAccessFile shp = new RandomAccessFile(fileName + ".shp", "rw");
        RandomAccessFile shx = new RandomAccessFile(fileName + ".shx", "rw");
        ShapefileWriter writer = new ShapefileWriter(shp.getChannel(), shx.getChannel());
        writer.write(geometryCollection, shapeType);
    }
}

