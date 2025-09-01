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

package org.matsim.freight.logistics.examples.multipleChains;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.testcases.MatsimTestUtils;

import static org.junit.jupiter.api.Assertions.fail;
import static org.matsim.freight.logistics.examples.multipleChains.ExampleTwoLspsGroceryDeliveryMultipleChainsWithToll.TypeOfLsps.*;

public class ExampleTwoLspsGroceryDeliveryMultipleChainsWithTollTest {
    private static final Logger log = LogManager.getLogger(ExampleTwoLspsGroceryDeliveryMultipleChainsWithTollTest.class);
	private static final String HEAVY_40T = "heavy40t";
	private static final String HEAVY_40T_ELECTRO = "heavy40t_electro";
	private static final String LIGHT_8T_ELECTRO = "light8t_electro";

	@RegisterExtension
    public final MatsimTestUtils utils = new MatsimTestUtils();


/**
 * This Test should ensure that the results are stable by checking for LSP File and events File to be equal to a previous run.
 * It is **not** meant to get never chanced. In contrast, it will prevent me from unintended changes.
 * I assume that with ongoing work, I will adapt the test input regularly.
 */
    @Test
    public void testOutputIsEqual_onePlanOnlyDirectChain() {

        try {
				String[] argsToSet = {
					"--outputDirectory=" + utils.getOutputDirectory(),
					"--matsimIterations=1",
					"--jspritIterationsMain=1",
					"--jspritIterationsDirect=1",
					"--jspritIterationsDistribution=1",
					"--tollValue=1000.0",
					"--tolledVehicleTypes=" + HEAVY_40T + "," + HEAVY_40T_ELECTRO,
					"--HubCostsFix=100.0",
					"--typeOfLsps="+ ONE_PLAN_ONLY_DIRECT_CHAIN,
					"--lsp1Name=Edeka",
					"--lsp1CarrierId=edeka_SUPERMARKT_TROCKEN",
					"--lsp1HubLinkId=91085",
					"--lsp1vehTypesDirect=" + HEAVY_40T,
//					"--lsp1vehTypesMain=heavy40t",
//					"--lsp1vehTypesDelivery=heavy40t",
					"--lsp2Name=Kaufland",
					"--lsp2CarrierId=kaufland_VERBRAUCHERMARKT_TROCKEN",
					"--lsp2HubLinkId=91085",
					"--lsp2vehTypesDirect=" + HEAVY_40T,
//					"--lsp2vehTypesMain=heavy40t",
//					"--lsp2vehTypesDelivery=light8t_electro"
				};
				ExampleTwoLspsGroceryDeliveryMultipleChainsWithToll.main(argsToSet);

        } catch (Exception ee) {
            log.fatal(ee);
            fail();
        }

        //Compare LSP files
        MatsimTestUtils.assertEqualFilesLineByLine(utils.getInputDirectory() + "output_lsps.xml.gz", utils.getOutputDirectory() + "output_lsps.xml.gz" );

        //Compare events files
        MatsimTestUtils.assertEqualEventsFiles(utils.getInputDirectory() + "output_events.xml.gz", utils.getOutputDirectory() + "output_events.xml.gz" );
    }

	/**
	 * This Test should ensure that the results are stable by checking for LSP File and events File to be equal to a previous run.
	 * It is **not** meant to get never chanced. In contrast, it will prevent me from unintended changes.
	 * I assume that with ongoing work, I will adapt the test input regularly.
	 */
	@Test
	public void testOutputIsEqual_onePlanOnlyDirectChain_Edeka() {

		try {
			String[] argsToSet = {
				"--outputDirectory=" + utils.getOutputDirectory(),
				"--matsimIterations=1",
				"--jspritIterationsMain=1",
				"--jspritIterationsDirect=1",
				"--jspritIterationsDistribution=1",
				"--tollValue=1000.0",
				"--tolledVehicleTypes=" + HEAVY_40T + "," + HEAVY_40T_ELECTRO,
				"--HubCostsFix=100.0",
				"--typeOfLsps="+ ONE_PLAN_ONLY_DIRECT_CHAIN,
				"--lsp1Name=Edeka",
				"--lsp1CarrierId=edeka_SUPERMARKT_TROCKEN",
				"--lsp1HubLinkId=91085",
				"--lsp1vehTypesDirect=" + HEAVY_40T,
			};
			ExampleTwoLspsGroceryDeliveryMultipleChainsWithToll.main(argsToSet);

		} catch (Exception ee) {
			log.fatal(ee);
			fail();
		}

		//Compare LSP files
		MatsimTestUtils.assertEqualFilesLineByLine(utils.getInputDirectory() + "output_lsps.xml.gz", utils.getOutputDirectory() + "output_lsps.xml.gz" );

		//Compare events files
		MatsimTestUtils.assertEqualEventsFiles(utils.getInputDirectory() + "output_events.xml.gz", utils.getOutputDirectory() + "output_events.xml.gz" );
	}

	/**
	 * This Test should ensure that the results are stable by checking for LSP File and events File to be equal to a previous run.
	 * It is **not** meant to get never chanced. In contrast, it will prevent me from unintended changes.
	 * I assume that with ongoing work, I will adapt the test input regularly.
	 */
	@Test
	public void testOutputIsEqual_onePlanOnly2echelonChain() {

		try {
			String[] argsToSet = {
				"--outputDirectory=" + utils.getOutputDirectory(),
				"--matsimIterations=1",
				"--jspritIterationsMain=1",
				"--jspritIterationsDirect=1",
				"--jspritIterationsDistribution=1",
				"--tollValue=1000.0",
				"--tolledVehicleTypes=" + HEAVY_40T + "," + HEAVY_40T_ELECTRO,
				"--HubCostsFix=100.0",
				"--typeOfLsps="+ ONE_PLAN_ONLY_TWO_ECHELON_CHAIN,
				"--lsp1Name=Edeka",
				"--lsp1CarrierId=edeka_SUPERMARKT_TROCKEN",
				"--lsp1HubLinkId=91085",
//				"--lsp1vehTypesDirect=heavy40t",
				"--lsp1vehTypesMain=" + HEAVY_40T,
				"--lsp1vehTypesDelivery=" + HEAVY_40T,
				"--lsp2Name=Kaufland",
				"--lsp2CarrierId=kaufland_VERBRAUCHERMARKT_TROCKEN",
				"--lsp2HubLinkId=91085",
//				"--lsp2vehTypesDirect=heavy40t",
				"--lsp2vehTypesMain=" + HEAVY_40T,
				"--lsp2vehTypesDelivery=" + LIGHT_8T_ELECTRO
			};
			ExampleTwoLspsGroceryDeliveryMultipleChainsWithToll.main(argsToSet);

		} catch (Exception ee) {
			log.fatal(ee);
			fail();
		}

		//Compare LSP files
		MatsimTestUtils.assertEqualFilesLineByLine(utils.getInputDirectory() + "output_lsps.xml.gz", utils.getOutputDirectory() + "output_lsps.xml.gz" );

		//Compare events files
		MatsimTestUtils.assertEqualEventsFiles(utils.getInputDirectory() + "output_events.xml.gz", utils.getOutputDirectory() + "output_events.xml.gz" );
	}

	/**
	 * This Test should ensure that the results are stable by checking for LSP File and events File to be equal to a previous run.
	 * It is **not** meant to get never chanced. In contrast, it will prevent me from unintended changes.
	 * I assume that with ongoing work, I will adapt the test input regularly.
	 */
	@Test
	public void testOutputIsEqual_onePlanBothChains() {

		try {
			String[] argsToSet = {
				"--outputDirectory=" + utils.getOutputDirectory(),
				"--matsimIterations=1",
				"--jspritIterationsMain=1",
				"--jspritIterationsDirect=1",
				"--jspritIterationsDistribution=1",
				"--tollValue=1000.0",
				"--tolledVehicleTypes=" + HEAVY_40T + "," + HEAVY_40T_ELECTRO,
				"--HubCostsFix=100.0",
				"--typeOfLsps="+ ONE_PLAN_BOTH_CHAINS,
				"--lsp1Name=Edeka",
				"--lsp1CarrierId=edeka_SUPERMARKT_TROCKEN",
				"--lsp1HubLinkId=91085",
				"--lsp1vehTypesDirect=" + HEAVY_40T,
				"--lsp1vehTypesMain=" + HEAVY_40T,
				"--lsp1vehTypesDelivery=" + HEAVY_40T,
				"--lsp2Name=Kaufland",
				"--lsp2CarrierId=kaufland_VERBRAUCHERMARKT_TROCKEN",
				"--lsp2HubLinkId=91085",
				"--lsp2vehTypesDirect=" + HEAVY_40T_ELECTRO, //Note this was electro in my reference case, when creating the IT.
				"--lsp2vehTypesMain=" + HEAVY_40T,
				"--lsp2vehTypesDelivery=" + LIGHT_8T_ELECTRO
			};
			ExampleTwoLspsGroceryDeliveryMultipleChainsWithToll.main(argsToSet);

		} catch (Exception ee) {
			log.fatal(ee);
			fail();
		}

		//Compare LSP files
		MatsimTestUtils.assertEqualFilesLineByLine(utils.getInputDirectory() + "output_lsps.xml.gz", utils.getOutputDirectory() + "output_lsps.xml.gz" );

		//Compare events files
		MatsimTestUtils.assertEqualEventsFiles(utils.getInputDirectory() + "output_events.xml.gz", utils.getOutputDirectory() + "output_events.xml.gz" );
	}


}
