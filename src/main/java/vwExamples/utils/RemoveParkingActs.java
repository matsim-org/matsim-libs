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
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author saxer
 */
public class RemoveParkingActs {


    public static void main(String[] args) {


        //Create a Scenario
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        //Fill this Scenario with a population.
        new PopulationReader(scenario).readFile("C:\\Temp\\plans\\drt.xml.gz");
        String randomOrderedPop = "C:\\Temp\\plans\\drtSelectedWithoutParking.xml.gz";
        StreamingPopulationWriter filteredPop = new StreamingPopulationWriter();
        filteredPop.startStreaming(randomOrderedPop);

        for (Person person : scenario.getPopulation().getPersons().values()) {
            PersonUtils.removeUnselectedPlans(person);

            for (Plan plan : person.getPlans()) {
                removeCarStagingActivities(plan);
            }

            filteredPop.writePerson(person);
        }

        filteredPop.closeStreaming();

    }

    public static void removeCarStagingActivities(Plan plan) {

        List<Integer> idxList = new ArrayList<Integer>();
        Activity nexAct = null;

        List<Leg> legList = PopulationUtils.getLegs(plan);

        for (Leg leg : legList) {

            // Next activity after this leg
            nexAct = PopulationUtils.getNextActivity(plan, leg);

            // If this nexAct is car_interaction, it can be deleted
            if (nexAct.getType().equals("car interaction")) {
                Leg prevLeg = PopulationUtils.getPreviousLeg(plan, nexAct);
                Leg nextLeg = PopulationUtils.getNextLeg(plan, nexAct);
                int elementIdx = PopulationUtils.getActLegIndex(plan, (PlanElement) nexAct);
                idxList.add(elementIdx);

                // Check if there is an access or egress walk

                // PopulationUtils.removeActivity(plan, elementIdx);

                if (prevLeg.getMode().equals("access_walk")) {
                    elementIdx = PopulationUtils.getActLegIndex(plan, (PlanElement) prevLeg);
                    // PopulationUtils.removeLeg(plan, elementIdx);
                    idxList.add(elementIdx);
                }

                if (nextLeg.getMode().equals("egress_walk")) {
                    elementIdx = PopulationUtils.getActLegIndex(plan, (PlanElement) nextLeg);
                    // PopulationUtils.removeLeg(plan, elementIdx);
                    idxList.add(elementIdx);
                }

            }

            if (nexAct.getType().equals("car parkingSearch")) {
                int elementIdx = PopulationUtils.getActLegIndex(plan, (PlanElement) nexAct);
                Leg nextLeg = PopulationUtils.getNextLeg(plan, nexAct);
                // PopulationUtils.removeActivity(plan, elementIdx);
                idxList.add(elementIdx);

                if (nextLeg.getMode().equals("car")) {
                    elementIdx = PopulationUtils.getActLegIndex(plan, (PlanElement) nextLeg);
                    // PopulationUtils.removeLeg(plan, elementIdx);
                    idxList.add(elementIdx);
                }

            }

        }
        if (!idxList.isEmpty()) {

            int actIdx = 0;
            for (Iterator<PlanElement> iter = plan.getPlanElements().iterator(); iter.hasNext(); ) {

                PlanElement element = iter.next();

                if (idxList.contains(actIdx)) {
                    //System.out.println(element);
                    iter.remove();
                }
                actIdx++;
            }
            //System.out.println("finished");
        }


    }

}






