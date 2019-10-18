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

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.Random;

/**
 * @author saxer
 */
public class SelectRandomPopSample {

    //Initialize SubsamplePopulation class
    static double samplePct = 0.01; //Global sample ratio


    public static void main(String[] args) {


        //Create a Scenario
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        //Fill this Scenario with a population.
        new PopulationReader(scenario).readFile("D:\\Matsim\\Axer\\Hannover\\Base\\vw280_0.1\\vw280_0.1.output_plans.xml.gz");
        String randomOrderedPop = "Y:\\vw280_0.001.output_plans.xml.gz";
        StreamingPopulationWriter filteredPop = new StreamingPopulationWriter();
        filteredPop.startStreaming(randomOrderedPop);

        Random p = new Random();

        for (Person person : scenario.getPopulation().getPersons().values()) {
            if (p.nextDouble() < samplePct) {
                filteredPop.writePerson(person);
            }


        }

        filteredPop.closeStreaming();

    }
}






