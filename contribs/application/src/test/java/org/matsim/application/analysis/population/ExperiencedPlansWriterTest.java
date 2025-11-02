package org.matsim.application.analysis.population;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.testcases.MatsimTestUtils;

class ExperiencedPlansWriterTest{

	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils();

	@Test void testMain() {
		String[] args = {
			"--path", utils.getInputDirectory(),
			"--runId", "test",
			"--threads", "1"
		};
		ExperiencedPlansWriter.main( args );
	}

}
