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

/**
 *
 */
package vwExamples.utils;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.router.TransitActsRemover;

import java.util.Random;

/**
 * @author jbischoff
 */

/**
 *
 */
public class SimplePlansUpsampler {

    static final int clonePlans = 9;
    static final int radius = 500;
    static final int timeVariation = 900;
    static final String inputPopulation = "D:\\Axer\\MatsimDataStore\\Berlin_DRT\\input\\population\\be_251.output_plans_selected.xml.gz";
    static final String outputPopulation = "D:\\Axer\\MatsimDataStore\\Berlin_DRT\\input\\population\\be_251.output_plans_selected_1.0upsample.xml.gz";


    public static void main(String[] args) {
        Random r = MatsimRandom.getRandom();
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Population pop2 = PopulationUtils.createPopulation(ConfigUtils.createConfig());

        StreamingPopulationReader spr = new StreamingPopulationReader(scenario);
        StreamingPopulationWriter spw = new StreamingPopulationWriter();
        spw.startStreaming(outputPopulation);

        spr.addAlgorithm(new PersonAlgorithm() {

            @Override
            public void run(Person person) {
                Plan plan = person.getSelectedPlan();
                for (PlanElement pe : plan.getPlanElements()) {
                    if (pe instanceof Leg) {
                        ((Leg) pe).setRoute(null);
                        ((Leg) pe).setDepartureTime(Time.UNDEFINED_TIME);

                    } else if (pe instanceof Activity) {
                        Activity act = (Activity) pe;
                        act.setLinkId(null);
                    }
                }
                PersonUtils.removeUnselectedPlans(person);

                spw.run(person);
                new TransitActsRemover().run(plan);
                for (int i = 1; i <= clonePlans; i++) {
                    Person cloneP = pop2.getFactory().createPerson(Id.createPersonId(person.getId().toString() + "_" + i));
                    Plan clonePlan = pop2.getFactory().createPlan();
                    PopulationUtils.copyFromTo(plan, clonePlan);

                    cloneP.addPlan(clonePlan);
                    for (PlanElement pe : clonePlan.getPlanElements()) {
                        if (pe instanceof Activity) {
                            Activity act = (Activity) pe;
                            act.setLinkId(null);
                            Coord newCoord = new Coord(act.getCoord().getX() - radius + r.nextInt(2 * radius), act.getCoord().getY() - radius + r.nextInt(2 * radius));
                            act.setCoord(newCoord);
                            act.setEndTime(act.getEndTime() - timeVariation + r.nextInt(2 * timeVariation));
                        }
                    }


                    spw.run(cloneP);
                }


            }
        });
        spr.readFile(inputPopulation);
        spw.closeStreaming();

    }
}
