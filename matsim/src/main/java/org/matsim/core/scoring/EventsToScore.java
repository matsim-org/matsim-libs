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
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import com.google.inject.Inject;


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
	private NewScoreAssigner newScoreAssigner;
	private EventsToActivities eventsToActivities;
	private EventsToLegs eventsToLegs;
	private ScoringFunctionsForPopulation scoringFunctionsForPopulation;
	private final Population population;
	private final ScoringFunctionFactory scoringFunctionFactory;
	private final Network network;
	private @Inject(optional = true) TransitSchedule transitSchedule = null;

	private boolean finished = false;
	
	private int iteration = -1 ;



	@Inject
	EventsToScore(PlansConfigGroup plansConfigGroup, ControlerConfigGroup controlerConfigGroup, PlanCalcScoreConfigGroup planCalcScoreConfigGroup, ScoringFunctionFactory factory, EventsManager eventsManager, Population population, Network network) {
		this.population = population;
		this.plansConfigGroup = plansConfigGroup;
		this.scoringFunctionFactory = factory;
		this.network = network;
		initHandlers(scoringFunctionFactory);
		this.newScoreAssigner = new NewScoreAssigner(planCalcScoreConfigGroup, controlerConfigGroup);
		// With the Inject-Constructor, this class adds itself as an EventHandler.
		eventsManager.addHandler(this);
	}

	private EventsToScore(final Scenario scenario, final ScoringFunctionFactory scoringFunctionFactory, NewScoreAssigner newScoreAssigner) {
		this.population = scenario.getPopulation();
		this.network = scenario.getNetwork();
		this.plansConfigGroup = scenario.getConfig().plans();
		if (scenario.getConfig().transit().isUseTransit()) {
			this.transitSchedule = scenario.getTransitSchedule();
		}
		this.scoringFunctionFactory = scoringFunctionFactory;
		this.newScoreAssigner = newScoreAssigner;
		initHandlers(scoringFunctionFactory);
	}

	public static EventsToScore createWithScoreUpdating(final Scenario scenario, final ScoringFunctionFactory scoringFunctionFactory) {
		return new EventsToScore(scenario, scoringFunctionFactory, new NewScoreAssigner(scenario.getConfig().planCalcScore(), scenario.getConfig().controler()));
	}

	public static EventsToScore createWithoutScoreUpdating(Scenario scenario, ScoringFunctionFactory scoringFunctionFactory) {
		return new EventsToScore(scenario, scoringFunctionFactory, null);
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
		} else if ( event instanceof VehicleEntersTrafficEvent ) {
			eventsToLegs.handleEvent((VehicleEntersTrafficEvent) event ) ; 
		} else if ( event instanceof VehicleLeavesTrafficEvent ) {
			eventsToLegs.handleEvent((VehicleLeavesTrafficEvent) event ) ; 
		}
		scoringFunctionsForPopulation.handleEvent(event);
	}


	/**
	 * Finishes the calculation of the plans' scores and assigns the new scores
	 * to the plans if desired.
	 */
	public void finish() {
		eventsToActivities.finish();	
		scoringFunctionsForPopulation.finishScoringFunctions();
		if (newScoreAssigner != null) {
			newScoreAssigner.assignNewScores(this.iteration, scoringFunctionsForPopulation, population);
		}
		finished = true;
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
		ScoringFunction scoringFunction = scoringFunctionsForPopulation.getScoringFunctionForAgent(agentId);
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

	public Map<Id<Person>, Plan> getAgentRecords() {
		return scoringFunctionsForPopulation.getAgentRecords();
	}

	public void writeExperiencedPlans(String iterationFilename) {
		scoringFunctionsForPopulation.writeExperiencedPlans(iterationFilename);
	}

}
