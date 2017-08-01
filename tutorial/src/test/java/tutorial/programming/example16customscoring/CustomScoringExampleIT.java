package tutorial.programming.example16customscoring;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.testcases.MatsimTestUtils;

import tutorial.scoring.example16customscoring.RunCustomScoringExample;

import java.io.File;

/**
 * Created by gabriel on 30.11.15.
 *
 * Test class for CustomScoringExample
 */
public class CustomScoringExampleIT {

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils() ;

    /**
     *
     */
    @Test
    public void testCustomScoring() {
        final String pathname = "./output/example";
        try {
            IOUtils.deleteDirectoryRecursively(new File(pathname).toPath());
        } catch ( IllegalArgumentException ee ) {
            // (normally, the directory should NOT be there initially.  It might, however, be there if someone ran the main class in some other way,
            // and did not remove the directory afterwards.)
        }
        RunCustomScoringExample.main(null);

        IOUtils.deleteDirectoryRecursively(new File(pathname).toPath());
        // (here, the directory should have been there)
    }

}
