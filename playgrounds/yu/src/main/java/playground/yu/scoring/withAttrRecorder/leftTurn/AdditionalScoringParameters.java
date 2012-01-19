package playground.yu.scoring.withAttrRecorder.leftTurn;

import org.matsim.core.config.Config;

import playground.yu.integration.cadyts.CalibrationConfig;

public class AdditionalScoringParameters {
	public final double constantLeftTurn;

	public AdditionalScoringParameters(final Config config) {
		String constLeftTurnStr = config.findParam(
				CalibrationConfig.BSE_CONFIG_MODULE_NAME,
				CalibrationConfig.CONSTANT_LEFT_TURN);
		if (constLeftTurnStr != null) {
			constantLeftTurn = Double.parseDouble(constLeftTurnStr);
		} else {
			constantLeftTurn = 0d;
		}
	}
}