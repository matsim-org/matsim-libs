package org.matsim.contrib.freight.analysis;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;

public class FreightAnalysisEventBasedTest {

	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	@Test
	public void runFreightAnalysisEventBasedTest() throws IOException {

		RunFreightAnalysisEventBased analysisEventBased = new RunFreightAnalysisEventBased(testUtils.getClassInputDirectory(), testUtils.getOutputDirectory(),null);
		analysisEventBased.runAnalysis();

		MatsimTestUtils.assertEqualFilesLineByLine(testUtils.getInputDirectory() + "Carrier_stats.tsv",  testUtils.getOutputDirectory() + "Carrier_stats.tsv");
		MatsimTestUtils.assertEqualFilesLineByLine(testUtils.getInputDirectory() + "Load_perVehicle.tsv", testUtils.getOutputDirectory() + "Load_perVehicle.tsv");
		MatsimTestUtils.assertEqualFilesLineByLine(testUtils.getInputDirectory() + "TimeDistance_perVehicle.tsv", testUtils.getOutputDirectory() + "TimeDistance_perVehicle.tsv");
		MatsimTestUtils.assertEqualFilesLineByLine(testUtils.getInputDirectory() + "TimeDistance_perVehicleType.tsv", testUtils.getOutputDirectory() + "TimeDistance_perVehicleType.tsv");
	}
}
