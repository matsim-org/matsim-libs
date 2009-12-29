package org.matsim.socialnetworks.scoring;

import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Plan;
import org.matsim.scoring.ScoringFunction;

public class SNScoringFunction03 implements ScoringFunction {

	private final ScoringFunction scoringFunction;
	private final Plan plan;

	public SNScoringFunction03(final Plan plan, final ScoringFunction scoringFunction) {
		this.scoringFunction = scoringFunction;
		this.plan = plan;
		}

	public void finish() {
		this.scoringFunction.finish();
	}

	public void agentStuck(final double time) {
		this.scoringFunction.agentStuck(time);
	}

	public void endActivity(final double time) {
		this.scoringFunction.endActivity(time);
	}

	public void endLeg(final double time) {
		this.scoringFunction.endLeg(time);
	}

	public double getScore() {
		return this.scoringFunction.getScore() - getPlanLength(plan);
	}

	public void reset() {
		this.scoringFunction.finish();
	}

	public void startActivity(final double time, final Act act) {
		this.scoringFunction.startActivity(time, act);
	}

	public void startLeg(final double time, final Leg leg) {
		this.scoringFunction.startLeg(time, leg);
	}
	public double getPlanLength(Plan plan){

		double length=0.;
		for (int i = 0, max= plan.getActsLegs().size(); i < max-2; i += 2) {
			Act act1 = (Act)(plan.getActsLegs().get(i));
			Act act2 = (Act)(plan.getActsLegs().get(i+2));

			if (act2 != null && act1 != null) {
				double dist = act1.getCoord().calcDistance(act2.getCoord());
				length += dist;
			}
		}
		return length;
	}
}
