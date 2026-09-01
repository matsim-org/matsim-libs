package org.matsim.application;

import org.junit.jupiter.api.Test;
import org.matsim.application.options.ShpOptions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests core helper methods for MATSim application commands.
 */
public class ApplicationUtilsTest {

	@Test
	void shp() {

		assertTrue(ApplicationUtils.acceptsOptions(TestAnalysis.class, ShpOptions.class));

		assertFalse(ApplicationUtils.acceptsOptions(TestDependentAnalysis.class, ShpOptions.class));

	}

	@Test
	void checkCommand() {

		ApplicationUtils.checkCommand(TestAnalysis.class);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> ApplicationUtils.checkCommand(CommandWithoutSpec.class));
		assertThat(exception).hasMessageContaining("has no @CommandSpec annotation");

	}

	@Test
	void mergeArgs() {


		String[] result = ApplicationUtils.mergeArgs(new String[]{"--a", "1", "--b", "2"},
			"--a", "3", "--c", "4");

		assertThat(result)
			.containsExactly("--a", "1", "--b", "2", "--a", "3", "--c", "4");

	}

	/**
	 * Command fixture intentionally missing {@link CommandSpec}.
	 */
	private static final class CommandWithoutSpec implements MATSimAppCommand {

		@Override
		public Integer call() {
			return 0;
		}
	}
}
