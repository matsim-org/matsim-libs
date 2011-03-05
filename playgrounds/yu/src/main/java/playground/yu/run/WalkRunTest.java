/**
 *
 */
package playground.yu.run;

import java.io.IOException;

import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.yu.scoring.CharyparNagelScoringFunctionFactoryWithWalk;

/**
 * @author yu
 * 
 */
public class WalkRunTest {

	/**
	 * test for the change of scoring function, because "walk"-mode will be
	 * implemented
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(final String[] args) throws IOException {
		if (args == null || args.length == 0) {
			System.out.println("No argument given!");
			System.out.println("Usage: Controler config-file [dtd-file]");
			System.out.println();
		} else {
			Config config = ConfigUtils.loadConfig(args[0]);
			final Controler controler = new Controler(config);
			controler
					.setScoringFunctionFactory(new CharyparNagelScoringFunctionFactoryWithWalk(
							config.planCalcScore(), config
//									.vspExperimental().getOffsetWalk()));
							.planCalcScore().getConstantWalk())) ;
			// controler.addControlerListener(new MZComparisonListener());
			controler.setWriteEventsInterval(100);
			controler.setOverwriteFiles(true);
			controler.run();
		}
		System.exit(0);
	}
}
