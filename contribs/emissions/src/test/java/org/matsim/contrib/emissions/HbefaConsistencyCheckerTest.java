package org.matsim.contrib.emissions;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.contrib.emissions.utils.TestUtils;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;


class HbefaConsistencyCheckerTest {

	@RegisterExtension
	MatsimTestUtils utils = new MatsimTestUtils();

	// TODO Replace by small local test files
	private static String PATH = "https://svn.vsp.tu-berlin.de/repos/public-svn/3507bb3997e5657ab9da76dbedbb13c9b5991d3e/0e73947443d68f95202b71a156b337f7f71604ae/";

	@Test
	public void validHbefaTableTest(){
		// Set up the emission module
		EmissionsConfigGroup ecg = new EmissionsConfigGroup();
		ecg.setDetailedVsAverageLookupBehavior(EmissionsConfigGroup.DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageThenAverageTable);
		ecg.setWritingEmissionsEvents(false);
		ecg.setAverageColdEmissionFactorsFile(PATH + "22823adc0ee6a0e231f35ae897f7b224a86f3a7a.enc");
		ecg.setAverageWarmEmissionFactorsFile(PATH + "7eff8f308633df1b8ac4d06d05180dd0c5fdf577.enc");
		ecg.setDetailedColdEmissionFactorsFile(PATH + "b63f949211b7c93776cdce8a7600eff4e36460c8.enc");
		ecg.setDetailedWarmEmissionFactorsFile(PATH + "f5b276f41a0531ed740a81f4615ec00f4ff7a28d.enc");

		// Create the scenario. The test should run through without problems. If this test fails the Checker gives false alarm and should be investigated.
		Config config = utils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml"));
		config.addModule(ecg);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setLastIteration(0);

		MutableScenario scenario = ScenarioUtils.createMutableScenario(config);
		Network network = TestUtils.createRandomNetwork(10, 10, 10);
		scenario.setNetwork(network);
		new VspHbefaRoadTypeMapping().addHbefaMappings(network);

		new EmissionModule(scenario, null);
	}

	@Test
	public void corruptHbefaTableTest(){
		// Tests a table, where a corrupt table with switched emConcept and technology columns is used
		// Main purpose of this test is to make sure, that the checker will warn about the corrupted hbefa-tables with switched columns
		// Set up the emission module
		EmissionsConfigGroup ecg = new EmissionsConfigGroup();
		ecg.setDetailedVsAverageLookupBehavior(EmissionsConfigGroup.DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageThenAverageTable);
		ecg.setWritingEmissionsEvents(false);
		ecg.setAverageColdEmissionFactorsFile("/Users/aleksander/Documents/VSP/PHEMTest/hbefa/old/EFA_ColdStart_Vehcat_2020_Average.csv");
		ecg.setAverageWarmEmissionFactorsFile("/Users/aleksander/Documents/VSP/PHEMTest/hbefa/old/EFA_HOT_Vehcat_2020_Average.csv");
		ecg.setDetailedColdEmissionFactorsFile("/Users/aleksander/Documents/VSP/PHEMTest/hbefa/old/EFA_ColdStart_Concept_2020_detailed_perTechAverage_Bln_carOnly.csv");
		ecg.setDetailedWarmEmissionFactorsFile("/Users/aleksander/Documents/VSP/PHEMTest/hbefa/old/EFA_HOT_Concept_2020_detailed_perTechAverage_Bln_carOnly.csv");

		// Create the scenario. The test should run through without problems. If this test fails the Checker gives false alarm and should be investigated.
		Config config = utils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml"));
		config.addModule(ecg);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setLastIteration(0);

		MutableScenario scenario = ScenarioUtils.createMutableScenario(config);
		Network network = TestUtils.createRandomNetwork(10, 10, 10);
		scenario.setNetwork(network);
		new VspHbefaRoadTypeMapping().addHbefaMappings(network);

		try {
			new EmissionModule(scenario, null);

			//If this part of code is reached, the checker did not fail even though we gave it a corrupted file, fail the test
			Assertions.fail("HbefaConsistencyChecker did not throw an exception for a corrupted table. The checker is not working!");
		} catch (HbefaConsistencyChecker.CorruptedHbefaTableException corrupted){
			// The test works as intended: it throws a corruptedHbefaTable Exception
		} catch (Exception other){
			// Some other error occurred which is not planned: Fail the test
			Assertions.fail("Some other error than CorruptedHbefaTableException occured. This is not planned and should be investigated");
		}
	}

}
