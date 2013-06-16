package org.matsim.core.scoring;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scoring.EventsToActivities.ActivityHandler;
import org.matsim.core.scoring.EventsToLegs.LegHandler;
import org.matsim.core.utils.io.IOUtils;

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
	
	private final Map<Id,  Plan> agentRecords = new TreeMap<Id,Plan>();
	private final Map<Id,List<Double>> partialScores = new TreeMap<Id,List<Double>>() ;

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
				this.partialScores.put(person.getId(), new ArrayList<Double>() ) ;
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
                Collection<Double> partialScoresForAgent = partialScores.get(agentId) ;
                partialScoresForAgent.add( scoringFunctionForAgent.getScore() ) ;
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
                Collection<Double> partialScoresForAgent = partialScores.get(agentId) ;
                partialScoresForAgent.add( scoringFunctionForAgent.getScore() ) ;
            }
        }
    }
    
    public void finishScoringFunctions() {
		for (ScoringFunction sf : agentScorers.values()) {
			sf.finish();
		}
		if ( scenario.getConfig().planCalcScore().isWriteExperiencedPlans() ) {
			for ( Entry<Id, List<Double>> entry : this.partialScores.entrySet() ) {
				entry.getValue().add( 1, this.getScoringFunctionForAgent(entry.getKey()).getScore() ) ;
			}
		}
	}

	public void writeExperiencedPlans(String iterationFilename) {
		PopulationImpl population = new PopulationImpl((ScenarioImpl) scenario);
		for (Entry<Id, Plan> entry : agentRecords.entrySet()) {
			Person person = new PersonImpl(entry.getKey());
			Plan plan = entry.getValue();
			plan.setScore(getScoringFunctionForAgent(person.getId()).getScore());
			if ( plan.getScore().isNaN() ) {
				Logger.getLogger(this.getClass()).warn("score is NaN; plan:" + plan.toString() );
			}
			person.addPlan(plan);
			population.addPerson(person);
		}
		new PopulationWriter(population, scenario.getNetwork()).writeV5(iterationFilename);
		
		BufferedWriter out = IOUtils.getBufferedWriter(iterationFilename+".scores") ;
		for ( Entry<Id,List<Double>> entry : partialScores.entrySet() ) {
			try {
				out.write( entry.getKey().toString() ) ;
				for ( Double score : entry.getValue() ) {
					out.write( '\t'+ score.toString() ) ;
				}
				out.newLine() ;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			out.close() ;
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

}
