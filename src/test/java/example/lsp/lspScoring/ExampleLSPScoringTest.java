package example.lsp.lspScoring;

import lsp.LSP;
import lsp.LSPUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestUtils;

public class ExampleLSPScoringTest {
	private static final Logger log = LogManager.getLogger(ExampleLSPScoringTest.class);
	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testMain() {

		Config config = ExampleLSPScoring.prepareConfig();
		config.controler().setOutputDirectory(utils.getOutputDirectory());

		Scenario scenario = ExampleLSPScoring.prepareScenario(config);

		Controler controler = ExampleLSPScoring.prepareControler(scenario);

		controler.run();

		for (LSP lsp : LSPUtils.getLSPs(scenario).getLSPs().values()) {
			Assert.assertEquals(13.245734044444207, lsp.getSelectedPlan().getScore(), Double.MIN_VALUE);
		}

	}
}
