package org.matsim.codeexamples.programming.individualizedScoringParameters;

import org.junit.Test;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

import org.matsim.codeexamples.scoring.individualizedScoringParameters.RunIndividualizedScoringExample;

import java.io.File;

/**
 * @author thibautd
 */
public class IntegrationTest {
	@Test
	public void testMain() {
		final String pathname = "output/exampleIndividualScores/";
		try {
			IOUtils.deleteDirectoryRecursively(new File(pathname).toPath());
		} catch ( IllegalArgumentException ee ) {
			// (normally, the directory should NOT be there initially.  It might, however, be there if someone ran the main class in some other way,
			// and did not remove the directory afterwards.)
		} catch ( UncheckedIOException ee ) {
		}
		RunIndividualizedScoringExample.main();

		IOUtils.deleteDirectoryRecursively(new File(pathname).toPath());
		// (here, the directory should have been there)
	}
}
