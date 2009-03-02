/**
 * 
 */
package playground.yu.scoring;

import org.matsim.config.Config;
import org.matsim.controler.Controler;
import org.matsim.gbl.Gbl;

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
	public static void main(String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: Controler config-file [dtd-file]");
			System.out.println();
		} else {
			Config config = Gbl.createConfig(args);
			final Controler controler = new Controler(args);
			controler.setScoringFunctionFactory(new CharyparNagelScoringFunctionFactoryWithWalk(config.charyparNagelScoring()));
			controler.run();
		}
		System.exit(0);
	}
}
