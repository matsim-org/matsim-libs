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

/**
 *
 */
package ft.cemdap4H.planspreprocessing;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;

import java.util.HashSet;
import java.util.Set;

/**
 * @author jbischoff
 */

/**
 *
 */
public class ActivityTypeTimeConverter {

    /**
     * @param args
     */
    public static void main(String[] args) {
        final String inputPopulation = "D:/cemdap-vw/Output/mergedplans.xml.gz";
        final String outputPopulation = "D:/cemdap-vw/Output/mergedplans_dur.xml.gz";
        final String outputConfig = "D:/cemdap-vw/Output/activityConfig.xml";

        new ActivityTypeTimeConverter().run(inputPopulation, outputPopulation, outputConfig);
    }

    public void run(String inputPopulation, String outputPopulation, String outputConfig) {

        Config config = ConfigUtils.createConfig();
        ActivityParams work = new ActivityParams();
        work.setActivityType("work");
        work.setOpeningTime(6 * 3600);
        work.setClosingTime(18 * 3600);
        config.planCalcScore().addActivityParams(work);

        ActivityParams shopping = new ActivityParams();
        shopping.setActivityType("shopping");
        shopping.setOpeningTime(8 * 3600);
        shopping.setClosingTime(21 * 3600);
        config.planCalcScore().addActivityParams(shopping);


        ActivityParams leisure = new ActivityParams();
        leisure.setActivityType("leisure");
        leisure.setOpeningTime(10 * 3600);
        leisure.setClosingTime(21 * 3600);
        config.planCalcScore().addActivityParams(leisure);

        ActivityParams education = new ActivityParams();
        education.setActivityType("education");
        education.setOpeningTime(8 * 3600);
        education.setClosingTime(18 * 3600);
        config.planCalcScore().addActivityParams(education);


        ActivityParams other = new ActivityParams();
        other.setActivityType("other");
        other.setOpeningTime(6 * 3600);
        other.setClosingTime(23 * 3600);
        config.planCalcScore().addActivityParams(other);

        ActivityParams home = new ActivityParams();
        home.setActivityType("home");
        home.setTypicalDuration(24 * 3600); //for stay home plans
        config.planCalcScore().addActivityParams(home);

        Scenario scenario = ScenarioUtils.createScenario(config);
        Set<String> types = new HashSet<>();
        new PopulationReader(scenario).readFile(inputPopulation);

        for (Person p : scenario.getPopulation().getPersons().values()) {
            for (Plan plan : p.getPlans()) {
                Double firstActivityEndtime = null;
                Double lastActivityEndtime = null;
                Coord lastActivityCoord = null;
                int firstLastActivityDuration = 0;
                if (plan.getPlanElements().size() > 1) {
                    for (PlanElement pe : plan.getPlanElements()) {
                        if (pe instanceof Activity) {
                            if (firstActivityEndtime == null) {
                                firstActivityEndtime = ((Activity) pe).getEndTime();
                            }

                            if (lastActivityCoord != null) {
                                double traveltime = CoordUtils.calcEuclideanDistance(lastActivityCoord, ((Activity) pe).getCoord()) / 8.33;
                                double actStartTime = lastActivityEndtime + traveltime;
                                if (!Time.isUndefinedTime(((Activity) pe).getEndTime())) {
                                    int duration = (int) Math.round((((Activity) pe).getEndTime() - actStartTime) / 3600);
                                    if (duration <= 0) duration = 1;
                                    String type = ((Activity) pe).getType() + "_" + duration;
                                    ((Activity) pe).setType(type);
                                    types.add(type);
                                } else {
                                    int duration = (int) Math.round(((24 * 3600 - actStartTime) + firstActivityEndtime) / 3600);
                                    firstLastActivityDuration = duration;
                                    if (duration <= 0) duration = 1;
                                    String type = ((Activity) pe).getType() + "_" + duration;
                                    ((Activity) pe).setType(type);
                                    types.add(type);

                                }

                                lastActivityCoord = ((Activity) pe).getCoord();
                                lastActivityEndtime = ((Activity) pe).getEndTime();
                            } else {
                                lastActivityCoord = ((Activity) pe).getCoord();
                                lastActivityEndtime = ((Activity) pe).getEndTime();
                            }
                        }
                    }
                    Activity act0 = (Activity) (plan.getPlanElements().get(0));
                    act0.setType(act0.getType() + "_" + firstLastActivityDuration);

                }
            }
        }


        new PopulationWriter(scenario.getPopulation()).write(outputPopulation);
        for (String type : types) {
            String baseType = type.split("_")[0];
            double duration = Integer.parseInt(type.split("_")[1]) * 3600;
            ActivityParams t = new ActivityParams();
            t.setActivityType(type);
            t.setTypicalDuration(duration);
            Logger.getLogger(getClass()).info(baseType);
            t.setOpeningTime(config.planCalcScore().getActivityParams(baseType).getOpeningTime());
            t.setClosingTime(config.planCalcScore().getActivityParams(baseType).getClosingTime());
            config.planCalcScore().addActivityParams(t);
        }
        new ConfigWriter(config).write(outputConfig);
    }


}
