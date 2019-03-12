/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

/**
 *
 */
package vwExamples.utils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.analysis.vsp.traveltimedistance.CarTrip;
import org.matsim.contrib.analysis.vsp.traveltimedistance.CarTripsExtractor;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;

/**
 * @author axer
 */

/**
 *
 */
public class ExportCarTripsWithZones {
    Set<String> zones = new HashSet<>();
    Map<String, Geometry> zoneMap = new HashMap<>();
    String shapeFile = "D:\\Axer\\CEMDAP\\cemdap-vw\\add_data\\shp\\wvi-zones.shp";
    String shapeFeature = "NO";

    //Constructor which reads the shape file for later use!
    public ExportCarTripsWithZones() {
        readShape(this.shapeFile, this.shapeFeature);
    }

    public static void main(String[] args) {

        String folder = "D:\\Axer\\MatsimDataStore\\BaseCases\\";
        String run = "vw212";

        String eventsFile = folder + run + "/" + run + ".output_events.xml.gz";
        String plansFile = folder + run + "/" + run + ".output_plans.xml.gz";
        String networkFile = folder + run + "/" + run + ".output_network.xml.gz";
        String outFileName = folder + run + "/" + run + ".carTrips.csv";

        ExportCarTripsWithZones exportCarTripsWithZones = new ExportCarTripsWithZones();

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new PopulationReader(scenario).readFile(plansFile);
        new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);


        EventsManager events = EventsUtils.createEventsManager();

        CarTripsExtractor carTripsExtractor = new CarTripsExtractor(scenario.getPopulation().getPersons().keySet(), scenario.getNetwork());
        events.addHandler(carTripsExtractor);
        new MatsimEventsReader(events).readFile(eventsFile);
        List<CarTrip> carTrips = carTripsExtractor.getTrips();

        writeTravelTimes(outFileName, carTrips, exportCarTripsWithZones.zoneMap);


    }

    static void writeTravelTimes(String filename, List<CarTrip> trips, Map<String, Geometry> zoneMap) {
        BufferedWriter bw = IOUtils.getBufferedWriter(filename);
        try {
            bw.append("agent;departureTime;fromX;fromY;toX;toY;traveltimeActual;null;distance;null;departureZone;arrivalZone");
            for (CarTrip trip : trips) {

                String departureZone = getZone(trip.getDepartureLocation(), zoneMap);
                String arrivalZone = getZone(trip.getArrivalLocation(), zoneMap);

                bw.newLine();
                bw.append(trip.toString() + ";" + departureZone + ";" + arrivalZone);

            }

            bw.flush();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void readShape(String shapeFile, String featureKeyInShapeFile) {
        Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(shapeFile);
        for (SimpleFeature feature : features) {
            String id = feature.getAttribute(featureKeyInShapeFile).toString();
            Geometry geometry = (Geometry) feature.getDefaultGeometry();
            this.zones.add(id);
            this.zoneMap.put(id, geometry);
        }
    }

    public static String getZone(Coord coord, Map<String, Geometry> zoneMap) {
        //Function assumes EPSG:25832

        for (String zone : zoneMap.keySet()) {

            Geometry geometry = zoneMap.get(zone);
            if (geometry.contains(MGC.coord2Point(coord))) return zone;

        }
        return "other";


    }


}
