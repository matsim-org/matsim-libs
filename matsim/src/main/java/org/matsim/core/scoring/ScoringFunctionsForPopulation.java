package org.matsim.core.scoring;

import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scoring.EventsToActivities.ActivityHandler;
import org.matsim.core.scoring.EventsToLegs.LegHandler;

/**
 * 
 * This class helps EventsToScore by keeping ScoringFunctions for the entire Population - one per Person -, and dispatching Activities
 * and Legs to the ScoringFunctions. It also gives out the ScoringFunctions, so they can be given other events by EventsToScore.
 * It is not independently useful. Please do not make public.
 * @author michaz
 *
 */
class ScoringFunctionsForPopulation implements ActivityHandler, LegHandler {
	
	private ScoringFunctionFactory scoringFunctionFactory = null;

	private final TreeMap<Id,  ScoringFunction> agentScorers = new TreeMap<Id,ScoringFunction>();

	public ScoringFunctionsForPopulation(Scenario scenario, ScoringFunctionFactory scoringFunctionFactory) {
		this.scoringFunctionFactory = scoringFunctionFactory;
		for (Person person : scenario.getPopulation().getPersons().values()) {
			ScoringFunction data = this.scoringFunctionFactory.createNewScoringFunction(person.getSelectedPlan());
			this.agentScorers.put(person.getId(), data);
		}
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
		return this.agentScorers.get(agentId);
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
    
    public void finishScoringFunctions() {
		for (ScoringFunction sf : agentScorers.values()) {
			sf.finish();
		}
	}

}
