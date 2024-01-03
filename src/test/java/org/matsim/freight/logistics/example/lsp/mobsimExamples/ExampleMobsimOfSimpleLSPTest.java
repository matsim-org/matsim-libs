package org.matsim.freight.logistics.example.lsp.mobsimExamples;

import static org.junit.jupiter.api.Assertions.fail;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author Kai Martins-Turner (kturner)
 */
public class ExampleMobsimOfSimpleLSPTest {
	private static final Logger log = LogManager.getLogger(ExampleMobsimOfSimpleLSPTest.class);
	@RegisterExtension
	public final MatsimTestUtils utils = new MatsimTestUtils();

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