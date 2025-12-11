package org.matsim.application.analysis.population;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.PopulationComparison;
import org.matsim.testcases.MatsimTestUtils;

class ExperiencedPlansWriterTest{

	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils();

	@Test void testMain() {
		String[] args = {
			"--path", utils.getInputDirectory(),
			"--runId", "test",
			"--threads", "1"
		};

		// yy the way ExperiencedPlansWriter is set up, it is not possible to have the output in a directory different from the input directory. kai, nov'25

		ExperiencedPlansWriter.main( args );

		String expected = utils.getInputDirectory() + "/test.reference_experienced_plans.xml.gz";
		String actual = utils.getInputDirectory() + "/test.output_experienced_plans.xml.gz";
		PopulationComparison.Result result = PopulationUtils.comparePopulations( expected, actual );
		Assertions.assertTrue( result== PopulationComparison.Result.equal );
	}

}
