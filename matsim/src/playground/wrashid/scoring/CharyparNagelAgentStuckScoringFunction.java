package playground.wrashid.scoring;

import org.matsim.scoring.CharyparNagelScoringParameters;

import playground.wrashid.scoring.interfaces.AgentStuckScoringFunction;
import playground.wrashid.scoring.interfaces.BasicScoringFunction;

public class CharyparNagelAgentStuckScoringFunction implements AgentStuckScoringFunction, BasicScoringFunction {

	protected double score;

	private static final double INITIAL_SCORE = 0.0;

	/** The parameters used for scoring */
	protected final CharyparNagelScoringParameters params;

	public CharyparNagelAgentStuckScoringFunction(final CharyparNagelScoringParameters params) {
		this.params = params;
		this.reset();
	}

	public void reset() {
		this.score = INITIAL_SCORE;
	}

	public void agentStuck(final double time) {

		this.score += getStuckPenalty();
	}

	public void finish() {

	}

	public double getScore() {
		return this.score;
	}

	private double getStuckPenalty() {
		return this.params.abortedPlanScore;
	}

}
