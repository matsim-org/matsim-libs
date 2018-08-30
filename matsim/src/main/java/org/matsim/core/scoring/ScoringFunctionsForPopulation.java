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

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
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
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.PtConstants;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;

import gnu.trove.TDoubleCollection;
import gnu.trove.iterator.TDoubleIterator;
import gnu.trove.list.array.TDoubleArrayList;

import static org.matsim.core.router.TripStructureUtils.*;

/**
 * This class helps EventsToScore by keeping ScoringFunctions for the entire Population - one per Person -, and dispatching Activities
 * and Legs to the ScoringFunctions. It also gives out the ScoringFunctions, so they can be given other events by EventsToScore.
 * It is not independently useful. Please do not make public.
 * 
 * @author michaz
 *
 */
 final class ScoringFunctionsForPopulation implements BasicEventHandler, EventsToLegs.LegHandler, EventsToActivities.ActivityHandler {
	// yyyyyy there is currently only one place outside package where this is used, and I think it
	// can be changed there.  kai, sep'17
	// I just removed that.  kai, apr'18
	
	@SuppressWarnings("unused")
	private final static Logger log = Logger.getLogger(ScoringFunctionsForPopulation.class);
	private final Population population;
	private final ScoringFunctionFactory scoringFunctionFactory;
	
	private final Map<Id<Person>, Plan> tripRecords = new HashMap<>() ;
	private final StageActivityTypes stageActivityTypes;

	/*
	 * Replaced TreeMaps with (Linked)HashMaps since they should perform much better. For 'partialScores'
	 * a LinkedHashMap is used to ensure that agents are written in a deterministic order to the output files.
	 *
	 * Replaced List with TDoubleCollection (TDoubleArrayList) in the partialScores map. This collection allows
	 * storing primitive objects, i.e. its double entries don't have to be wrapped into Double objects which
	 * should be faster and reduce the memory overhead.
	 *
	 * cdobler, nov'15
	 */
	private final Map<Id<Person>, ScoringFunction> agentScorers = new HashMap<>();
	private final Map<Id<Person>, TDoubleCollection> partialScores = new LinkedHashMap<>();
	private final AtomicReference<Throwable> exception = new AtomicReference<>();
	
//	/**
//	 * For something like the bicycle scoring, we need to know individual links at the level of the scoring function.  This is a first sketch how this could be implemented.
//	 * kai, mar'17
//	 */
//	private boolean passLinkEventsToPerson = false;
	
	private Vehicle2DriverEventHandler vehicles2Drivers = new Vehicle2DriverEventHandler();

	@Inject
	ScoringFunctionsForPopulation( ControlerListenerManager controlerListenerManager, EventsManager eventsManager, EventsToActivities eventsToActivities, EventsToLegs eventsToLegs,
						 Population population, ScoringFunctionFactory scoringFunctionFactory, TripRouter tripRouter ) {
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
//		if ( passLinkEventsToPerson ) {
			eventsManager.addHandler(vehicles2Drivers);
//		}
//		stageActivityTypes = tripRouter.getStageActivityTypes() ;
		stageActivityTypes = new StageActivityTypesImpl( new String [] {PtConstants.TRANSIT_ACTIVITY_TYPE} ) ;
	}

	private void init() {
		for (Person person : population.getPersons().values()) {
			ScoringFunction data = scoringFunctionFactory.createNewScoringFunction(person);
			this.agentScorers.put(person.getId(), data);
			this.partialScores.put(person.getId(), new TDoubleArrayList());
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
//		if ( passLinkEventsToPerson ) {
			// Establish and end connection between driver and vehicle
			if (o instanceof VehicleEntersTrafficEvent) {
				vehicles2Drivers.handleEvent((VehicleEntersTrafficEvent) o);
			}
			if (o instanceof VehicleLeavesTrafficEvent) {
				vehicles2Drivers.handleEvent((VehicleLeavesTrafficEvent) o);
			}
			// Pass LinkEnterEvent to person scoring, required e.g. for bicycle where link attributes are observed in scoring
			if ( o instanceof LinkEnterEvent ) {
				Id<Vehicle> vehicleId = ((LinkEnterEvent)o).getVehicleId() ;
				Id<Person> driverId = vehicles2Drivers.getDriverOfVehicle(vehicleId) ;
				ScoringFunction scoringFunction = getScoringFunctionForAgent( driverId );
				// (this will NOT do the scoring function lookup twice since LinkEnterEvent is not an instance of HasPersonId.  kai, mar'17)
				if (scoringFunction != null) {
					scoringFunction.handleEvent(o) ;
				}
			}
			/*
			 * (This shouldn't really be more expensive than passing the link events to the router: here, we have a map lookup
			 * for agentId, there we have a map lookup for linkId. Should be somewhat similar in terms of average
			 * computational complexity. In BetaTravelTest, 194sec w/ "false", 193sec w/ "true". However, the experienced
			 * plans service in fact does the same thing, so we should be able to get away without having to do this twice.
			 * kai, mar'17)
			 */
//		}
	}

	@Override
	synchronized public void handleLeg(PersonExperiencedLeg o) {
		Id<Person> agentId = o.getAgentId();
		Leg leg = o.getLeg();
		ScoringFunction scoringFunction = ScoringFunctionsForPopulation.this.getScoringFunctionForAgent(agentId);
		if (scoringFunction != null) {
			scoringFunction.handleLeg(leg);
			TDoubleCollection partialScoresForAgent = partialScores.get(agentId);
			partialScoresForAgent.add(scoringFunction.getScore());
		}
		Plan plan = tripRecords.get( agentId ) ; // as container for trip
		if ( plan==null ) {
			plan = PopulationUtils.createPlan() ;
			tripRecords.put( agentId, plan ) ;
		}
		plan.addLeg( leg );
		
	}

	@Override
	synchronized public void handleActivity(PersonExperiencedActivity o) {
		Id<Person> agentId = o.getAgentId();
		Activity activity = o.getActivity();
		ScoringFunction scoringFunction = ScoringFunctionsForPopulation.this.getScoringFunctionForAgent(agentId);
		if (scoringFunction != null) {
			scoringFunction.handleActivity(activity);
			TDoubleCollection partialScoresForAgent = partialScores.get(agentId);
			partialScoresForAgent.add(scoringFunction.getScore());
		}
		Plan plan = tripRecords.get( agentId ); // as container for trip
		if ( stageActivityTypes.isStageActivity( activity.getType() ) ) {
			Gbl.assertNotNull( plan );
			plan.addActivity( activity );
		} else if ( plan != null ) {
			Gbl.assertNotNull( stageActivityTypes );
			final List<Trip> trips = TripStructureUtils.getTrips( plan, stageActivityTypes );
			
			log.warn( "trips.size=" + trips.size() ) ;
			
			log.warn( "trip=" + trips ) ;
			
			Gbl.assertIf( trips.size()==1 );
			final Trip trip = trips.get( 0 );
			Gbl.assertNotNull( trip );
			scoringFunction.handleTrip( trip );
			tripRecords.remove( agentId ) ;
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
		Throwable throwable = exception.get();
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
					out.write('\t' + String.valueOf(iterator.next()));
				}
				out.newLine();
			}
			out.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void reset(int iteration) {

	}

//	public boolean isPassLinkEventsToPerson() {
//		return passLinkEventsToPerson;
//	}

//	public void setPassLinkEventsToPerson(boolean passLinkEventsToPerson) {
//		this.passLinkEventsToPerson = passLinkEventsToPerson;
//	}
}
