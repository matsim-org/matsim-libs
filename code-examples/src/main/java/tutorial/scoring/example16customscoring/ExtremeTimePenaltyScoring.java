package tutorial.scoring.example16customscoring;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.scoring.SumScoringFunction;

final class ExtremeTimePenaltyScoring implements SumScoringFunction.ActivityScoring {
	// Some people (not all) really do not like leaving home early or coming home late. These people
	// get extra penalties if that should happen.
	// This property is not directly attached to the Person object. I have to look it up through my 
	// custom attribute table which I defined above.

	private double score;

	@Override public void handleFirstActivity(Activity act) {
		if (act.getEndTime() < (6 * 60 * 60)) {
			score -= 300.0;
		} 
	}

	@Override public void handleActivity(Activity act) {} // Not doing anything on mid-day activities.

	@Override public void handleLastActivity(Activity act) {	
		if (act.getStartTime() > (20.0 * 60 * 60)) {
			score -= 100.0;
		}
	}

	@Override public void finish() {}

	@Override
	public double getScore() {
		return score;
	}
}