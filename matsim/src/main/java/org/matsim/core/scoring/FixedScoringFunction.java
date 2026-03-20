package org.matsim.core.scoring;

import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;

/**
 * This scoring function returns a fixed, predefined score.
 */
public class FixedScoringFunction implements ScoringFunction{

	private final double score;

	public FixedScoringFunction(double score) {
		this.score = score;
	}

	@Override
	public double getScore() {
		return score;
	}

	@Override
	public void handleActivity(Activity activity) {
		throw new UnsupportedOperationException("No supported.");
	}

	@Override
	public void handleLeg(Leg leg) {
		throw new UnsupportedOperationException("No supported.");
	}

	@Override
	public void agentStuck(double time) {
		throw new UnsupportedOperationException("No supported.");
	}

	@Override
	public void addMoney(double amount) {
		throw new UnsupportedOperationException("No supported.");
	}

	@Override
	public void addScore(double amount) {
		throw new UnsupportedOperationException("No supported.");
	}

	@Override
	public void finish() {
		throw new UnsupportedOperationException("No supported.");
	}

	@Override
	public void handleEvent(Event event) {
		throw new UnsupportedOperationException("No supported.");
	}
}
