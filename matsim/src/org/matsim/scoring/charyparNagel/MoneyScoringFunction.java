package org.matsim.scoring.charyparNagel;

import org.matsim.scoring.CharyparNagelScoringParameters;
import org.matsim.scoring.interfaces.BasicScoring;

import org.matsim.scoring.interfaces.MoneyScoring;

public class MoneyScoringFunction implements MoneyScoring, BasicScoring {

	protected double score;

	private static final double INITIAL_SCORE = 0.0;

	/** The parameters used for scoring */
	protected final CharyparNagelScoringParameters params;

	public MoneyScoringFunction(final CharyparNagelScoringParameters params) {
		this.params = params;
		this.reset();

	}

	public void reset() {
		this.score = INITIAL_SCORE;
	}

	public void addMoney(final double amount) {
		this.score += amount; // linear mapping of money to score
	}

	public void finish() {

	}

	public double getScore() {
		return this.score;
	}

}
