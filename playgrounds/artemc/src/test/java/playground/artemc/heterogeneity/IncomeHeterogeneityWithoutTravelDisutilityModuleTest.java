package playground.artemc.heterogeneity;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerDefaultsModule;
import org.matsim.testcases.MatsimTestUtils;

/**
 * Created by artemc on 25/4/15.
 */
public class IncomeHeterogeneityWithoutTravelDisutilityModuleTest {

	final int nIterations = 1;

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testControlerWithIncomeHeterogeneityWorks() {
		Config config = utils.loadConfig(utils.getClassInputDirectory() + "/config.xml");
		config.plans().setInputPersonAttributeFile(utils.getClassInputDirectory() + "/personAttributes.xml");
		ConfigUtils.addOrGetModule(config, HeterogeneityConfigGroup.GROUP_NAME, HeterogeneityConfigGroup.class);
		Controler controler = new Controler(config);
		controler.setModules(new ControlerDefaultsModule(), new IncomeHeterogeneityModule());
		config.controler().setLastIteration(nIterations-1);
		controler.run();
	}
	
}