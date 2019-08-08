/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package commercialtraffic.hannover;/*
 * created by jbischoff, 20.06.2019
 */

import commercialtraffic.jobGeneration.CommercialJobUtils;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class SampleFreightPlans {

    public static void main(String[] args) {
        String inputFile = "C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/commercialtraffic/example/data_from_VW/parcel_demand_0.1.xml.gz";
        String outputFile = "C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/commercialtraffic/example/input/all_agents_0.1.xml.gz";

        List<String> operators = Arrays.asList("op1", "op2", "op3", "op4");
        Random random = MatsimRandom.getRandom();

        StreamingPopulationReader spr = new StreamingPopulationReader(ScenarioUtils.createScenario(ConfigUtils.createConfig()));
        StreamingPopulationWriter spw = new StreamingPopulationWriter();
        spw.startStreaming(outputFile);
        spr.addAlgorithm(person -> {
            PersonUtils.removeUnselectedPlans(person);
            if (CommercialJobUtils.planExpectsDeliveries(person.getSelectedPlan())) {
                person.getSelectedPlan().getPlanElements().stream()
                        .filter(Activity.class::isInstance)
                        .filter(planElement -> planElement.getAttributes().getAsMap().containsKey(CommercialJobUtils.JOB_TYPE))
                        .forEach(a -> {
                            a.getAttributes().putAttribute(CommercialJobUtils.JOB_OPERATOR, operators.get(random.nextInt(operators.size())));
                            a.getAttributes().putAttribute(CommercialJobUtils.JOB_DURATION, "180");
                            Double timeWindowStart = Double.valueOf(String.valueOf(a.getAttributes().getAttribute(CommercialJobUtils.JOB_EARLIEST_START)));
                            Double timeWindowEnd = Double.valueOf(String.valueOf(a.getAttributes().getAttribute(CommercialJobUtils.JOB_TIME_END)));
                            if (timeWindowEnd <= timeWindowStart) {
                                timeWindowEnd = timeWindowStart + 1800;
                                a.getAttributes().putAttribute(CommercialJobUtils.JOB_TIME_END, timeWindowEnd);
                            }
                        });
//                spw.run(person); //samples only agents with activities
            }
            spw.run(person);
        });

        spr.readFile(inputFile);
        spw.closeStreaming();

    }
}
