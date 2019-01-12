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

package vwExamples.peoplemoverVWExample;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.Random;

/**
 * @author saxer
 */

//This script samples a complete population to a certain subset. Variable threshold definies the relative subset share. 
public class GetSubsampleFromPopulation {

    public static void main(String[] args) {
        String inputPlansFile = "D:/Axer/MatsimDataStore/WOB_DRT_Relocating/population/run124.100.output_plans_DRT0.5.xml.gz";
        String outputPersonAttributes = "D:/Axer/MatsimDataStore/WOB_DRT_Relocating/population/run124.10.output_plans_DRT0.5.xml.gz";

        double threshold = 0.1;
        Random r = MatsimRandom.getRandom();

        //Is the number of used agents
        Integer j;
        j = 0;

//		Create new Writer
        StreamingPopulationWriter popWriter = new StreamingPopulationWriter();

//		Open stream
        popWriter.startStreaming(outputPersonAttributes);

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new PopulationReader(scenario).readFile(inputPlansFile);
        for (Person p : scenario.getPopulation().getPersons().values()) {

            //Take every 10th element
            if (r.nextDouble() < threshold) {
                j = j + 1;
                System.out.println(j);

                popWriter.writePerson(p);
//				scenario.getPopulation().getPersonAttributes().putAttribute(p.getId().toString(), "subpopulation", subpopulation);
            }
        }

        popWriter.closeStreaming();
//		new ObjectAttributesXmlWriter(scenario.getPopulation().getPersonAttributes()).writeFile(outputPersonAttributes);
    }

}
