package playground.wrashid.scoring;

import java.util.ArrayList;

import org.matsim.events.AgentMoneyEvent;
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

	private Plan plan = null;
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

	public void endActivity(double time) {
		// TODO Auto-generated method stub

	}

	public void endLeg(double time) {
		// TODO Auto-generated method stub

	}

	public void finish() {
		// TODO Auto-generated method stub

	}

	public double getScore() {
		int score = 0;
		for (BasicScoringFunction basicScoringFunction : basicScoringFunctions) {
			score += basicScoringFunction.getScore();
		}
		return score;
	}

	public void reset() {
		// TODO Auto-generated method stub

	}

	public void startActivity(double time, Act act) {
		for (BasicScoringFunction scoringFunction : basicScoringFunctions) {
			if (scoringFunction instanceof ActivityScoringFunction) {
				ActivityScoringFunction activityScoringFunction = (ActivityScoringFunction) scoringFunction;
				activityScoringFunction.startActivity(time, act);
			}
		}
	}

	public void startLeg(double time, Leg leg) {
		// TODO Auto-generated method stub

	}

	public ScoringFunctionAccumulator(Plan plan) {
		basicScoringFunctions = new ArrayList<BasicScoringFunction>();
		this.plan = plan;
	}

	/**
	 * add the scoring function the list of functions, it implemented the interfaces.
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
