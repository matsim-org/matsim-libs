/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
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


//This script has been adapted in order to write only trips of agents that are living within the defined shape.
//Only export trips within defined shape

package vwExamples.utils.tripAnalyzer;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.opengis.feature.simple.SimpleFeature;


public class RunExperiencedTripsAnalysis {

    static Set<Id<Person>> relevantAgents = new HashSet<>();
    static Map<String, Geometry> zoneMap = new HashMap<>();
    static Set<String> zones = new HashSet<>();
    static String shapeFile = "D:\\Thiel\\Programme\\MatSim\\03_HannoverDRT\\Input\\shp\\Hannover_Service_Area.shp";
    static String shapeFeature = "NO";

    public static void main(String[] args) {

        String runDirectory = "D:\\Thiel\\Programme\\MatSim\\03_HannoverDRT\\Output\\Moia_4\\";
        String runId = "Moia_4.";
        String runPrefix = runDirectory + "/" + runId;

        boolean useTransitSchedule = true;

        Set<String> monitoredModes = new HashSet<>();
        monitoredModes.add("pt");
        monitoredModes.add("transit_walk");
        monitoredModes.add("drt");
        monitoredModes.add("drt_walk");
        monitoredModes.add("access_walk");
        monitoredModes.add("egress_walk");
        monitoredModes.add("car");
        monitoredModes.add("walk");
        monitoredModes.add("bike");

        readShape(shapeFile, shapeFeature);


        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile(runPrefix + "output_network.xml.gz");

        if (useTransitSchedule) {
            new TransitScheduleReader(scenario).readFile(runPrefix + "output_transitSchedule.xml.gz");
        }
        StreamingPopulationReader spr = new StreamingPopulationReader(scenario);
        spr.addAlgorithm(new PersonAlgorithm() {
            @Override
            public void run(Person person) {
                relevantAgents.add(person.getId());


            	//Take only specific agents
				for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
					if (pe instanceof Activity) {
						if (((Activity) pe).getType().contains("home")) {

							Activity activity = ((Activity) pe);
							Coord coord = activity.getCoord();
							if (vwExamples.utils.modalSplitAnalyzer.modalSplitEvaluator.isWithinZone(coord, zoneMap)) {
								relevantAgents.add(person.getId());
								// System.out.println(person.getId().toString());
								break;

							}

						}
					}
				}

            }

        });
        spr.readFile(runPrefix + "output_plans.xml.gz");

        //System.out.println(relevantAgents.size());


        // Analysis
        EventsManager events = EventsUtils.createEventsManager();


        Set<Id<Link>> monitoredStartAndEndLinks = new HashSet<>();

//        DrtPtTripEventHandler eventHandler = new DrtPtTripEventHandler(scenario.getNetwork(), scenario.getTransitSchedule(),
//                monitoredModes, monitoredStartAndEndLinks);
//        events.addHandler(eventHandler);
//        new DrtEventsReader(events).readFile(runPrefix + "output_events.xml.gz");
//        System.out.println("Start writing trips of " + eventHandler.getPerson2ExperiencedTrips().size() + " agents.");
//        ExperiencedTripsWriter tripsWriter = new ExperiencedTripsWriter(runPrefix +
//                "experiencedTrips.csv",
//                eventHandler.getPerson2ExperiencedTrips(), monitoredModes, scenario.getNetwork(), relevantAgents, zoneMap);
//        tripsWriter.writeExperiencedTrips();
//        ExperiencedTripsWriter legsWriter = new ExperiencedTripsWriter(runPrefix +
//                "experiencedLegs.csv",
//                eventHandler.getPerson2ExperiencedTrips(), monitoredModes, scenario.getNetwork(), relevantAgents, zoneMap);
//        legsWriter.writeExperiencedLegs();
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
