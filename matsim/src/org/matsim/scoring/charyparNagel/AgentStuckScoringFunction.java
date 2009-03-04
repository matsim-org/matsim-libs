package org.matsim.scoring.charyparNagel;

import org.matsim.scoring.CharyparNagelScoringParameters;
import org.matsim.scoring.interfaces.AgentStuckScoring;
import org.matsim.scoring.interfaces.BasicScoring;


public class AgentStuckScoringFunction implements AgentStuckScoring, BasicScoring {

	protected double score;

	private static final double INITIAL_SCORE = 0.0;

	/** The parameters used for scoring */
	protected final CharyparNagelScoringParameters params;

	public AgentStuckScoringFunction(final CharyparNagelScoringParameters params) {
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
