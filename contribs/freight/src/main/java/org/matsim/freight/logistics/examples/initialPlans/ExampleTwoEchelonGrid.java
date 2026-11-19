/*
 *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       :  (C) 2022 by the members listed in the COPYING,       *
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
 * ***********************************************************************
 */

package org.matsim.freight.logistics.examples.initialPlans;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controller;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.freight.logistics.LSP;
import org.matsim.freight.logistics.LSPPlan;
import org.matsim.freight.logistics.LSPUtils;
import org.matsim.freight.logistics.examples.initialPlans.TwoEchelonGridUtils.CarrierCostSetting;
import org.matsim.freight.logistics.examples.initialPlans.TwoEchelonGridUtils.DemandSetting;

/**
 * This is an academic example for the 2-echelon problem. It uses the 9x9-grid network from the
 * matsim-examples.
 *
 * <p>The depot is located at the outer border of the network, while the jobs are located in the
 * middle area. The {@link LSP} has two different {@link LSPPlan}s: 1) direct delivery from the
 * depot 2) Using a TransshipmentHubResource: All goods were brought from the depot to the hub,
 * reloaded and then brought from the hub to the customers
 *
 * <p>The decision which of these plans is chosen should be made via the Score of the plans. We will
 * modify the costs of the vehicles and/or for using(having) the Transshipment hub. Depending on
 * this setting, the plan selection should be done accordingly.
 *
 * <p>Please note: This example is in part on existing examples, but I start from the scratch for a)
 * see, if this works and b) have a "clean" class :)
 *
 * <p>The scenario, the LSP, and the controller are created by {@link TwoEchelonGridUtils}, which is shared with the
 * other 2-echelon grid examples. This class only contains the run settings.
 *
 * @author Kai Martins-Turner (kturner)
 */
final class ExampleTwoEchelonGrid {

	// Run Settings
	static final double HUBCOSTS_FIX = 100;
	private static final DemandSetting demandSetting = DemandSetting.oneCustomer;
	private static final CarrierCostSetting costSetting = CarrierCostSetting.lowerCost4LastMile;
	private static final LSPUtils.LogicOfVrp vrpLogicDistribution = LSPUtils.LogicOfVrp.serviceBased;
	private static final double TOLL_VALUE = 1000;

	private static final Logger log = LogManager.getLogger(ExampleTwoEchelonGrid.class);

	private ExampleTwoEchelonGrid() {
	} // so it cannot be instantiated

	public static void main(String[] args) {
		log.info("Prepare Config");
		Config config = TwoEchelonGridUtils.prepareConfig(args, "output/2echelon_" + demandSetting + "_" + costSetting + "_" + HUBCOSTS_FIX + "_" + TOLL_VALUE,
			OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

		log.info("Prepare scenario");
		Scenario scenario = TwoEchelonGridUtils.prepareScenario(config, demandSetting, costSetting, HUBCOSTS_FIX, vrpLogicDistribution);

		log.info("Prepare Controller");
		Controller controller = TwoEchelonGridUtils.prepareController(scenario, TOLL_VALUE);

		log.info("Run MATSim");
		log.warn("Runs settings were: Demand: {}\n CarrierCosts: {}\n HubCosts: " + HUBCOSTS_FIX + "\n tollValue: " + TOLL_VALUE, demandSetting, costSetting);
		controller.run();

		log.info("Some results ....");
		TwoEchelonGridUtils.printResults(controller);
		log.info("Done.");
	}
}
