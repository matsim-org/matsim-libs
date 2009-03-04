package org.matsim.scoring;

import java.util.ArrayList;

import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.Leg;
import org.matsim.scoring.interfaces.ActivityScoring;
import org.matsim.scoring.interfaces.AgentStuckScoring;
import org.matsim.scoring.interfaces.BasicScoring;
import org.matsim.scoring.interfaces.LegScoring;
import org.matsim.scoring.interfaces.MoneyScoring;

public class ScoringFunctionAccumulator implements ScoringFunction {

	private ArrayList<BasicScoring> basicScoringFunctions = new ArrayList<BasicScoring>();
	private ArrayList<ActivityScoring> activityScoringFunctions = new ArrayList<ActivityScoring>();
	private ArrayList<MoneyScoring> moneyScoringFunctions = new ArrayList<MoneyScoring>();
	private ArrayList<LegScoring> legScoringFunctions = new ArrayList<LegScoring>();
	private ArrayList<AgentStuckScoring> agentStuckScoringFunctions = new ArrayList<AgentStuckScoring>();

	public void addMoney(double amount) {
		for (MoneyScoring moneyScoringFunction : moneyScoringFunctions) {
			moneyScoringFunction.addMoney(amount);
		}
	}

	public void agentStuck(double time) {
		for (AgentStuckScoring agentStuckScoringFunction : agentStuckScoringFunctions) {
			agentStuckScoringFunction.agentStuck(time);
		}
	}

	public void startActivity(double time, Act act) {
		for (ActivityScoring activityScoringFunction : activityScoringFunctions) {
			activityScoringFunction.startActivity(time, act);
		}
	}

	public void endActivity(double time) {
		for (ActivityScoring activityScoringFunction : activityScoringFunctions) {
			activityScoringFunction.endActivity(time);
		}
	}

	public void startLeg(double time, Leg leg) {
		for (LegScoring legScoringFunction : legScoringFunctions) {
			legScoringFunction.startLeg(time, leg);
		}
	}

	public void endLeg(double time) {
		for (LegScoring legScoringFunction : legScoringFunctions) {
			legScoringFunction.endLeg(time);
		}
	}

	public void finish() {
		for (BasicScoring basicScoringFunction : basicScoringFunctions) {
			basicScoringFunction.finish();
		}
	}

	/**
	 * Add the score of all functions.
	 */
	public double getScore() {
		double score = 0;
		for (BasicScoring basicScoringFunction : basicScoringFunctions) {
			score += basicScoringFunction.getScore();
		}
		return score;
	}

	public void reset() {
		for (BasicScoring basicScoringFunction : basicScoringFunctions) {
			basicScoringFunction.reset();
		}
	}

	/**
	 * add the scoring function the list of functions, it implemented the
	 * interfaces.
	 * 
	 * @param scoringFunction
	 */
	public void addScoringFunction(BasicScoring scoringFunction) {
		basicScoringFunctions.add(scoringFunction);

		if (scoringFunction instanceof ActivityScoring) {
			activityScoringFunctions.add((ActivityScoring) scoringFunction);
		}

		if (scoringFunction instanceof AgentStuckScoring) {
			agentStuckScoringFunctions.add((AgentStuckScoring) scoringFunction);
		}

		if (scoringFunction instanceof LegScoring) {
			legScoringFunctions.add((LegScoring) scoringFunction);
		}

		if (scoringFunction instanceof MoneyScoring) {
			moneyScoringFunctions.add((MoneyScoring) scoringFunction);
		}

	}

}
