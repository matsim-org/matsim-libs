/**
 * 
 */
package playground.yu.scoring.withAttrRecorder.leftTurn;

import org.matsim.core.config.Config;
import org.matsim.core.scoring.ScoringFunctionFactory;

import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.testAttRecorder.PCCtl;

/**
 * @author yu
 * 
 */
public class PCCtlwithLeftTurnPenalty extends PCCtl {

	public PCCtlwithLeftTurnPenalty(Config config) {
		super(config);
	}

	@Override
	protected ScoringFunctionFactory loadScoringFunctionFactory() {
		return new CharyparNagelScoringFunctionFactoryWithLeftTurnPenalty(
				config, network);
	}

}
