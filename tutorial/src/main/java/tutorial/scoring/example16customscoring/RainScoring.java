package tutorial.scoring.example16customscoring;

import org.matsim.api.core.v01.events.Event;
import org.matsim.core.scoring.SumScoringFunction;

final class RainScoring implements SumScoringFunction.ArbitraryEventScoring {
	private double score;

	@Override
	public void handleEvent(Event event) {
		if (event instanceof RainOnPersonEvent) {
			score -= 1000.0;
		}
	}

	@Override public void finish() {}

	@Override
	public double getScore() {
		return score;
	}
}