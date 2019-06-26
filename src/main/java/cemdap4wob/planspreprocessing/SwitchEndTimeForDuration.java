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

package cemdap4wob.planspreprocessing;/*
 * created by jbischoff, 13.06.2019
 */

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.algorithms.TripsToLegsAlgorithm;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;

public class SwitchEndTimeForDuration {
    public static void main(String[] args) {
        String inputFile = "D:/runs-svn/vw_rufbus/2018/vw219/ITERS/it.0/vw219.0.plans.xml.gz";
        String outputFile = "C://Users//Joschka//Documents//shared-svn//projects//vw_rufbus//projekt2//cemdap-vw//vw219-input/initial_plans1.0.xml.gz";
        final int[] i = {0, 0};
        TripsToLegsAlgorithm tripsToLegsAlgorithm = new TripsToLegsAlgorithm(activityType -> activityType.endsWith("interaction"), new MainModeIdentifierImpl());

        StreamingPopulationReader spr = new StreamingPopulationReader(ScenarioUtils.createScenario(ConfigUtils.createConfig()));
        StreamingPopulationWriter spw = new StreamingPopulationWriter();
        spw.startStreaming(outputFile);
        spr.addAlgorithm(new PersonAlgorithm() {
            @Override
            public void run(Person person) {
                boolean hasCountedPerson = false;
                PersonUtils.removeUnselectedPlans(person);
                tripsToLegsAlgorithm.run(person.getSelectedPlan());
                for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
                    if (pe instanceof Activity) {
                        Activity activity = (Activity) pe;

                        if (activity.getType().contains("_")) {
                            double intendedDuration = Integer.valueOf(activity.getType().split("_")[1]) * 3600;
                            if (intendedDuration <= 7200) {
                                activity.setEndTime(Time.getUndefinedTime());
                                activity.setMaximumDuration(intendedDuration);
                                i[0]++;
                                if (!hasCountedPerson) {
                                    hasCountedPerson = true;
                                    i[1]++;
                                }
                            }
                        }
                    }
                }
                spw.run(person);
            }
        });
        spr.readFile(inputFile);
        spw.closeStreaming();
        System.out.println("Persons modified " + i[1]);
        System.out.println("Activities modified " + i[0]);
    }

}