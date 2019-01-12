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
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import vwExamples.utils.modalSplitAnalyzer.PersonValidator;

import java.util.*;

/**
 * @author saxer
 */
public class CountAgentsWithinArea {

    //Initialize SubsamplePopulation class
    static double samplePct = 0.1; //Global sample ratio
    private Map<String, PersonValidator> groups = new HashMap<>();
    static Set<String> zones = new HashSet<>();
    static Map<String, Geometry> zoneMap = new HashMap<>();
    static String shapeFile = "D:\\Axer\\MatsimDataStore\\Berlin_DRT\\input\\shapes\\Prognoseraum_EPSG_31468_cleaned_small.shp";
    static String shapeFeature = "SCHLUESSEL";
    StageActivityTypes stageActs;
    static Set<Id<Person>> relevantAgents = new HashSet<>();
    static Set<Id<Person>> allAgents = new HashSet<>();


    public static void main(String[] args) {


        String runDir = "D:\\Axer\\MatsimDataStore\\Berlin_DRT\\output\\be_as_beSmall_Beta900_4000veh_6pax\\";
        String runId = "be_as_beSmall_Beta900_4000veh_6pax.";

        readShape(shapeFile, shapeFeature);

        StreamingPopulationReader spr = new StreamingPopulationReader(ScenarioUtils.createScenario(ConfigUtils.createConfig()));
        spr.addAlgorithm(new PersonAlgorithm() {
            @Override
            public void run(Person person) {
                allAgents.add(person.getId());


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
        System.out.println("All agents: " + allAgents.size() + "  In area: " + relevantAgents.size());
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






