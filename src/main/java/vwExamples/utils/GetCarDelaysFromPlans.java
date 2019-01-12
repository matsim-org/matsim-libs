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

import com.vividsolutions.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;

/**
 * @author saxer
 */
public class GetCarDelaysFromPlans {

    static Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
    static Map<String, Geometry> zoneMap = new HashMap<>();
    static String shapeFile = "D:\\Axer\\MatsimDataStore\\Berlin_DRT\\input\\shapes\\Prognoseraum_EPSG_31468_cleaned_small.shp";
    static String shapeFeature = "SCHLUESSEL";
    static Set<String> zones = new HashSet<>();
    static List<String> carLegDelayList = new ArrayList<String>();

    public static void main(String[] args) {


        //Create a Scenario
        readShape(shapeFile, shapeFeature);

        //Fill this Scenario with a population.
        new PopulationReader(scenario).readFile("D:\\Axer\\MatsimDataStore\\Berlin_DRT\\output\\be_as_beSmall_4500veh_6pax\\be_as_beSmall_4500veh_6pax.output_plans.xml.gz");
        new MatsimNetworkReader(scenario.getNetwork()).readFile("D:\\Axer\\MatsimDataStore\\Berlin_DRT\\input\\network\\modifiedNetwork.xml.gz");

        for (Person person : scenario.getPopulation().getPersons().values()) {
            List<Trip> trips = TripStructureUtils.getTrips(person.getSelectedPlan(), EmptyStageActivityTypes.INSTANCE);


            for (Trip trip : trips) {

                Coord from = trip.getOriginActivity().getCoord();
                Coord to = trip.getDestinationActivity().getCoord();

                if (vwExamples.utils.modalSplitAnalyzer.modalSplitEvaluator.isWithinZone(from, zoneMap) &&
                        vwExamples.utils.modalSplitAnalyzer.modalSplitEvaluator.isWithinZone(to, zoneMap)) {

                    List<Leg> legs = trip.getLegsOnly();

                    for (Leg leg : legs) {
                        //Reset values
                        double usedTravelTime = 0;
                        double ffTravelTime = 0;
                        double delay = 0;

                        if (leg.getMode().equals(TransportMode.car)) {
                            usedTravelTime = leg.getTravelTime();
                            ffTravelTime = getFreeFlowTravelTime(leg.getRoute());


                            delay = usedTravelTime - ffTravelTime;
                            if (delay < 0) delay = 0;
//								System.out.println(delay);
                            carLegDelayList.add(person.getId().toString() + ";" + usedTravelTime + ";" + ffTravelTime + ";" + delay + ";" + from.getX() + ";" + from.getY() + ";" + to.getX() + ";" + to.getY() + "\n");
                        }


                    }
                }
            }

        }


        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new File("D:\\Axer\\MatsimDataStore\\Berlin_DRT\\output\\be_as_beSmall_4500veh_6pax\\delayData.csv"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        //StringBuilder builder = new StringBuilder();
        String ColumnNamesList = "AgentID;travelTime;freeFlowTravelTime;delay;fromX;fromY;toX;toY\n";
        // No need give the headers Like: id, Name on builder.append
//		builder.append(ColumnNamesList +"\n");
//		builder.append("1"+",");
//		builder.append("Chola");
//		builder.append('\n');

        pw.write(ColumnNamesList);

        for (String entry : carLegDelayList) {
            pw.write(entry);
        }

        pw.close();
        System.out.println("done!");


    }

    public static double getFreeFlowTravelTime(Route route) {

        double ffTravelTime = 0;


        List<String> LinkList = new ArrayList<String>(Arrays.asList(route.getRouteDescription().split(" ")));

        if (LinkList.isEmpty()) {
            System.out.println("Caution");
        }

        for (String linkid : LinkList) {
            Id<Link> linkId = Id.createLinkId(linkid);

            ffTravelTime += NetworkUtils.getFreespeedTravelTime(scenario.getNetwork().getLinks().get(linkId));

        }

        return ffTravelTime;
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






