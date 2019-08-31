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

package peoplemover.stop;

import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

public class RunBSWOBDrtScenarioWithVariableStopTimes {

    private static String inputPath = "D:\\BS_DRT\\input\\";

    public static void main(String[] args) {
        Config config = ConfigUtils.loadConfig(inputPath + "stoptimes-config.xml", new DvrpConfigGroup(),
                new OTFVisConfigGroup(), new MultiModeDrtConfigGroup());
        config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
        config.plans().setInputFile("population/vw219_it_1_sampleRate0.1replaceRate_bs_drt.xml.gz");
        config.controler().setLastIteration(1); // Number of simulation iterations
        config.controler().setWriteEventsInterval(1); // Write Events file every x-Iterations
        config.controler().setWritePlansInterval(1); // Write Plan file every x-Iterations
        config.network().setInputFile("network/modifiedNetwork.xml.gz");

        DrtConfigGroup drt = (DrtConfigGroup) config.getModules().get(DrtConfigGroup.GROUP_NAME);

        // Use custom stop duration
        drt.setOperationalScheme(DrtConfigGroup.OperationalScheme.stopbased);
        drt.setMaxTravelTimeBeta(500);
        drt.setMaxTravelTimeAlpha(1.3);
        drt.setMaxWaitTime(500);
        drt.setStopDuration(15);
        drt.setTransitStopFile("virtualstops/stopsGrid_300m.xml");
        drt.setMaxWalkDistance(800.0);
        drt.setPrintDetailedWarnings(false);
        drt.setVehiclesFile("edrt/e-drt_bs_100.xml");

        String runId = "drt-test-very-short-stoptimes";
//		String runId = "drt-test-very-long-stoptimes";
        config.controler().setRunId(runId);
        config.qsim().setFlowCapFactor(0.12);
        config.qsim().setStorageCapFactor(0.24);

        config.controler().setOutputDirectory(inputPath + "../output/" + runId);

        Controler controler = DrtControlerCreator.createControlerWithSingleModeDrt(config, false);

        //Define the stop times below
        double pickupTimePerPassenger = 1.0;
        double dropoffTimePerPassenger = 1.0;
        double fixedStopTime = 5.0;

        //these two lines enable the variable stop time
        BusStopDurationCalculator busStopDurationCalculator = new LinearBusStopDurationCalculator(
                pickupTimePerPassenger, dropoffTimePerPassenger, fixedStopTime);
        controler.addOverridingQSimModule(
                new VariableDurationBusStopQSimModule(drt.getMode(), busStopDurationCalculator));

        controler.run();
    }


}
