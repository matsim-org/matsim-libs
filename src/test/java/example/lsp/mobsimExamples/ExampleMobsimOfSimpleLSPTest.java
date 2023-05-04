package example.lsp.mobsimExamples;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

import static org.junit.Assert.fail;

/**
 * @author Kai Martins-Turner (kturner)
 */
public class ExampleMobsimOfSimpleLSPTest {
	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	private static final Logger log = LogManager.getLogger(ExampleMobsimOfSimpleLSPTest.class);


	@Test
	public void testForRuntimeExceptionsAndCompareEvents() {
		try {
			ExampleMobsimOfSimpleLSP.main(new String[]{
					"--config:controler.outputDirectory=" + utils.getOutputDirectory()
			});

		} catch (Exception ee) {
			log.fatal(ee);
			fail();
		}

		MatsimTestUtils.assertEqualEventsFiles(utils.getClassInputDirectory() + "output_events.xml.gz", utils.getOutputDirectory() + "output_events.xml.gz" );
	}

}