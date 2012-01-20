/**
 * 
 */
package playground.yu.scoring.withAttrRecorder.leftTurn;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.scoring.CharyparNagelScoringParameters;

import playground.yu.scoring.withAttrRecorder.LegScoringFunctionWithAttrRecorder;
import playground.yu.utils.LeftTurnIdentifier;

/**
 * adds the penalty for the left turns (just for experiments for creating of
 * some utility parameters that don't correlate with each other)
 * 
 * @author yu
 * 
 */
public class LegScoringFunctionWithLeftTurnPenalty extends
		LegScoringFunctionWithAttrRecorder {

	private int nbOfLeftTurnAttrCar = 0;
	protected final AdditionalScoringParameters additionalParams;

	public LegScoringFunctionWithLeftTurnPenalty(
			// Plan plan,
			CharyparNagelScoringParameters params, Network network,
			AdditionalScoringParameters additionalParams) {
		super(
		// plan,
				params, network);
		this.additionalParams = additionalParams;
	}

	@Override
	protected double calcLegScore(double departureTime, double arrivalTime,
			Leg leg) {
		nbOfLeftTurnAttrCar += LeftTurnIdentifier.getNumberOfLeftTurnsFromALeg(
				leg, network.getLinks());
		double originalScore = super.calcLegScore(departureTime, arrivalTime,
				leg);
		double score = originalScore + additionalParams.constantLeftTurn
				* nbOfLeftTurnAttrCar;
		// System.out.println(">>>>>\tconstantLeftTurn:\t"
		// + additionalParams.constantLeftTurn
		// + "\t*\tnbOfLeftTurnAttrCar:\t" + nbOfLeftTurnAttrCar
		// + "\toriginalScore:\t" + originalScore + "\tsum:\t" + score);
		return score;
	}

	public int getNbOfLeftTurnAttrCar() {
		return nbOfLeftTurnAttrCar;
	}

	@Override
	public void reset() {
		super.reset();
		nbOfLeftTurnAttrCar = 0;
	}

}
