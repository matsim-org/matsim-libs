/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
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
 * *********************************************************************** *
 */

package playground.boescpa.lib.tools.spatialCutting;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.analysis.filters.population.PersonIntersectAreaFilter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Geographically cuts a MATSim population to a specified area.
 *
 * This class is a customization of the class
 * /balmermi/src/main/java/playground/balmermi/census2000v2/DilutedZurichFilter.java
 *
 * @author boescpa
 */
public class PopulationCutter {

    private final static Logger log = Logger.getLogger(PopulationCutter.class);
    private final Scenario scenario;

    public PopulationCutter(Scenario scenario) {
        this.scenario = scenario;
    }

    public static void main(String[] args) {
        // args 0: Path to config.
        // args 1: X-coord center (double)
        // args 2: Y-coord center (double)
        // args 3: Radius (int)
        // args 4: Path to population-output

        // For 30km around Zurich Center (Bellevue): X - 683518.0, Y - 246836.0, radius - 30000


        Config config = ConfigUtils.loadConfig(args[0]);
        Scenario scenario = ScenarioUtils.createScenario(config);
        new MatsimNetworkReader(scenario.getNetwork()).readFile(config.network().getInputFile());

        Coord center = new Coord(Double.parseDouble(args[1]), Double.parseDouble(args[2]));
        int radius = Integer.parseInt(args[3]);

        PopulationCutter cutter = new PopulationCutter(scenario);
        cutter.createSubscenario(config.plans().getInputFile(), args[4], center, radius);
    }

    private void createSubscenario(String populationInputFile, String populationOutputFile, Coord center, int radius) {
        PopulationImpl population = (PopulationImpl)scenario.getPopulation();
        Network network = scenario.getNetwork();

        log.info(" Area of interest (AOI): center=" + center + "; radius=" + radius);
        // Identify all links within area of interest:
        final Map<Id<Link>, Link> areaOfInterest = new HashMap<>();
        for (Link link : network.getLinks().values()) {
            final Node from = link.getFromNode();
            final Node to = link.getToNode();
            if ((CoordUtils.calcEuclideanDistance(from.getCoord(), center) <= radius)
                    || (CoordUtils.calcEuclideanDistance(to.getCoord(), center) <= radius)) {
                areaOfInterest.put(link.getId(),link);
            }
        }
        log.info(" AOI contains: " + areaOfInterest.size() + " links.");

        log.info(" Setting up population objects...");
        population.setIsStreaming(true);
        PopulationWriter pop_writer = new PopulationWriter(population, scenario.getNetwork());
        pop_writer.startStreaming(populationOutputFile);
        PopulationReader pop_reader = new MatsimPopulationReader(scenario);

        log.info(" Adding person modules...");
        PersonIntersectAreaFilter filter = new PersonIntersectAreaFilter(pop_writer, areaOfInterest, network);
        filter.setAlternativeAOI(center, radius);
        population.addAlgorithm(filter);

        log.info(" Reading, processing, writing plans...");
        pop_reader.readFile(populationInputFile);
        pop_writer.closeStreaming();
        population.printPlansCount();
        log.info(" Filtered persons: " + filter.getCount());
    }
}
