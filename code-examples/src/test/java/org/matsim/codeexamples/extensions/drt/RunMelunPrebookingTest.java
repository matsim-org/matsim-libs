package org.matsim.codeexamples.extensions.drt;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.testcases.MatsimTestUtils;

public class RunMelunPrebookingTest {
	@RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void runExample() {
		RunMelunPrebooking.RunSettings settings = new RunMelunPrebooking.RunSettings();

		settings.prebookingShare = 0.5;
		settings.submissionSlack = 3600.0;
		settings.enableExclusivity = true;

		RunMelunPrebooking.runSingle(new File(RunMelunPrebooking.DEFAULT_POPULATION_PATH),
				new File(RunMelunPrebooking.DEFAULT_NETWORK_PATH), new File(utils.getOutputDirectory()), settings);
	}
}
