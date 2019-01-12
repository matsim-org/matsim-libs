/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package vwExamples.utils.delays;

import analysis.drtOccupancy.DynModeTripsAnalyser;
import com.vividsolutions.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import java.util.*;

public class RunTravelDelayAnalysis {

    static Set<Id<Person>> relevantAgents = new HashSet<>();
    static Map<String, Geometry> zoneMap = new HashMap<>();
    static Set<String> zones = new HashSet<>();
    static String shapeFile = "D:\\Axer\\MatsimDataStore\\WOB_BS_DRT\\BS\\input\\shapes\\wvi-zones-bsonly.shp";
    static String shapeFeature = "NO";

    public static void main(String[] args) {

        String runDir = "D:\\Axer\\MatsimDataStore\\WOB_BS_DRT\\BS\\output\\bs_as_boston_1200veh_6pax\\";
        String runId = "bs_as_boston_1200veh_6pax.";

        readShape(shapeFile, shapeFeature);

        StreamingPopulationReader spr = new StreamingPopulationReader(ScenarioUtils.createScenario(ConfigUtils.createConfig()));
        spr.addAlgorithm(new PersonAlgorithm() {
            @Override
            public void run(Person person) {
                //relevantAgents.add(person.getId());


                for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
                    if (pe instanceof Activity) {
                        if (((Activity) pe).getType().contains("home")) {

                            Activity activity = ((Activity) pe);
                            Coord coord = activity.getCoord();
                            if (vwExamples.utils.modalSplitAnalyzer.modalSplitEvaluator.isWithinZone(coord, zoneMap)) {
                                relevantAgents.add(person.getId());
                                //System.out.println(person.getId().toString());
                                break;

                            }

                        }
                    }
                }

            }

        });
        spr.readFile(runDir + runId + "output_plans.xml.gz");


        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(runDir + runId + "output_network.xml.gz");
        TravelDelayCalculator tdc = new TravelDelayCalculator(network, relevantAgents);

        EventsManager events = EventsUtils.createEventsManager();
        events.addHandler(tdc);
        new MatsimEventsReader(events).readFile(runDir + runId + "output_events.xml.gz");
        DynModeTripsAnalyser.collection2Text(tdc.getTrips(), runDir + "delays.csv", "PersonId;ArrivalTime;FreespeedTravelTime;ActualTravelTime");
    }

    public static void readShape(String shapeFile, String featureKeyInShapeFile) {
        Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(shapeFile);
        for (SimpleFeature feature : features) {
            String id = feature.getAttribute(featureKeyInShapeFile).toString();
            Geometry geometry = (Geometry) feature.getDefaultGeometry();
            zones.add(id);
            zoneMap.put(id, geometry);
        }
    }
}
