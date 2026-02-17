package org.matsim.application.analysis.population;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.PopulationComparison;
import org.matsim.testcases.MatsimTestUtils;

class GenerateExperiencedPlansWithVTTSTest{

	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils();

	@Disabled // I think that this combined functionality no longer exists.
	@Test void testMain() {
		String[] args = {
			"--path", utils.getInputDirectory(),
			"--runId", "test",
			"--output", utils.getOutputDirectory(),
			"--threads", "1"
		};

		// yy the way the class under test is set up, it is not possible to have the output in a directory different from the
		// input directory.  In consequence, the test output goes into the test input directory.  kai, nov'25

//		GenerateExperiencedPlansWithVTTS.main( args );
		Assertions.assertTrue( false );
		// repair test

		String expected = utils.getInputDirectory() + "/test.reference_experienced_plans.xml.gz";
		String actual = utils.getOutputDirectory() + "/test.output_experienced_plans.xml.gz";
		PopulationComparison.Result result = PopulationUtils.comparePopulations( expected, actual );
		Assertions.assertSame( PopulationComparison.Result.equal, result );
	}

}
