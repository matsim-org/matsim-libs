package org.matsim.core.scoring;

import java.util.Map.Entry;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioImpl;
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
	
	private final TreeMap<Id,  PlanImpl> agentRecords = new TreeMap<Id,PlanImpl>();

	private Scenario scenario;

	public ScoringFunctionsForPopulation(Scenario scenario, ScoringFunctionFactory scoringFunctionFactory) {
		this.scoringFunctionFactory = scoringFunctionFactory;
		this.scenario = scenario;
		for (Person person : scenario.getPopulation().getPersons().values()) {
			ScoringFunction data = this.scoringFunctionFactory.createNewScoringFunction(person.getSelectedPlan());
			this.agentScorers.put(person.getId(), data);
			
		}
		if (scenario.getConfig().planCalcScore().isWriteExperiencedPlans()) {
			for (Person person : scenario.getPopulation().getPersons().values()) {
				this.agentRecords.put(person.getId(), new PlanImpl());
			}
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
            if (scenario.getConfig().planCalcScore().isWriteExperiencedPlans()) {
                agentRecords.get(agentId).addActivity(activity);
            }
        }
    }

    @Override
    public void handleLeg(Id agentId, Leg leg) {
        ScoringFunction scoringFunctionForAgent = this.getScoringFunctionForAgent(agentId);
        if (scoringFunctionForAgent != null) {
            scoringFunctionForAgent.handleLeg(leg);
            if (scenario.getConfig().planCalcScore().isWriteExperiencedPlans()) {
            	agentRecords.get(agentId).addLeg(leg);
            }
        }
    }
    
    public void finishScoringFunctions() {
		for (ScoringFunction sf : agentScorers.values()) {
			sf.finish();
		}
	}

	public void writeExperiencedPlans(String iterationFilename) {
		PopulationImpl population = new PopulationImpl((ScenarioImpl) scenario);
		for (Entry<Id, PlanImpl> entry : agentRecords.entrySet()) {
			PersonImpl person = new PersonImpl(entry.getKey());
			PlanImpl plan = entry.getValue();
			plan.setScore(getScoringFunctionForAgent(person.getId()).getScore());
			person.addPlan(plan);
			population.addPerson(person);
		}
		new PopulationWriter(population, scenario.getNetwork()).writeV5(iterationFilename);	
	}

}
