/**
 * 
 */
package playground.yu.scoring;

import org.matsim.core.api.experimental.ScenarioLoader;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;

/**
 * @author yu
 * 
 */
public class WalkScoringTest {

	/**
	 * test for the change of scoring function, because "walk"-mode will be
	 * implemented
	 * 
	 * @param args
	 */
	public static void main(final String[] args) {
		if (args == null || args.length == 0) {
			System.out.println("No argument given!");
			System.out.println("Usage: Controler config-file [dtd-file]");
			System.out.println();
		} else {
			Config config = new ScenarioLoader(args[0]).loadScenario()
					.getConfig();
			final Controler controler = new Controler(args);
			controler
					.setScoringFunctionFactory(new CharyparNagelScoringFunctionFactoryWithWalk(
							config.charyparNagelScoring()));
			controler.setWriteEventsInterval(100);
			controler.run();
		}
		System.exit(0);
	}
}
