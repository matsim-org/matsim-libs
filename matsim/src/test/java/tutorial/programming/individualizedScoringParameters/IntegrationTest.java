package tutorial.programming.individualizedScoringParameters;

import org.junit.Test;
import org.matsim.core.utils.io.IOUtils;

import java.io.File;

/**
 * @author thibautd
 */
public class IntegrationTest {
	@Test
	public void testMain() {
		final String pathname = "output/exampleIndividualScores/";
		try {
			IOUtils.deleteDirectory(new File(pathname),false);
		} catch ( IllegalArgumentException ee ) {
			// (normally, the directory should NOT be there initially.  It might, however, be there if someone ran the main class in some other way,
			// and did not remove the directory afterwards.)
		}
		RunExampleIndividualizedScoring.main();

		IOUtils.deleteDirectory(new File(pathname),false);
		// (here, the directory should have been there)
	}
}
