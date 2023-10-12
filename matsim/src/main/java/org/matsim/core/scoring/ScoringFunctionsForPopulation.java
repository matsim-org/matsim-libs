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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.HasPersonId;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.PersonScoreEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.controler.ControlerListenerManager;
import org.matsim.core.controler.listener.BeforeMobsimListener;
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
 final class ScoringFunctionsForPopulation implements BasicEventHandler {

	private final Population population;
	private final ScoringFunctionFactory scoringFunctionFactory;

	private final EventsToLegs legsDelegate;
	private final EventsToActivities actsDelegate;

	private final IdMap<Person, ScoringFunction> agentScorers = new IdMap<>(Person.class);
	private final IdMap<Person, TDoubleCollection> partialScores = new IdMap<>(Person.class);
	private final AtomicReference<Throwable> exception = new AtomicReference<>();
	private final IdMap<Person, Plan> tripRecords = new IdMap<>(Person.class);

	private final Vehicle2DriverEventHandler vehicles2Drivers = new Vehicle2DriverEventHandler();

	@Inject
	ScoringFunctionsForPopulation(ControlerListenerManager controlerListenerManager, EventsManager eventsManager, EventsToActivities eventsToActivities, EventsToLegs eventsToLegs,
						 Population population, ScoringFunctionFactory scoringFunctionFactory, Config config) {
		ControllerConfigGroup controllerConfigGroup = config.controller();

		if (controllerConfigGroup.getEventTypeToCreateScoringFunctions() == ControllerConfigGroup.EventTypeToCreateScoringFunctions.IterationStarts) {
			controlerListenerManager.addControlerListener((IterationStartsListener) event -> init());
		} else if (controllerConfigGroup.getEventTypeToCreateScoringFunctions() == ControllerConfigGroup.EventTypeToCreateScoringFunctions.BeforeMobsim) {
			controlerListenerManager.addControlerListener((BeforeMobsimListener) event -> init());
		} else {
			throw new RuntimeException("Unknown approach when to create the scoring functions for population. Aborting...");
		}

		this.population = population;
		this.legsDelegate = eventsToLegs;
		this.actsDelegate = eventsToActivities;
		this.scoringFunctionFactory = scoringFunctionFactory;

		eventsManager.addHandler(this);
		eventsToActivities.addActivityHandler(this::handleActivity);
		eventsToLegs.addLegHandler(this::handleLeg);
	}

	private void init() {
		for (Person person : this.population.getPersons().values()) {
			this.agentScorers.put(person.getId(), this.scoringFunctionFactory.createNewScoringFunction(person ) );
			this.partialScores.put(person.getId(), new TDoubleArrayList());
			this.tripRecords.put(person.getId(), PopulationUtils.createPlan());
		}
	}

	@Override
	public void handleEvent(Event o) {
		// this is for the stuff that is directly based on events. note that this passes on _all_ person events, even those which are
		// aggregated into legs and activities. for the time being, not all PersonEvents may "implement HasPersonId". link enter/leave events
		// are NOT passed on, for performance reasons. kai/dominik, dec'12
		if (o instanceof HasPersonId) {
			ScoringFunction scoringFunction = getScoringFunctionForAgent(((HasPersonId) o).getPersonId());
			if (scoringFunction != null) {
				if (o instanceof PersonStuckEvent) {
					scoringFunction.agentStuck(o.getTime());
				} else if (o instanceof PersonMoneyEvent) {
					scoringFunction.addMoney(((PersonMoneyEvent) o).getAmount());
					// yy looking at this, I am a bit skeptic if it truly makes sense to not pass this additionally into the general events handling function below.
					// A use case might be different utilities of money by money transaction type (e.g. toll, fare, reimbursement, ...).  kai, mar'17
				} else if (o instanceof PersonScoreEvent) {
					scoringFunction.addScore(((PersonScoreEvent) o).getAmount());
				}
				scoringFunction.handleEvent(o);
				// passing this on in any case, see comment above.  kai, mar'17
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
		if (o instanceof LinkEnterEvent) {
			Id<Vehicle> vehicleId = ((LinkEnterEvent)o).getVehicleId();
			Id<Person> driverId = this.vehicles2Drivers.getDriverOfVehicle(vehicleId);
			ScoringFunction scoringFunction = getScoringFunctionForAgent( driverId );
			// (this will NOT do the scoring function lookup twice since LinkEnterEvent is not an instance of HasPersonId.  kai, mar'17)
			if (scoringFunction != null) {
				scoringFunction.handleEvent(o);
			}
		}

		/* Now also handle events for eventsToLegs and eventsToActivities.
		 * This class deliberately only implements BasicEventHandler and not the individual event handlers required
		 * by EventsToLegs and EventsToActivities to better control the order in which events are passed to scoring
		 * functions. By handling the delegation here *after* having the events passed to scoringFunction.handleEvent()
		 * makes sure that the corresponding event was already seen by a scoring function when the call to handleActivity(),
		 * handleLeg() or handleTrip() is done.
		 */
		if (o instanceof ActivityStartEvent) this.handleActivityStart((ActivityStartEvent) o);
		if (o instanceof ActivityEndEvent) this.actsDelegate.handleEvent((ActivityEndEvent) o);

		if (o instanceof PersonDepartureEvent) this.legsDelegate.handleEvent((PersonDepartureEvent) o);
		if (o instanceof PersonArrivalEvent) this.legsDelegate.handleEvent((PersonArrivalEvent) o);
		if (o instanceof LinkEnterEvent) this.legsDelegate.handleEvent((LinkEnterEvent) o);
		if (o instanceof TeleportationArrivalEvent) this.legsDelegate.handleEvent((TeleportationArrivalEvent) o);
		if (o instanceof TransitDriverStartsEvent) this.legsDelegate.handleEvent((TransitDriverStartsEvent) o);
		if (o instanceof PersonEntersVehicleEvent) this.legsDelegate.handleEvent((PersonEntersVehicleEvent) o);
		if (o instanceof VehicleArrivesAtFacilityEvent) this.legsDelegate.handleEvent((VehicleArrivesAtFacilityEvent) o);
		if (o instanceof VehicleEntersTrafficEvent) this.legsDelegate.handleEvent((VehicleEntersTrafficEvent) o);
		if (o instanceof VehicleLeavesTrafficEvent) this.legsDelegate.handleEvent((VehicleLeavesTrafficEvent) o);
	}

	private void handleActivityStart(ActivityStartEvent event) {
		this.actsDelegate.handleEvent(event);
		if (!StageActivityTypeIdentifier.isStageActivity( event.getActType() ) ) {
			this.callTripScoring(event);
		}
	}

	private void callTripScoring(ActivityStartEvent event) {
		Plan plan = this.tripRecords.get(event.getPersonId()); // as container for trip
		if (plan != null) {
			// we are at a real activity, which is not the first one we see for this agent.  output the trip ...
			Activity activity = PopulationUtils.createActivityFromLinkId(event.getActType(), event.getLinkId());
			activity.setStartTime(event.getTime());
			plan.addActivity(activity);
			final List<Trip> trips = TripStructureUtils.getTrips(plan);
			// yyyyyy should in principle only return one trip.  There are, however, situations where it returns two trips, in particular
			// in conjunction with the minibus raptor.  Possibly something that has to do with not alternating between acts and legs.
			// (To make matters worse, it passes on my local machine, but fails in jenkins.  Possibly, the byte buffer memory management
			// in the minibus raptor implementation has issues--???) kai, sep'18

			ScoringFunction scoringFunction = ScoringFunctionsForPopulation.this.getScoringFunctionForAgent(event.getPersonId());
			for (Trip trip : trips) {
				if (trip != null) {
					scoringFunction.handleTrip(trip);
				}
			}

			// ... and clean out the intermediate plan (which will remain in tripRecords).
			plan.getPlanElements().clear();
		}
	}

	void handleLeg(PersonExperiencedLeg o) {
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

	void handleActivity(PersonExperiencedActivity o) {
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
	ScoringFunction getScoringFunctionForAgent(final Id<Person> agentId) {
		return this.agentScorers.get(agentId);
	}

	void finishScoringFunctions() {
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

	void writePartialScores(String iterationFilename) {
		try ( BufferedWriter out = IOUtils.getBufferedWriter(iterationFilename) ) {
			for (Entry<Id<Person>, TDoubleCollection> entry : this.partialScores.entrySet()) {
				out.write(entry.getKey().toString());
				TDoubleIterator iterator = entry.getValue().iterator();
				while (iterator.hasNext()) {
					out.write('\t');
					out.write(String.valueOf(iterator.next()));
				}
				out.write(IOUtils.NATIVE_NEWLINE);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void reset(int iteration) {
		this.legsDelegate.reset(iteration);
		this.actsDelegate.reset(iteration);
	}

}
