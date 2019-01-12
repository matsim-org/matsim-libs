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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

/**
 * @author saxer
 */
public class StorePlansInRandomOrder {

    //Initialize SubsamplePopulation class
    static double samplePct = 0.05; //Global sample ratio
    static Set<Id<Person>> seenAgents = new HashSet<>();


    public static void main(String[] args) {


        //Create a Scenario
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        //Fill this Scenario with a population.
        new PopulationReader(scenario).readFile("E:\\Axer\\MatsimDataStore2\\BSWOB_Clean\\input\\vw219_10pct.xml.gz");
        String randomOrderedPopDest = "E:\\Axer\\MatsimDataStore2\\BSWOB_Clean\\input\\vw219_10pct_RandOrder.xml.gz";
        StreamingPopulationWriter randomOrderedPop = new StreamingPopulationWriter();
        randomOrderedPop.startStreaming(randomOrderedPopDest);

        int totalPopulation = scenario.getPopulation().getPersons().size();
        int insertedPopulation = 0;

        //System.out.println(insertedPopulation + " out of " +totalPopulation);

        Population pop = scenario.getPopulation();
        int loopValue = 0;

        Random p = new Random();

        while (totalPopulation > insertedPopulation) {

            for (Iterator<? extends Person> iterator = pop.getPersons().values().iterator(); iterator.hasNext(); ) {


                Double actRand = p.nextDouble();
                Person pers = iterator.next();

                if (actRand < samplePct) {
                    if (seenAgents.contains(pers.getId())) System.out.println("WARNING CLONED AN AGEND");

                    //System.out.println(actRand);
                    randomOrderedPop.writePerson(pers);
                    seenAgents.add(pers.getId());


//					scenario.getPopulation().removePerson(iterator.next().getId());
                    iterator.remove();
                    pop.removePerson(pers.getId());


                    insertedPopulation += 1;
//					totalPopulation = pop.getPersons().size();
                    //System.out.println(insertedPopulation + " out of " +totalPopulation);
                }

            }
            //System.out.println("Loop "+loopValue);
            loopValue += 1;

        }

        randomOrderedPop.closeStreaming();
        System.out.println("Closed Stream");

    }
}






