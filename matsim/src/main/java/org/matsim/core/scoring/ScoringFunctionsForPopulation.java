/* *********************************************************************** *
 * project: org.matsim.*
 * ScoringFunctionsForPopulation.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

import com.google.inject.Inject;
import gnu.trove.TDoubleCollection;
import gnu.trove.iterator.TDoubleIterator;
import gnu.trove.list.array.TDoubleArrayList;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.internal.HasPersonId;
import org.matsim.core.controler.ControlerListenerManager;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.StageActivityTypeIdentifier;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

import static org.matsim.core.router.TripStructureUtils.Trip;

/**
 * This class helps EventsToScore by keeping ScoringFunctions for the entire Population - one per Person -, and dispatching Activities
 * and Legs to the ScoringFunctions. It also gives out the ScoringFunctions, so they can be given other events by EventsToScore.
 * It is not independently useful. Please do not make public.
 * 
 * @author michaz
 *
 */
 final class ScoringFunctionsForPopulation implements BasicEventHandler, EventsToLegs.LegHandler, EventsToActivities.ActivityHandler {
	// there is currently only one place outside package where this is used, and I think it
	// can be changed there.  kai, sep'17
	// I just removed that.  kai, apr'18
	
	@SuppressWarnings("unused")
	private final static Logger log = Logger.getLogger(ScoringFunctionsForPopulation.class);
	private final Population population;
	private final ScoringFunctionFactory scoringFunctionFactory;
	
	/*
	 * Replaced List with TDoubleCollection (TDoubleArrayList) in the partialScores map. This collection allows
	 * storing primitive objects, i.e. its double entries don't have to be wrapped into Double objects which
	 * should be faster and reduce the memory overhead.
	 *
	 * cdobler, nov'15
	 */
	private final IdMap<Person, ScoringFunction> agentScorers = new IdMap<>(Person.class);
	private final IdMap<Person, TDoubleCollection> partialScores = new IdMap<>(Person.class);
	private final AtomicReference<Throwable> exception = new AtomicReference<>();
	private final IdMap<Person, Plan> tripRecords = new IdMap<>(Person.class);
	
	private Vehicle2DriverEventHandler vehicles2Drivers = new Vehicle2DriverEventHandler();

	@Inject
	ScoringFunctionsForPopulation( ControlerListenerManager controlerListenerManager, EventsManager eventsManager, EventsToActivities eventsToActivities, EventsToLegs eventsToLegs,
						 Population population, ScoringFunctionFactory scoringFunctionFactory) {
		controlerListenerManager.addControlerListener(new IterationStartsListener() {
			@Override
			public void notifyIterationStarts(IterationStartsEvent event) {
				init();
			}
		});
		this.population = population;
		this.scoringFunctionFactory = scoringFunctionFactory;
		eventsManager.addHandler(this);
		eventsToActivities.addActivityHandler(this);
		eventsToLegs.addLegHandler(this);
	}

	private void init() {
		for (Person person : this.population.getPersons().values()) {
			ScoringFunction data = this.scoringFunctionFactory.createNewScoringFunction(person);
			this.agentScorers.put(person.getId(), data);
			this.partialScores.put(person.getId(), new TDoubleArrayList());
			this.tripRecords.put(person.getId(), PopulationUtils.createPlan());
		}
	}

	@Override
	synchronized public void handleEvent(Event o) {
		// this is for the stuff that is directly based on events.
		// note that this passes on _all_ person events, even those which are aggregated into legs and activities.
		// for the time being, not all PersonEvents may "implement HasPersonId".
		// link enter/leave events are NOT passed on, for performance reasons.
		// kai/dominik, dec'12
		if (o instanceof HasPersonId) {
			ScoringFunction scoringFunction = getScoringFunctionForAgent(((HasPersonId) o).getPersonId());
			if (scoringFunction != null) {
				if (o instanceof PersonStuckEvent) {
					scoringFunction.agentStuck(o.getTime());
				} else if (o instanceof PersonMoneyEvent) {
					scoringFunction.addMoney(((PersonMoneyEvent) o).getAmount());
					// yy looking at this, I am a bit skeptic if it truly makes sense to not pass this additionally into the general events handling function below.
					// A use case might be different utilities of money by money transaction type (e.g. toll, fare, reimbursement, ...).  kai, mar'17
				} 
//				else {
					scoringFunction.handleEvent(o);
					// passing this on in any case, see comment above.  kai, mar'17
//				}
			}
		}

		// Establish and end connection between driver and vehicle
		if (o instanceof VehicleEntersTrafficEvent) {
			this.vehicles2Drivers.handleEvent((VehicleEntersTrafficEvent) o);
		}
		if (o instanceof VehicleLeavesTrafficEvent) {
			this.vehicles2Drivers.handleEvent((VehicleLeavesTrafficEvent) o);
		}
		// Pass LinkEnterEvent to person scoring, required e.g. for bicycle where link attributes are observed in scoring
		/*
		 * (This shouldn't really be more expensive than passing the link events to the router: here, we have a map lookup
		 * for agentId, there we have a map lookup for linkId. Should be somewhat similar in terms of average
		 * computational complexity. In BetaTravelTest, 194sec w/ "false", 193sec w/ "true". However, the experienced
		 * plans service in fact does the same thing, so we should be able to get away without having to do this twice.
		 * kai, mar'17)
		 */
		if ( o instanceof LinkEnterEvent ) {
			Id<Vehicle> vehicleId = ((LinkEnterEvent)o).getVehicleId();
			Id<Person> driverId = this.vehicles2Drivers.getDriverOfVehicle(vehicleId);
			ScoringFunction scoringFunction = getScoringFunctionForAgent( driverId );
			// (this will NOT do the scoring function lookup twice since LinkEnterEvent is not an instance of HasPersonId.  kai, mar'17)
			if (scoringFunction != null) {
				scoringFunction.handleEvent(o);
			}
		}
	}

	@Override
	synchronized public void handleLeg(PersonExperiencedLeg o) {
		Id<Person> agentId = o.getAgentId();
		Leg leg = o.getLeg();
		ScoringFunction scoringFunction = ScoringFunctionsForPopulation.this.getScoringFunctionForAgent(agentId);
		if (scoringFunction != null) {
			scoringFunction.handleLeg(leg);
			TDoubleCollection partialScoresForAgent = this.partialScores.get(agentId);
			partialScoresForAgent.add(scoringFunction.getScore());
		}
		Plan plan = this.tripRecords.get( agentId ) ; // as container for trip
		if ( plan!=null ) {
			plan.addLeg( leg );
		}
	}

	@Override
	synchronized public void handleActivity(PersonExperiencedActivity o) {
		Id<Person> agentId = o.getAgentId();
		Activity activity = o.getActivity();
		ScoringFunction scoringFunction = ScoringFunctionsForPopulation.this.getScoringFunctionForAgent(agentId);
		if (scoringFunction != null) {
			scoringFunction.handleActivity(activity);
			TDoubleCollection partialScoresForAgent = this.partialScores.get(agentId);
			partialScoresForAgent.add(scoringFunction.getScore());
		}
		
		Plan plan = this.tripRecords.get( agentId ); // as container for trip
		if ( plan!= null ) {
			if ( !plan.getPlanElements().isEmpty() ) {
				// plan != null, meaning we already have pre-existing material
				if (StageActivityTypeIdentifier.isStageActivity( activity.getType() ) ) {
					// we are at a stage activity.  Don't do anything ; activity will be added later
				} else {
					// we are at a real activity, which is not the first one we see for this agent.  output the trip ...
					plan.addActivity( activity );
					final List<Trip> trips = TripStructureUtils.getTrips( plan );
					// yyyyyy should in principle only return one trip.  There are, however, situations where
					// it returns two trips, in particular in conjunction with the minibus raptor.  Possibly
					// something that has to do with not alternativing between acts and legs.
					// (To make matters worse, it passes on my local machine, but fails in jenkins.  Possibly,
					// the byte buffer memory management in the minibus raptor implementation has
					// issues--???)
					// kai, sep'18
					
					for ( Trip trip : trips ) {
						if ( trip != null ) {
							scoringFunction.handleTrip( trip );
						}
					}
					
					// ... and clean out the intermediate plan:
					plan.getPlanElements().clear();
				}
			}
			plan.addActivity( activity );
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
	public ScoringFunction getScoringFunctionForAgent(final Id<Person> agentId) {
		return this.agentScorers.get(agentId);
	}

	public void finishScoringFunctions() {
		// Rethrow an exception in a scoring function (user code) if there was one.
		Throwable throwable = this.exception.get();
		if (throwable != null) {
			if (throwable instanceof RuntimeException) {
				throw ((RuntimeException) throwable);
			} else {
				throw new RuntimeException(throwable);
			}
		}
		for (ScoringFunction sf : this.agentScorers.values()) {
			sf.finish();
		}
		for (Entry<Id<Person>, TDoubleCollection> entry : this.partialScores.entrySet()) {
			entry.getValue().add(this.getScoringFunctionForAgent(entry.getKey()).getScore());
		}
	}

	public void writePartialScores(String iterationFilename) {
		try ( BufferedWriter out = IOUtils.getBufferedWriter(iterationFilename) ) {
			for (Entry<Id<Person>, TDoubleCollection> entry : this.partialScores.entrySet()) {
				out.write(entry.getKey().toString());
				TDoubleIterator iterator = entry.getValue().iterator();
				while (iterator.hasNext()) {
					out.write('\t');
					out.write(String.valueOf(iterator.next()));
				}
				out.newLine();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void reset(int iteration) {

	}

}
