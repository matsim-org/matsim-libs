package playground.yu.scoring.withAttrRecorder.leftTurn;

import org.matsim.core.config.Config;

public class AdditionalScoringParameters {
	public final double constantLeftTurn;

	public AdditionalScoringParameters(final Config config) {
		String constLeftTurnStr = config.findParam("bse", "constantLeftTurn");
		if (constLeftTurnStr != null) {
			constantLeftTurn = Double.parseDouble(constLeftTurnStr);
		} else {
			constantLeftTurn = 0d;
		}
	}
}