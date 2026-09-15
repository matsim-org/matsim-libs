/*
 *   *********************************************************************** *
 *   project: org.matsim.*													 *
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C) 2025 by the members listed in the COPYING, 		 *
 *                          LICENSE and WARRANTY file.  					 *
 *   email           : info at matsim dot org   							 *
 *                                                                         	 *
 *   *********************************************************************** *
 *                                                                        	 *
 *     This program is free software; you can redistribute it and/or modify  *
 *      it under the terms of the GNU General Public License as published by *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.								     *
 *     See also COPYING, LICENSE and WARRANTY file						 	 *
 *                                                                           *
 *   *********************************************************************** *
 */

package org.matsim.freight.logistics.examples.multipleChains;

import static org.matsim.freight.logistics.examples.multipleChains.ExampleTwoLspsGroceryDeliveryMultipleChainsWithToll.TypeOfLsps.*;

/**
 * This class runs the example of two LSPs (Edeka and Kaufland) with grocery delivery,
 * where each LSP has multiple transport chains and tolls are applied to certain vehicle types.
 * It sets the parameters for the simulation, including the number of iterations for the jsprit solver
 * and the toll value for specific vehicle types.
 * The hub costs and hub link IDs for both LSPs are also specified.
 */
public class RunExampleTwoLspsGroceryDeliveryMultipleChainsWithToll {

	private static final String HEAVY_40T = "heavy40t";
	private static final String HEAVY_40T_ELECTRO = "heavy40t_electro";
	private static final String LIGHT_8T_ELECTRO = "light8t_electro";
	private static final String LIGHT_8T = "light8t";

	public static void main(String[] args) throws Exception {
		String[] argsToSet = {
			"--outputDirectory=output/groceryDelivery_kmt_banDieselVehiclesFromScript3",
			"--matsimIterations=5",
			"--jspritIterationsMain=1",
			"--jspritIterationsDirect=1",
			"--jspritIterationsDistribution=1",
			"--tollValue=1000.0",
			"--tolledVehicleTypes=heavy40t,heavy40t_electro",
			"--HubCostsFix=100.0",
			"--typeOfLsps="+ONE_PLAN_BOTH_CHAINS,
			"--lsp1Name=Kaufland",
			"--lsp1CarrierId=kaufland_VERBRAUCHERMARKT_TROCKEN",
			"--lsp1HubLinkId=91085",
			"--lsp1vehTypesDirect=" + HEAVY_40T,
			"--lsp1vehTypesMain=" + HEAVY_40T,
			"--lsp1vehTypesDelivery=" + LIGHT_8T
		};
		ExampleTwoLspsGroceryDeliveryMultipleChainsWithToll.main(argsToSet);
	}
}
