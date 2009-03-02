package playground.wrashid.scoring;

import java.util.ArrayList;

import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.Leg;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.scoring.ScoringFunction;

import playground.wrashid.scoring.interfaces.ActivityScoringFunction;
import playground.wrashid.scoring.interfaces.AgentStuckScoringFunction;
import playground.wrashid.scoring.interfaces.BasicScoringFunction;
import playground.wrashid.scoring.interfaces.LegScoringFunction;
import playground.wrashid.scoring.interfaces.MoneyScoringFunction;

public class ScoringFunctionAccumulator implements ScoringFunction {

	private ArrayList<BasicScoringFunction> basicScoringFunctions = null;
	private ArrayList<ActivityScoringFunction> activityScoringFunctions = null;
	private ArrayList<MoneyScoringFunction> moneyScoringFunctions = null;
	private ArrayList<LegScoringFunction> legScoringFunctions = null;
	private ArrayList<AgentStuckScoringFunction> agentStuckScoringFunctions = null;

	public void addMoney(double amount) {
		for (MoneyScoringFunction moneyScoringFunction : moneyScoringFunctions) {
			moneyScoringFunction.addMoney(amount);
		}
	}

	public void agentStuck(double time) {
		for (AgentStuckScoringFunction agentStuckScoringFunction : agentStuckScoringFunctions) {
			agentStuckScoringFunction.agentStuck(time);
		}
	}

	public void startActivity(double time, Act act) {
		for (ActivityScoringFunction activityScoringFunction : activityScoringFunctions) {
			activityScoringFunction.startActivity(time, act);
		}
	}

	public void endActivity(double time) {
		for (ActivityScoringFunction activityScoringFunction : activityScoringFunctions) {
			activityScoringFunction.endActivity(time);
		}
	}

	public void startLeg(double time, Leg leg) {
		for (LegScoringFunction legScoringFunction : legScoringFunctions) {
			legScoringFunction.startLeg(time, leg);
		}
	}

	public void endLeg(double time) {
		for (LegScoringFunction legScoringFunction : legScoringFunctions) {
			legScoringFunction.endLeg(time);
		}
	}

	public void finish() {
		for (BasicScoringFunction basicScoringFunction : basicScoringFunctions) {
			basicScoringFunction.finish();
		}
	}

	/**
	 * Add the score of all functions.
	 */
	public double getScore() {
		int score = 0;
		for (BasicScoringFunction basicScoringFunction : basicScoringFunctions) {
			score += basicScoringFunction.getScore();
		}
		return score;
	}

	public void reset() {
		for (BasicScoringFunction basicScoringFunction : basicScoringFunctions) {
			basicScoringFunction.reset();
		}
	}

	public ScoringFunctionAccumulator() {
		basicScoringFunctions = new ArrayList<BasicScoringFunction>();
	}

	/**
	 * add the scoring function the list of functions, it implemented the
	 * interfaces.
	 * 
	 * @param scoringFunction
	 */
	public void addScoringFunction(BasicScoringFunction scoringFunction) {
		basicScoringFunctions.add(scoringFunction);

		if (scoringFunction instanceof ActivityScoringFunction) {
			activityScoringFunctions.add((ActivityScoringFunction) scoringFunction);
		}

		if (scoringFunction instanceof AgentStuckScoringFunction) {
			agentStuckScoringFunctions.add((AgentStuckScoringFunction) scoringFunction);
		}

		if (scoringFunction instanceof LegScoringFunction) {
			legScoringFunctions.add((LegScoringFunction) scoringFunction);
		}

		if (scoringFunction instanceof MoneyScoringFunction) {
			moneyScoringFunctions.add((MoneyScoringFunction) scoringFunction);
		}

	}

}
