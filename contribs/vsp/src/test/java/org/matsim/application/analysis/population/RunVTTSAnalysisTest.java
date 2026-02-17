package org.matsim.application.analysis.population;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.testcases.MatsimTestUtils;

class RunVTTSAnalysisTest{
	// yyyy !! GenerateExperiencedPlansWithVTTSTest has solved this in a way that one can overwrite the output dir !!

	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils();

	@Disabled // not testing anything useful as of now
	@Test void testMain() {
		String[] args = {
			"--path", utils.getInputDirectory(),
			"--runId", "test",
			"--threads", "1"
		};

		// yy the way the class under test is set up, it is not possible to have the output in a directory different from the
		// input directory.  In consequence, the test output goes into the test input directory.  kai, nov'25

		AddVttsEtcToActivities.main( args );

//		String expected = utils.getInputDirectory() + "/test.reference_experienced_plans.xml.gz";
//		String actual = utils.getInputDirectory() + "/test.output_experienced_plans.xml.gz";
//		PopulationComparison.Result result = PopulationUtils.comparePopulations( expected, actual );
//		Assertions.assertSame( result, PopulationComparison.Result.equal );
	}


}
