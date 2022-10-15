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

	private static final Logger log = LogManager.getLogger(ExampleMobsimOfSimpleLSPTest.class);
	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testForRuntimeExceptions() {
		try {
			ExampleMobsimOfSimpleLSP.main(new String[]{
					"--config:controler.outputDirectory=" + utils.getOutputDirectory()
			});

		} catch (Exception ee) {
			log.fatal(ee);
			fail();
		}
	}
}