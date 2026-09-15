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
import org.matsim.testcases.MatsimTestUtils;

class HbefaConsistencyCheckerTest {

	@RegisterExtension
	MatsimTestUtils utils = new MatsimTestUtils();

	private EmissionsConfigGroup getEmissionsConfigGroup(String detCold, String detWarm) {
		EmissionsConfigGroup ecg = new EmissionsConfigGroup();
		ecg.setDetailedVsAverageLookupBehavior(EmissionsConfigGroup.DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageThenAverageTable);
		ecg.setWritingEmissionsEvents(false);
		ecg.setAverageColdEmissionFactorsFile(utils.getClassInputDirectory() + "EFA_ColdStart_TEST_Average.csv");
		ecg.setAverageWarmEmissionFactorsFile(utils.getClassInputDirectory() + "EFA_HOT_TEST_Average.csv");
		ecg.setDetailedColdEmissionFactorsFile(utils.getClassInputDirectory() + detCold);
		ecg.setDetailedWarmEmissionFactorsFile(utils.getClassInputDirectory() + detWarm);
		return ecg;
	}

	@Test
	public void validHbefaTableTest() {
		// Set up the emission module
		EmissionsConfigGroup ecg = getEmissionsConfigGroup("EFA_ColdStart_TEST_detailed_valid.csv", "EFA_HOT_TEST_detailed_valid.csv");

		// Create the scenario. The test should run through without problems. If this test fails the Checker gives false alarm and should be investigated.
		Config config = utils.createConfig();
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
		EmissionsConfigGroup ecg = getEmissionsConfigGroup("EFA_ColdStart_TEST_detailed_corrupted.csv", "EFA_HOT_TEST_detailed_corrupted.csv");
		// Create the scenario. The test should run through without problems. If this test fails the Checker gives false alarm and should be investigated.
		Config config = utils.createConfig();
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
