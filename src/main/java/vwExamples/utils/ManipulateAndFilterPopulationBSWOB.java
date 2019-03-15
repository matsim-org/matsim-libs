/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package vwExamples.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.pt.router.TransitActsRemover;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.opengis.feature.simple.SimpleFeature;

/**
 * @author saxer
 */
public class ManipulateAndFilterPopulationBSWOB {

    //Initialize SubsamplePopulation class
    Set<String> zones = new HashSet<>();
    Map<String, Geometry> zoneMap = new HashMap<>();
    String shapeFile = "C:\\Temp\\BS_DRT\\input\\shapes\\wvi-zones.shp";
    int zoneRange[] = {300, 400};
    static String serachMode = "def";
    static String newMode = "def";
    String shapeFeature = "NO";
    static double samplePct = 0.1; //Global sample ratio
    static double replancementPct = 0.0; //Ratio of mode substitution
    static int instanceNumber = 5;

    //Constructor which reads the shape file for later use!
    public ManipulateAndFilterPopulationBSWOB() {
        readShape(this.shapeFile, this.shapeFeature);

    }

    public static void main(String[] args) {

        for (int i = 1; i <= instanceNumber; i++) {

            ManipulateAndFilterPopulationBSWOB manipulateAndFilterPopulation = new ManipulateAndFilterPopulationBSWOB();
            //Create a Scenario
            Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
            //Fill this Scenario with a population.
            new PopulationReader(scenario).readFile("C:\\Temp\\BS_DRT\\input\\population\\vw219.output_plans.xml.gz");
            new TransitScheduleReader(scenario).readFile("C:\\Temp\\BS_DRT\\input\\transit\\vw219.output_transitSchedule.xml.gz");
            String filteredPopDesination = ("C:\\Temp\\BS_DRT\\input\\population\\vw219_it_" + i + "_sampleRate" + samplePct + "replaceRate_" + replancementPct + "_" + serachMode + "_" + newMode + ".xml.gz");
            StreamingPopulationWriter filteredPop = new StreamingPopulationWriter();
            filteredPop.startStreaming(filteredPopDesination);
            int drtTrips = 0;

            for (Person p : scenario.getPopulation().getPersons().values()) {
                Plan plan = p.getSelectedPlan();


                //Sample a certain percentage of the hole population
                if (MatsimRandom.getRandom().nextDouble() < samplePct) {

                    //Check whether this person's home location is located within a relevant zone, whereas the zone needs to fit with the zonePrefix
                    //Otherwise, we do not touch this person
                    //if (checkAgentLocationAndActivity(p,manipulateAndFilterPopulation.searchedActivityName,manipulateAndFilterPopulation.zoneMap,manipulateAndFilterPopulation.zonePrefix))

                    //{

                    //Modify only a certain percentage of relevant Agents
                    if (MatsimRandom.getRandom().nextDouble() < replancementPct) {
                        new TransitActsRemover().run(plan);


                        //If it is a relevant person, we assign certain legs with person's selected plans to a new mode
                        for (PlanElement pe : plan.getPlanElements()) {

                            if (pe instanceof Leg) {
                                Leg leg = ((Leg) pe);
                                if (leg.getMode().equals(serachMode)) {

                                    if (checkAgentLegWithinZone(plan, leg, manipulateAndFilterPopulation.zoneMap, manipulateAndFilterPopulation.zoneRange)) {

                                        //									if (getPtTransportMode(leg,transitLines).equals("bus"))
                                        //										{
                                        System.out.println("Replaced pt leg with " + newMode);
                                        //Write newMode into Leg
                                        leg.setMode(newMode);
                                        //Remove route from leg
                                        leg.setTravelTime(0.0);
                                        //System.out.println(leg);

                                        drtTrips++;
                                        //										}

                                    }
                                }
                            }


                        }


                    }
                    //}


                    PersonUtils.removeUnselectedPlans(p);
                    filteredPop.writePerson(p);
                }
            }

            filteredPop.closeStreaming();
            System.out.println("Modified trips:" + drtTrips);
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

    public static boolean checkAgentLocationAndActivity(Person person, String searchedActivityName, Map<String, Geometry> zoneMap2, int[] zoneRange) {
        boolean relevantAgent = false;


        for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
            if (pe instanceof Activity) {
                if (((Activity) pe).getType().contains(searchedActivityName)) {

                    Activity activity = ((Activity) pe);
                    Coord coord = activity.getCoord();
                    if (isWithinZone(coord, zoneMap2, zoneRange)) {
                        relevantAgent = true;


                    }

                }
            }
        }

        if (relevantAgent) {
            System.out.println("Relavent Agend: " + person.getId().toString());
            return true;
        } else return false;
    }


    public static boolean checkAgentLegWithinZone(Plan plan, Leg leg, Map<String, Geometry> zoneMap2, int[] zoneRange) {
        boolean prevActInZone = false;
        boolean nextActInZone = false;

        Activity prevAct = PopulationUtils.getPreviousActivity(plan, leg);
        Activity nextAct = PopulationUtils.getNextActivity(plan, leg);


        if (isWithinZone(prevAct.getCoord(), zoneMap2, zoneRange)) prevActInZone = true;
        if (isWithinZone(nextAct.getCoord(), zoneMap2, zoneRange)) nextActInZone = true;


        if ((prevActInZone == true) && (nextActInZone == true)) {
            System.out.println("Leg in Zone: " + plan.getPerson().getId().toString());
            return true;
        } else return false;
    }


    public static String getPtTransportMode(Leg leg, Map<Id<TransitLine>, TransitLine> transitLines) {
        //Initialize variables
        String transportMode = null;
        String transitLineID = null;

        //We could get the PtTransportMode only if a route is already stored in agent's plan
        if (leg.getRoute() != null) {
            String routeDescription = leg.getRoute().getRouteDescription();
            String[] routeDescriptionElements = routeDescription.split("===");
            transitLineID = routeDescriptionElements[2];

//	System.out.println(transitLineID);

            Id<TransitLine> transitLineIDDummy = Id.create(transitLineID, TransitLine.class);

            if (transitLineID != null) {
                transportMode = transitLines.get(transitLineIDDummy).getRoutes().entrySet().iterator().next().getValue().getTransportMode();

            }

        } else {
            throw new RuntimeException("Public Transport Route is missing in agente's plan.");
        }

        return transportMode;


    }

    public static boolean isWithinZone(Coord coord, Map<String, Geometry> zoneMap, int[] zoneRange) {
        //Function assumes EPSG:25832

        boolean relevantCoord = false;
        for (String zone : zoneMap.keySet()) {

            //If the zone does not fit to the require zonePrefix
            if (!(Integer.parseInt(zone) > zoneRange[0] && Integer.parseInt(zone) < zoneRange[1])) continue;
            Geometry geometry = zoneMap.get(zone);
            if (geometry.contains(MGC.coord2Point(coord))) relevantCoord = true;

        }
        if (relevantCoord) return true;
        else return false;

    }


}
