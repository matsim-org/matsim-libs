/**
 * 
 */
package playground.yu.scoring.withAttrRecorder.leftTurn;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import playground.yu.tests.parameterCalibration.naiveWithoutUC.SimCntLogLikelihoodCtlListener;

/**
 * @author yu
 * 
 */
public class LeftTurnPenaltyControlerWithLlh {
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Config config;
		if (args.length < 1) {
			config = ConfigUtils
					.loadConfig("test/input/2car1ptRoutes/pcTravCarLeftTurn/cfgBase-3_-1.xml");
		} else/* args.length>=1 */{
			config = ConfigUtils.loadConfig(args[0]);
		}
		LeftTurnPenaltyControler controler = new LeftTurnPenaltyControler(
				config);
		controler.addControlerListener(new SimCntLogLikelihoodCtlListener());
		controler.setOverwriteFiles(true);
		controler.setCreateGraphs(false);
		controler.run();
	}
}
