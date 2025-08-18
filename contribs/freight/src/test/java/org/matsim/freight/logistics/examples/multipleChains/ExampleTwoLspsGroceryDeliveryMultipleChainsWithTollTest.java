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
    @RegisterExtension
    public final MatsimTestUtils utils = new MatsimTestUtils();


/**
 * This Test should ensure that the results are stable by checking for LSP File and events File to be equal to a previous run.
 * It is **not** meat to get never chanced. In contrast, it will prevent me from unintended changes.
 * I assume that with ongoing work, I will adapt the test input regularly.
 */
    @Test
    public void testOutputIsEqual_OneChainDirect() {

        try {
				String[] argsToSet = {
					"--outputDirectory=" + utils.getOutputDirectory(),
					"--matsimIterations=1",
					"--jspritIterationsMain=1",
					"--jspritIterationsDirect=1",
					"--jspritIterationsDistribution=1",
					"--tollValue=1000.0",
					"--tolledVehicleTypes=heavy40t,heavy40t_electro",
					"--HubCostsFix=100.0",
					"--typeOfLsps="+ONE_CHAIN_DIRECT,
					"--EdekaCarrierId=edeka_SUPERMARKT_TROCKEN",
					"--HubLinkIdEdeka=91085",
					"--KauflandCarrierId=kaufland_VERBRAUCHERMARKT_TROCKEN",
					"--HubLinkIdKaufland=91085"
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
	 * It is **not** meat to get never chanced. In contrast, it will prevent me from unintended changes.
	 * I assume that with ongoing work, I will adapt the test input regularly.
	 */
	@Test
	public void testOutputIsEqual_oneChain2echelon() {

		try {
			String[] argsToSet = {
				"--outputDirectory=" + utils.getOutputDirectory(),
				"--matsimIterations=1",
				"--jspritIterationsMain=1",
				"--jspritIterationsDirect=1",
				"--jspritIterationsDistribution=1",
				"--tollValue=1000.0",
				"--tolledVehicleTypes=heavy40t,heavy40t_electro",
				"--HubCostsFix=100.0",
				"--typeOfLsps="+ ONE_CHAIN_TWO_ECHELON,
				"--EdekaCarrierId=edeka_SUPERMARKT_TROCKEN",
				"--HubLinkIdEdeka=91085",
				"--KauflandCarrierId=kaufland_VERBRAUCHERMARKT_TROCKEN",
				"--HubLinkIdKaufland=91085"
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
	 * It is **not** meat to get never chanced. In contrast, it will prevent me from unintended changes.
	 * I assume that with ongoing work, I will adapt the test input regularly.
	 */
	@Test
	public void testOutputIsEqual_twoChains() {

		try {
			String[] argsToSet = {
				"--outputDirectory=" + utils.getOutputDirectory(),
				"--matsimIterations=1",
				"--jspritIterationsMain=1",
				"--jspritIterationsDirect=1",
				"--jspritIterationsDistribution=1",
				"--tollValue=1000.0",
				"--tolledVehicleTypes=heavy40t,heavy40t_electro",
				"--HubCostsFix=100.0",
				"--typeOfLsps="+ TWO_CHAINS_DIRECT_AND_TWO_ECHELON,
				"--EdekaCarrierId=edeka_SUPERMARKT_TROCKEN",
				"--HubLinkIdEdeka=91085",
				"--KauflandCarrierId=kaufland_VERBRAUCHERMARKT_TROCKEN",
				"--HubLinkIdKaufland=91085"
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
