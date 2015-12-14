/* *********************************************************************** *
 * project: org.matsim.*
 * EventsToScore.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.core.scoring;

import java.util.Map;
import java.util.HashMap;

import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.internal.HasPersonId;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.pt.transitSchedule.api.TransitSchedule;


/**
 * Calculates the score of the selected plans of a given scenario
 * based on events. The final scores are written to the selected plans of each person in the
 * scenario.
 * 
 * This class is the bridge between a stream of Events, the ScoringFunctionFactory and the Plan database.
 * This mechanism is considered core to MATSim, and changing it is not supported, except of course
 * by providing your own ScoringFunctionFactory.
 * 
 * Therefore, this class is instantiated and used by the Controler. Create your own instance if you want
 * to compute scores from an Event file, for example. You will still need a Scenario with proper selected
 * Plans, though. This is not yet fully decoupled.
 *
 * @author mrieser, michaz
 */
public class EventsToScore implements BasicEventHandler {
	static private final Logger log = Logger.getLogger(EventsToScore.class);

	private final PlansConfigGroup plansConfigGroup;
	private EventsToActivities eventsToActivities;
	private EventsToLegs eventsToLegs;
	private ScoringFunctionsForPopulation scoringFunctionsForPopulation;
	private final Population population;
	private final ScoringFunctionFactory scoringFunctionFactory;
	private final Network network;
	private @Inject(optional = true) TransitSchedule transitSchedule = null;

	private double learningRate;
	private boolean finished = false;
	
	private int iteration = -1 ;

	private double scoreSum = 0.0;
	private long scoreCount = 0;
	private Integer scoreMSAstartsAtIteration;


	@Inject
	EventsToScore(PlansConfigGroup plansConfigGroup, ControlerConfigGroup controlerConfigGroup, PlanCalcScoreConfigGroup planCalcScoreConfigGroup, ScoringFunctionFactory factory, EventsManager eventsManager, Population population, Network network) {
		this.population = population;
		this.plansConfigGroup = plansConfigGroup;
		this.scoringFunctionFactory = factory;
		this.network = network;
		this.learningRate = planCalcScoreConfigGroup.getLearningRate();
		initHandlers(scoringFunctionFactory);
		init(planCalcScoreConfigGroup, controlerConfigGroup);
		// With the Inject-Constructor, this class adds itself as an EventHandler.
		eventsManager.addHandler(this);
	}

	public EventsToScore(final Scenario scenario, final ScoringFunctionFactory factory) {
		this(scenario, factory, 1.0);
	}

	public EventsToScore(final Scenario scenario, final ScoringFunctionFactory scoringFunctionFactory, final double learningRate) {
		this.population = scenario.getPopulation();
		this.network = scenario.getNetwork();
		this.plansConfigGroup = scenario.getConfig().plans();
		if (scenario.getConfig().transit().isUseTransit()) {
			this.transitSchedule = scenario.getTransitSchedule();
		}
		this.scoringFunctionFactory = scoringFunctionFactory;
		this.learningRate = learningRate;
		initHandlers(scoringFunctionFactory);
		init(scenario.getConfig().planCalcScore(), scenario.getConfig().controler());
	}

	private void init(PlanCalcScoreConfigGroup planCalcScoreConfigGroup, ControlerConfigGroup controlerConfigGroup) {
		if (planCalcScoreConfigGroup.getFractionOfIterationsToStartScoreMSA()!=null ) {
			final int diff = controlerConfigGroup.getLastIteration() - controlerConfigGroup.getFirstIteration();
			this.scoreMSAstartsAtIteration = (int) (diff
				* planCalcScoreConfigGroup.getFractionOfIterationsToStartScoreMSA() + controlerConfigGroup.getFirstIteration());
		}
	}

	private void initHandlers(final ScoringFunctionFactory factory) {
		this.eventsToActivities = new EventsToActivities();
		this.scoringFunctionsForPopulation = new ScoringFunctionsForPopulation(plansConfigGroup, network, population, factory);
		this.eventsToActivities.setActivityHandler(this.scoringFunctionsForPopulation);
		this.eventsToLegs = new EventsToLegs(network, transitSchedule);
		this.eventsToLegs.setLegHandler(this.scoringFunctionsForPopulation);
	}

	@Override
	public void handleEvent(Event event) {
		// this is for the activity and leg related stuff ("old" scoring function)
		if ( event instanceof LinkEnterEvent ) {
			eventsToLegs.handleEvent((LinkEnterEvent) event) ;
		} else if ( event instanceof LinkLeaveEvent ) {
			eventsToLegs.handleEvent((LinkLeaveEvent) event ) ;
		} else if ( event instanceof PersonDepartureEvent ) {
			eventsToLegs.handleEvent((PersonDepartureEvent) event) ;
		} else if ( event instanceof PersonArrivalEvent ) {
			eventsToLegs.handleEvent((PersonArrivalEvent) event ) ;
		} else if ( event instanceof ActivityStartEvent ) {
			eventsToActivities.handleEvent((ActivityStartEvent) event) ;
		} else if ( event instanceof ActivityEndEvent ) {
			eventsToActivities.handleEvent( (ActivityEndEvent) event ) ;
		} else if ( event instanceof TeleportationArrivalEvent ) {
			eventsToLegs.handleEvent( (TeleportationArrivalEvent) event ) ;
		} else if ( event instanceof PersonEntersVehicleEvent ) {
			eventsToLegs.handleEvent( (PersonEntersVehicleEvent) event) ;
		} else if ( event instanceof VehicleArrivesAtFacilityEvent ) {
			eventsToLegs.handleEvent( (VehicleArrivesAtFacilityEvent) event ) ;
		} else if ( event instanceof TransitDriverStartsEvent ) {
			eventsToLegs.handleEvent( (TransitDriverStartsEvent) event ) ;
		} 

		// this is for the stuff that is directly based on events.
		// note that this passes on _all_ person events, even those already passed above.
		// for the time being, not all PersonEvents may "implement HasPersonId".
		// link enter/leave events are NOT passed on, for performance reasons.
		// kai/dominik, dec'12
		if ( event instanceof HasPersonId ) {
			ScoringFunction sf = getScoringFunctionForAgent( ((HasPersonId)event).getPersonId());
			if (sf != null) {
				if ( event instanceof PersonStuckEvent ) {
					sf.agentStuck( event.getTime() ) ;
				} else if ( event instanceof PersonMoneyEvent ) {
					sf.addMoney( ((PersonMoneyEvent)event).getAmount() ) ;
				} else {
					sf.handleEvent( event ) ;
				}
			}
		}
	}


	/**
	 * Finishes the calculation of the plans' scores and assigns the new scores
	 * to the plans.
	 * I think this should be split into two methods: One can want to close the ScoringFunctions to look
	 * at scores WITHOUT wanting something to be written into Plans.
	 * Actually, I think the two belong in different classes. michaz '12
	 * <p/>
	 * yy Absolutely.  kai, oct'12
	 */
	public void finish() {
		eventsToActivities.finish();	
		scoringFunctionsForPopulation.finishScoringFunctions();
		assignNewScores();
		finished = true;
	}
	
	private Map<Plan,Integer> msaContributions = new HashMap<>() ;
	private void assignNewScores() {
		log.info("it: " + this.iteration + " msaStart: " + this.scoreMSAstartsAtIteration );

		for (Person person : population.getPersons().values()) {
			ScoringFunction sf = scoringFunctionsForPopulation.getScoringFunctionForAgent(person.getId());
			double score = sf.getScore();
			Plan plan = person.getSelectedPlan();
			Double oldScore = plan.getScore();
			if (oldScore == null) {
				plan.setScore(score);
				if ( plan.getScore().isNaN() ) {
					log.warn("score is NaN; plan:" + plan.toString() );
				}
			} else {
				if ( this.scoreMSAstartsAtIteration == null || this.iteration < this.scoreMSAstartsAtIteration ) {
					final double newScore = this.learningRate * score + (1 - this.learningRate) * oldScore;
					if ( log.isTraceEnabled() ) { 
						log.trace( " lrn: " + this.learningRate + " oldScore: " + oldScore + " simScore: " + score + " newScore: " + newScore );
					}
					plan.setScore(newScore);
					if ( plan.getScore().isNaN() ) {
						log.warn("score is NaN; plan:" + plan.toString()+" with lrn: " + this.learningRate + " oldScore: " + oldScore + " simScore: " + score + " newScore: " + newScore );
					}
				} else {
//					double alpha = 1./(this.iteration - this.scoreMSAstartsAtIteration + 1) ;
//					alpha *= scenario.getConfig().strategy().getMaxAgentPlanMemorySize() ; //(**)
//					if ( alpha>1 ) {
//						alpha = 1. ;
//					}

					Integer msaContribs = this.msaContributions.get(plan) ;
					if ( msaContribs==null ) {
						msaContribs = 0 ;
					}
					this.msaContributions.put(plan,msaContribs+1) ;
					double alpha = 1./(msaContribs+1) ;

					final double newScore = alpha * score + (1.-alpha) * oldScore;
					if ( log.isTraceEnabled() ) {
						log.trace( " alpha: " + alpha + " oldScore: " + oldScore + " simScore: " + score + " newScore: " + newScore );
					}
					plan.setScore( newScore ) ;
					if ( plan.getScore().isNaN() ) {
						log.warn("score is NaN; plan:" + plan.toString() );
					}
					/*
					// the above is some variant of MSA (method of successive
					// averages). It is not the same as MSA since
					// a plan is typically not scored in every iteration.
					// However, plans are called with rates, for example
					// only every 10th iteration. Yet, something like 1/(10x)
					// still diverges in the same way as 1/x
					// when integrated, so MSA should still converge to the
					// correct result. kai, oct'12
					// The above argument may be theoretically correct.  But something 9/10*old+1/10*new is too slow in practice.  Now
					// multiplying with number of plans (**) in hope that it is better.  (Where is the theory department?) kai, nov'13
					 * Looks to me like this is now truly MSA. kai, apr'15 
					// yyyy this has never been tested with scenarios :-(  .  At least there is a test case.  kai, oct'12
					// (In the meantime, I have used it in certain of my own 1% runs, e.g. Ivory Coast.)
					 */
				}
			}

			this.scoreSum += score;
			this.scoreCount++;
		}
	}

	

	/**
	 * Returns the actual average plans' score before it was assigned to the
	 * plan and possibility mixed with old scores (learningrate).
	 *
	 * @return the average score of the plans before mixing with the old scores
	 *         (learningrate)
	 */
	public double getAveragePlanPerformance() {
		if (this.scoreSum == 0)
			return Double.NaN;
		else
			return (this.scoreSum / this.scoreCount);
	}

	/**
	 * Returns the score of a single agent. This method only returns useful
	 * values if the method {@link #finish() } was called before. description
	 *
	 * @param agentId
	 *            The id of the agent the score is requested for.
	 * @return The score of the specified agent.
	 */
	public Double getAgentScore(final Id<Person> agentId) {
		if (!finished) {
			throw new IllegalStateException("Must call finish first.");
		}
		ScoringFunction scoringFunction = getScoringFunctionForAgent(agentId);
		if (scoringFunction == null)
			return null;
		return scoringFunction.getScore();
	}

	@Override
	public void reset(final int iteration) {
		this.eventsToActivities.reset(iteration);
		this.eventsToLegs.reset(iteration);
		initHandlers(scoringFunctionFactory);
		finished = false;
		this.iteration = iteration ;
		// ("reset" is called just before the mobsim starts, so it probably has the correct iteration number for our purposes) 
		
//		this.msaContributions.clear() ;
	}

	public ScoringFunction getScoringFunctionForAgent(Id<Person> agentId) {
		return scoringFunctionsForPopulation.getScoringFunctionForAgent(agentId);
	}

	public Map<Id<Person>, Plan> getAgentRecords() {
		return scoringFunctionsForPopulation.getAgentRecords();
	}

	public void writeExperiencedPlans(String iterationFilename) {
		scoringFunctionsForPopulation.writeExperiencedPlans(iterationFilename);
	}

}
