package org.matsim.core.scoring;

import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.utils.collections.Tuple;

public class PlanElementsToScore implements ActivityHandler, LegHandler{
	
	private Scenario scenario = null;
	private ScoringFunctionFactory scoringFunctionFactory = null;
	private final double learningRate;

	private final TreeMap<Id, Tuple<Plan, ScoringFunction>> agentScorers = new TreeMap<Id, Tuple<Plan, ScoringFunction>>();

	private double scoreSum = 0.0;
	private long scoreCount = 0;
	
	private static Logger logger = Logger.getLogger(PlanElementsToScore.class);

	public PlanElementsToScore(Scenario scenario, ScoringFunctionFactory scoringFunctionFactory, double learningRate) {
		this.scenario = scenario;
		this.scoringFunctionFactory = scoringFunctionFactory;
		this.learningRate = learningRate;
	}

	private Tuple<Plan, ScoringFunction> getPlanAndScoringFunctionForAgent(final Id agentId) {
		Tuple<Plan, ScoringFunction> data = this.agentScorers.get(agentId);
		if (data == null) {
			Person person = this.scenario.getPopulation().getPersons().get(agentId);
			if (person == null) {
				return null;
			}
			data = new Tuple<Plan, ScoringFunction>(person.getSelectedPlan(), this.scoringFunctionFactory.createNewScoringFunction(person.getSelectedPlan()));
			this.agentScorers.put(agentId, data);
		}
		return data;
	}

	/**
	 * Returns the scoring function for the specified agent. If the agent
	 * already has a scoring function, that one is returned. If the agent does
	 * not yet have a scoring function, a new one is created and assigned to the
	 * agent and returned.
	 *
	 * @param agentId
	 *            The id of the agent the scoring function is requested for.
	 * @return The scoring function for the specified agent.
	 */
	public ScoringFunction getScoringFunctionForAgent(final Id agentId) {
		Tuple<Plan, ScoringFunction> data = this.getPlanAndScoringFunctionForAgent(agentId);
		if (data == null) {
			return null;
		}
		return data.getSecond();
	}
	
	@Override
    public void handleActivity(Id agentId, Activity activity) {
        ScoringFunction scoringFunctionForAgent = this.getScoringFunctionForAgent(agentId);
        if (scoringFunctionForAgent != null) {
            scoringFunctionForAgent.handleActivity(activity);
        }
    }

    @Override
    public void handleLeg(Id agentId, Leg leg) {
        ScoringFunction scoringFunctionForAgent = this.getScoringFunctionForAgent(agentId);
        if (scoringFunctionForAgent != null) {
            scoringFunctionForAgent.handleLeg(leg);
        }
    }
    
    

	public void finish() {
		for (Map.Entry<Id, Tuple<Plan, ScoringFunction>> entry : this.agentScorers.entrySet()) {
			Plan plan = entry.getValue().getFirst();
			logger.trace("Now scoring agent " + plan.getPerson().getId());
			ScoringFunction sf = entry.getValue().getSecond();
			sf.finish();
			double score = sf.getScore();
			Double oldScore = plan.getScore();
			if (oldScore == null) {
				plan.setScore(score);
			} else {
				plan.setScore(this.learningRate * score + (1 - this.learningRate) * oldScore);
			}

			this.scoreSum += score;
			this.scoreCount++;
		}
	}

	public double getAveragePlanPerformance() {
		if (this.scoreSum == 0)
			return Double.NaN;
		else
			return (this.scoreSum / this.scoreCount);
	}
	
}