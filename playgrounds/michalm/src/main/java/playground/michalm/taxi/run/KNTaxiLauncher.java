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
import org.matsim.contrib.dvrp.extensions.taxi.TaxiUtils;
import org.matsim.contrib.dvrp.run.VrpPopulationUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import playground.michalm.taxi.optimizer.AbstractTaxiOptimizerParams;
import playground.michalm.taxi.optimizer.AbstractTaxiOptimizerParams.TravelTimeSource;
import playground.michalm.taxi.optimizer.rules.RuleBasedTaxiOptimizer.Goal;
import playground.michalm.taxi.optimizer.rules.RuleBasedTaxiOptimizerParams;
import playground.michalm.taxi.scheduler.TaxiSchedulerParams;


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
            VrpPopulationUtils.removePersonsNotUsingMode(TaxiUtils.TAXI_MODE, launcher.scenario);

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


    private static Configuration createConfig()
    {
        String inputDir = "../../../shared-svn/projects/maciejewski/Mielec/2014_02_base_scenario/";
        Map<String, Object> map = new HashMap<>();
        map.put(TaxiLauncherParams.NET_FILE, inputDir + "network.xml");

        //demand: 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0
        map.put(TaxiLauncherParams.PLANS_FILE, inputDir + "plans_taxi/plans_taxi_4.0.xml.gz");
        //supply: 25, 50
        map.put(TaxiLauncherParams.TAXIS_FILE, inputDir + "taxis-25.xml");

        map.put(TaxiLauncherParams.ONLINE_VEHICLE_TRACKER, Boolean.FALSE);
        map.put(TaxiLauncherParams.OTF_VIS, "true");

        String sPrefix = TaxiConfigUtils.SCHEDULER + TaxiConfigUtils.DELIMITER;
        map.put(sPrefix + TaxiSchedulerParams.DESTINATION_KNOWN, Boolean.FALSE);
        map.put(sPrefix + TaxiSchedulerParams.VEHICLE_DIVERSION, Boolean.FALSE);
        map.put(sPrefix + TaxiSchedulerParams.PICKUP_DURATION, 1.);
        map.put(sPrefix + TaxiSchedulerParams.DROPOFF_DURATION, 1.);

        String oPrefix = TaxiConfigUtils.OPTIMIZER + TaxiConfigUtils.DELIMITER;
        map.put(oPrefix + AbstractTaxiOptimizerParams.PARAMS_CLASS,
                RuleBasedTaxiOptimizerParams.class.getName());
        map.put(oPrefix + AbstractTaxiOptimizerParams.ID, "KN");
        map.put(oPrefix + AbstractTaxiOptimizerParams.TRAVEL_TIME_SOURCE,
                TravelTimeSource.FREE_FLOW_SPEED.name());
        map.put(oPrefix + RuleBasedTaxiOptimizerParams.GOAL, Goal.DEMAND_SUPPLY_EQUIL.name());
        map.put(oPrefix + RuleBasedTaxiOptimizerParams.NEAREST_REQUESTS_LIMIT, 99999);
        map.put(oPrefix + RuleBasedTaxiOptimizerParams.NEAREST_VEHICLES_LIMIT, 99999);
        map.put(oPrefix + RuleBasedTaxiOptimizerParams.CELL_SIZE, 1000);

        return new MapConfiguration(map);
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
