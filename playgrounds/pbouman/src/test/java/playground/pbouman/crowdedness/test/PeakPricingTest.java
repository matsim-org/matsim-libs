/**
 * 
 */
package playground.pbouman.crowdedness.test;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import playground.pbouman.transitfares.TransitFares;

/**
 * @author nagel
 *
 */
public class PeakPricingTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	/**
	 * This test is meant to re-test the peak hour pricing study in Paul Bowman's dissertation.  But the test ain't there yet.  kai, mar'17
	 */
	@Test public final void testPeakPricing() {

		Config config = ConfigUtils.loadConfig("../../examples/scenarios/pt-simple/config.xml");

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(10);
		
//		{
//		StrategySettings stratSets = new StrategySettings() ;
//		stratSets.setStrategyName(DefaultStrategy.ChangeSingleTripMode.toString());
//		stratSets.setWeight(0.1);
//		config.strategy().addStrategySettings(stratSets);
//		}
//		config.changeMode().setModes(new String[] {"car","pt"}) ;
		// not necessary, we are only interested in dp time change. kai, mar'17

		Scenario scenario = ScenarioUtils.loadScenario(config);

		Controler controler = new Controler(scenario);
		
		TransitFares.activateTransitPricing(controler, false, false);
		
		controler.run();

	}

}
