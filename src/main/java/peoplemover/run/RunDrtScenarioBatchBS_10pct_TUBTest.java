
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

package peoplemover.run;

import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingParams;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

/** * @author axer */

public class RunDrtScenarioBatchBS_10pct_TUBTest {

	public static void main(String[] args) {

		// Enable or Disable rebalancing

		String inputPath = "C:\\Users\\Joschka\\Documents\\shared-svn\\projects\\vw_rufbus\\projekt2\\drt_test_Scenarios\\BS_DRT\\input\\";
		// For each demandScenario we are generating a new config file
		// Some config parameters will be taken from the provided config file
		// Other config parameters will be generated or modified dynamically within this
		// loop
		// Define the path to the config file and enable / disable otfvis
		final Config config = ConfigUtils.loadConfig(inputPath + "config.xml", new MultiModeDrtConfigGroup(),
				new DvrpConfigGroup(), new OTFVisConfigGroup());

		// OTFVis is an open source, OpenGL-based visualizer for looking at MATSim
		// scenarios and output.
		// With this switch we could disable/enable the visualization.
		boolean otfvis = false;

		// Overwrite existing configuration parameters
		config.plans().setInputFile("population/vw219_it_1_sampleRate0.1replaceRate_bs_drt.xml.gz");
		config.controler().setLastIteration(1); // Number of simulation iterations
		config.controler().setWriteEventsInterval(1); // Write Events file every x-Iterations
		config.controler().setWritePlansInterval(1); // Write Plan file every x-Iterations

		config.network().setInputFile("network/modifiedNetwork.xml.gz");

		// This part allows to change dynamically DRT config parameters
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

		String runId = "0.1_drt_100veh";
		config.controler().setRunId(runId);
		config.qsim().setFlowCapFactor(0.12);
		config.qsim().setStorageCapFactor(0.24);

		config.controler().setOutputDirectory(inputPath+"../output/" + runId); 

		// For each demand scenario we are using a predefined drt vehicle fleet size
		drt.setVehiclesFile("fleets/fleet_100.xml.gz");

		// Define the MATSim Controler
		// Based on the prepared configuration this part creates a controller that runs
		Controler controler = DrtControlerCreator.createControlerWithSingleModeDrt(config, otfvis);

		// Every x-seconds the simulation calls a re-balancing process.
		// Re-balancing has the task to move vehicles into cells or zones that fits
		// typically with the demand situation
		// The technically used re-balancing strategy is then installed/binded within
		// the initialized controler
		System.out.println("Rebalancing Online");
		MinCostFlowRebalancingParams rebalancingParams = drt.getMinCostFlowRebalancing().get();
		rebalancingParams.setInterval(600);
		rebalancingParams.setCellSize(2000);


		// Change the routing module in this way, that agents are forced to go to their
		// closest bus stop.
		// If we would remove this part, agents are searching a bus stop which lies in
		// the direction of their destination but is maybe far away.
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				// addRoutingModuleBinding(DvrpConfigGroup.get(config).getMode())
				// .to(ClosestStopBasedDrtRoutingModule.class);
				// Link travel times are iterativly updated between iteration
				// tt[i] = alpha * experiencedTT + (1 - alpha) * oldEstimatedTT;
				// Remark: Small alpha leads to more smoothing and longer lags in reaction.
				// Default alpha is 0.05. Which means i.e. 0.3 is not smooth in comparison to
				// 0.05
				DvrpConfigGroup.get(config).setTravelTimeEstimationAlpha(0.15);
			}
		});

		// We finally run the controller to start MATSim
		controler.run();

		// }
	}
}
