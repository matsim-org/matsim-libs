/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.contrib.freight.controler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.carrier.Tour.TourActivity;
import org.matsim.contrib.freight.carrier.Tour.TourElement;
import org.matsim.contrib.freight.events.FreightEventCreator;
import org.matsim.contrib.freight.utils.FreightUtils;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

/**
 * This keeps track of the carrier during simulation.
 * 
 * @author mzilske, sschroeder
 *
 */
final class CarrierAgent implements Identifiable<Carrier>
{
	private static final Logger log = LogManager.getLogger( CarrierAgent.class );

	private final Id<Carrier> id;

	private final Carrier carrier;

	private final Collection<Id<Person>> driverIds = new ArrayList<>();

	private int nextId = 0;

	private final Map<Id<Person>, CarrierDriverAgent> carrierDriverAgents = new HashMap<>();

	private final ScoringFunction scoringFunction;
	private final EventsManager events;
	private final Collection<FreightEventCreator> freightEventCreators;

	CarrierAgent( Carrier carrier, ScoringFunction carrierScoringFunction, EventsManager events, Collection<FreightEventCreator> freightEventCreators) {
		this.carrier = carrier;
		this.id = carrier.getId();
		this.scoringFunction = carrierScoringFunction;
		this.events = events;
		this.freightEventCreators = freightEventCreators;
	}

	@Override public Id<Carrier> getId() {
		return id;
	}

	/**
	 * Returns a list of plans created on the basis of the carrier's plan.
	 * 
	 * <p>A carrier plan consists usually of many tours (activity chains). Each plan in the returned list represents a carrier tour.
	 *  
	 * @return list of plans
	 * @see Plan, CarrierPlan
	 */
	List<Plan> createFreightDriverPlans() {
		clear();
		System.out.flush();
		System.err.flush() ;
		if (carrier.getSelectedPlan() == null) {
			return Collections.emptyList();
		}
		List<Plan> routes = new ArrayList<>();
		for (ScheduledTour scheduledTour : carrier.getSelectedPlan().getScheduledTours()) {
			// (go through scheduled tours of selected plan:)

			Id<Person> driverId = createDriverId(scheduledTour.getVehicle());
			CarrierVehicle carrierVehicle = scheduledTour.getVehicle();
			Person driverPerson = createDriverPerson(driverId);
			Vehicle vehicle = createVehicle(driverPerson,carrierVehicle);
			CarrierDriverAgent carrierDriverAgent = new CarrierDriverAgent(driverId, scheduledTour, scoringFunction, carrier, events, freightEventCreators);
			Plan plan = PopulationUtils.createPlan();
			Activity startActivity = PopulationUtils.createActivityFromLinkId(FreightConstants.START, scheduledTour.getVehicle().getLinkId() );
			startActivity.setEndTime(scheduledTour.getDeparture());
			plan.addActivity(startActivity);
			for (TourElement tourElement : scheduledTour.getTour().getTourElements()) {				
				if ( tourElement instanceof Tour.Leg tourLeg ) {
					Route route = tourLeg.getRoute();

					if(route == null) throw new IllegalStateException("missing route for carrier " + this.getId());
					// yy At least in EquilWithoutCarrierWithPassIT and routes removed, it runs through even without the above
					// line ... but it looks like the simulation is not generating kilometers.  Presumably, there is no equivalent
					// to "prepareForSim" for carriers.  Did not check any further.  kai, jul'22

					//this returns TransportMode.car if the attribute is null
					Leg leg = PopulationUtils.createLeg(CarrierUtils.getCarrierMode(carrier));

					//TODO we might need to set the route to null if the the mode is a drt mode
					leg.setRoute(route);
					leg.setDepartureTime(tourLeg.getExpectedDepartureTime());

					leg.setTravelTime(tourLeg.getExpectedTransportTime());
					leg.setTravelTime( tourLeg.getExpectedDepartureTime() + tourLeg.getExpectedTransportTime() - leg.getDepartureTime().seconds());
					// yy why is it setting travel time twice?  kai, jul'22

					plan.addLeg(leg);
				} else if (tourElement instanceof TourActivity) {
					TourActivity act = (TourActivity) tourElement;
					Activity tourElementActivity = PopulationUtils.createActivityFromLinkId(act.getActivityType(), act.getLocation());					
					double duration = act.getDuration() ;
					tourElementActivity.setMaximumDuration(duration); // "maximum" has become a bit of a misnomer ...
					plan.addActivity(tourElementActivity);
				}
			}
			Activity endActivity = PopulationUtils.createActivityFromLinkId(FreightConstants.END, scheduledTour.getVehicle().getLinkId() );
			plan.addActivity(endActivity);
			driverPerson.addPlan(plan);
			plan.setPerson(driverPerson);
			FreightUtils.putVehicle( plan, vehicle );
			routes.add(plan);
			carrierDriverAgents.put(driverId, carrierDriverAgent);
		}
		return routes;
	}

	private Vehicle createVehicle(Person driverPerson, CarrierVehicle carrierVehicle) {
		// yyyyyy This here actually ignores the carrierVehicleId.  kai, jul'22
		// why not just use carrierVehicle directly?  kai, jul'22

		Gbl.assertNotNull(driverPerson);
		Gbl.assertNotNull( carrierVehicle.getType() );
		return VehicleUtils.getFactory().createVehicle(Id.create(driverPerson.getId(), Vehicle.class), carrierVehicle.getType() );
	}

	private void clear() {
		carrierDriverAgents.clear();
		driverIds.clear();
		nextId = 0;
	}

	Collection<Id<Person>> getDriverIds() {
		return Collections.unmodifiableCollection(driverIds);
	}

	private Person createDriverPerson(Id<Person> driverId) {
		return PopulationUtils.getFactory().createPerson( driverId );
	}

	private Id<Person> createDriverId(CarrierVehicle carrierVehicle) {
		Id<Person> id = Id.create("freight_" + carrier.getId() + "_veh_" + carrierVehicle.getId() + "_" + nextId, Person.class );
		driverIds.add(id);
		++nextId;
		return id;
	}

	void scoreSelectedPlan() {
		if (carrier.getSelectedPlan() == null) {
			return;
		}
		scoringFunction.finish();
		final double score = scoringFunction.getScore();
		log.warn("score=" + score);
		carrier.getSelectedPlan().setScore( score );
	}
	void handleEvent(Event event, Id<Person> driverId) {
		// the event comes to here from CarrierAgentTracker only for those drivers that belong to this carrier.  The driver IDs are also
		// passed on "as a service", which means that here we can distribute the events to the drivers.
		getDriver( driverId ).handleAnEvent( event);
	}
	CarrierDriverAgent getDriver(Id<Person> driverId){
		return carrierDriverAgents.get(driverId);
	}
}
