package org.matsim.application;

import org.junit.jupiter.api.Test;
import org.matsim.application.analysis.TestAnalysis;
import org.matsim.application.analysis.TestDependentAnalysis;
import org.matsim.application.options.ShpOptions;
import org.matsim.application.prepare.population.CleanPopulation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class ApplicationUtilsTest {

	@Test
	void shp() {

		assertTrue(ApplicationUtils.acceptsOptions(TestAnalysis.class, ShpOptions.class));

		assertFalse(ApplicationUtils.acceptsOptions(TestDependentAnalysis.class, ShpOptions.class));

	}

	@Test
	void checkCommand() {

		ApplicationUtils.checkCommand(TestAnalysis.class);

		// Just picked a random class that does not use the command spec, if this command is converted, another one has to be picked.
		assertThrows(IllegalArgumentException.class, () -> ApplicationUtils.checkCommand(CleanPopulation.class));

	}

	@Test
	void mergeArgs() {


		String[] result = ApplicationUtils.mergeArgs(new String[]{"--a", "1", "--b", "2"},
			"--a", "3", "--c", "4");

		assertThat(result)
			.containsExactly("--a", "1", "--b", "2", "--a", "3", "--c", "4");

	}
}
