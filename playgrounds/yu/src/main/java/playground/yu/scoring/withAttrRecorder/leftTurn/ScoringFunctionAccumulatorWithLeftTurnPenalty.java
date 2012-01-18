/**
 * 
 */
package playground.yu.scoring.withAttrRecorder.leftTurn;

import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.core.scoring.charyparNagel.LegScoringFunction;
import org.matsim.core.scoring.interfaces.BasicScoring;

import playground.yu.scoring.withAttrRecorder.ScoringFunctionAccumulatorWithAttrRecorder;

/**
 * @author yu
 * 
 */
public class ScoringFunctionAccumulatorWithLeftTurnPenalty extends
ScoringFunctionAccumulatorWithAttrRecorder {
	private int nbOfLeftTurnAttrCar;

	public ScoringFunctionAccumulatorWithLeftTurnPenalty(
			CharyparNagelScoringParameters params) {
		super(params);
	}

	public int getNbOfLeftTurnAttrCar() {
		return nbOfLeftTurnAttrCar;
	}

	@Override
	public double getScore() {
		for (BasicScoring basicScoringFunction : basicScoringFunctions) {
			if (basicScoringFunction instanceof LegScoringFunction) {
				LegScoringFunctionWithLeftTurnPenalty legScoringFunction = (LegScoringFunctionWithLeftTurnPenalty) basicScoringFunction;
				nbOfLeftTurnAttrCar = legScoringFunction
				.getNbOfLeftTurnAttrCar();
			}
		}
		return super.getScore();
	}

}
