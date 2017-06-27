package playground.artemc.heterogeneity;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerDefaultsModule;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by artemc on 25/4/15.
 */
public class IncomeHeterogeneityWithoutTravelDisutilityModuleIT {

	final int nIterations = 1;

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testControlerWithIncomeHeterogeneityWorks() throws MalformedURLException {
		Config config = utils.loadConfig(IOUtils.newUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml"));
		ConfigUtils.loadConfig(config, utils.getClassInputDirectory() + "/config.xml");
		config.plans().setInputPersonAttributeFile("file://" + new File(utils.getClassInputDirectory() + "/personAttributes.xml").getAbsolutePath());
		ConfigUtils.addOrGetModule(config, HeterogeneityConfigGroup.GROUP_NAME, HeterogeneityConfigGroup.class);
		Controler controler = new Controler(config);
		controler.setModules(new ControlerDefaultsModule(), new IncomeHeterogeneityModule());
		config.controler().setLastIteration(nIterations-1);
		controler.run();
	}
	
}