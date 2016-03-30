/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.michalm.taxi.run;

import java.util.*;

import org.apache.commons.configuration.*;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.taxi.optimizer.AbstractTaxiOptimizerParams;
import org.matsim.contrib.taxi.optimizer.rules.RuleBasedTaxiOptimizer.Goal;
import org.matsim.contrib.taxi.optimizer.rules.RuleBasedTaxiOptimizerParams;
import org.matsim.contrib.taxi.run.TaxiModule;
import org.matsim.contrib.taxi.scheduler.TaxiSchedulerParams;
import org.matsim.contrib.util.PopulationUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;


class KNTaxiLauncher
{
    /**
     * @param config configuration (e.g. read from a param file)
     * @param removeNonPassengers if {@code true}, only taxi traffic is simulated
     * @param endActivitiesAtTimeZero if {@code true}, everybody calls taxi at time 0
     * @param useOTFVis TODO
     */
    public static void run(Configuration config, boolean removeNonPassengers,
            boolean endActivitiesAtTimeZero, boolean useOTFVis)
    {
        SingleRunTaxiLauncher launcher = new SingleRunTaxiLauncher(config);

        if (removeNonPassengers) {
            PopulationUtils.removePersonsNotUsingMode(TaxiModule.TAXI_MODE, launcher.scenario);

            if (endActivitiesAtTimeZero) {
                setEndTimeForFirstActivities(launcher.scenario, 0);
            }
        }

        if (useOTFVis) {
            OTFVisConfigGroup otfConfig = new OTFVisConfigGroup();
            launcher.scenario.getConfig().addModule(otfConfig);
            otfConfig.setLinkWidth(2);
        }

        launcher.initTravelTimeAndDisutility();
        launcher.simulateIteration("");
    }


    private static void setEndTimeForFirstActivities(Scenario scenario, double time)
    {
        Map<Id<Person>, ? extends Person> persons = scenario.getPopulation().getPersons();
        for (Person p : persons.values()) {
            Activity activity = (Activity)p.getSelectedPlan().getPlanElements().get(0);
            activity.setEndTime(time);
        }
    }


    public static void main(String... args)
    {
        run(createConfig(), true, false, true);
    }
}
