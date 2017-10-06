package tutorial.programming.individualizedScoringParameters;

import org.junit.Test;
import org.matsim.core.utils.io.IOUtils;

import tutorial.scoring.individualizedScoringParameters.RunExampleIndividualizedScoring;

import java.io.File;

/**
 * @author thibautd
 */
public class IT {
	@Test
	public void testMain() {
		final String pathname = "output/exampleIndividualScores/";
		try {
			IOUtils.deleteDirectoryRecursively(new File(pathname).toPath());
		} catch ( IllegalArgumentException ee ) {
			// (normally, the directory should NOT be there initially.  It might, however, be there if someone ran the main class in some other way,
			// and did not remove the directory afterwards.)
		}
		RunExampleIndividualizedScoring.main();

		IOUtils.deleteDirectoryRecursively(new File(pathname).toPath());
		// (here, the directory should have been there)
	}
}
