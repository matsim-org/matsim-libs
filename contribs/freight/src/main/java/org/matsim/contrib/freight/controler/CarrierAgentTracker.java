package org.matsim.contrib.freight.controler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.controler.CarrierAgent.CarrierDriverAgent;
import org.matsim.contrib.freight.events.eventsCreator.LSPEventCreator;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scoring.ScoringFunction;

/**
 * This keeps track of all carrierAgents during simulation.
 * 
 * @author mzilske, sschroeder
 *
 */
public class CarrierAgentTracker implements ActivityStartEventHandler, ActivityEndEventHandler, PersonDepartureEventHandler, PersonArrivalEventHandler,
						     LinkEnterEventHandler, LinkLeaveEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler,
						     PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler 
{
	// yyyy not sure if this _has_ to be public, but current LSP design makes this necessary.  kai, sep'20

	private static final Logger log = Logger.getLogger( CarrierAgentTracker.class ) ;

	private final Carriers carriers;

	private final Vehicle2DriverEventHandler vehicle2DriverEventHandler = new Vehicle2DriverEventHandler();

	private final Collection<CarrierAgent> carrierAgents = new ArrayList<>();
	
	private final Map<Id<Person>, CarrierAgent> driverAgentMap = new HashMap<>();

	private final EventsManager events;

	private Collection<LSPEventCreator> lspEventCreators;

	CarrierAgentTracker( Carriers carriers, CarrierScoringFunctionFactory carrierScoringFunctionFactory, EventsManager events ) {
		this.events = events;
		this.carriers = carriers;
		createCarrierAgents(carrierScoringFunctionFactory);
	}
	public CarrierAgentTracker( Carriers carriers, Collection<LSPEventCreator> creators, EventsManager events ) {
		// yyyy needs to be public with current setup. kai, sep'20

		this.carriers = carriers;
		this.lspEventCreators = creators;
		this.events = events;
		createCarrierAgents();

		Gbl.assertNotNull( this.lspEventCreators );
	}

	private void createCarrierAgents(CarrierScoringFunctionFactory carrierScoringFunctionFactory) {
		for (Carrier carrier : carriers.getCarriers().values()) {
			ScoringFunction carrierScoringFunction = carrierScoringFunctionFactory.createScoringFunction(carrier);
			CarrierAgent carrierAgent = new CarrierAgent( carrier, carrierScoringFunction );
			carrierAgents.add(carrierAgent);
		}
	}
	private void createCarrierAgents() {
		for (Carrier carrier : carriers.getCarriers().values()) {
			carrierAgents.add( new CarrierAgent( this, carrier ) );
		}
	}

	/**
	 * Returns the entire set of selected carrier plans.
	 * 
	 * @return collection of plans
	 * @see Plan, CarrierPlan
	 */
	public Collection<Plan> createPlans() {
		List<Plan> vehicleRoutes = new ArrayList<>();
		for (CarrierAgent carrierAgent : carrierAgents) {
			List<Plan> plansForCarrier = carrierAgent.createFreightDriverPlans();
			vehicleRoutes.addAll(plansForCarrier);
		}
		return vehicleRoutes;
	}

	/**
	 * Request all carrier agents to score their plans.
	 * 
	 */
	public void scoreSelectedPlans() {
		for (Carrier carrier : carriers.getCarriers().values()) {
			CarrierAgent agent = findCarrierAgent(carrier.getId());
			agent.scoreSelectedPlan();
		}
	}

	@Override
	public void reset(int iteration) {
		vehicle2DriverEventHandler.reset(iteration );
	}

	private CarrierAgent findCarrierAgent(Id<Carrier> id) {
		for (CarrierAgent agent : carrierAgents) {
			if (agent.getId().equals(id)) {
				return agent;
			}
		}
		return null;
	}

	void notifyEventHappened( Event event, Carrier carrier, Activity activity, ScheduledTour scheduledTour, Id<Person> driverId, int activityCounter ) {
		for( org.matsim.contrib.freight.events.eventsCreator.LSPEventCreator LSPEventCreator : lspEventCreators ) {
			Event customEvent = LSPEventCreator.createEvent(event, carrier, activity, scheduledTour, driverId, activityCounter);
			if(customEvent != null) {
				events.processEvent(customEvent);
			}
		}
	}
	@Override
	public void handleEvent(ActivityEndEvent event) {
		final Id<Person> driverId = event.getPersonId();
		CarrierAgent carrierAgent = getCarrierAgent( driverId );
		if(carrierAgent == null) return;
		carrierAgent.handleEvent(event, driverId );
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		final Id<Person> driverId = vehicle2DriverEventHandler.getDriverOfVehicle( event.getVehicleId() );
		CarrierAgent carrierAgent = getCarrierAgent( driverId );
		if(carrierAgent == null) return;
		carrierAgent.handleEvent(event, driverId );
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		final Id<Person> driverId = event.getPersonId();
		CarrierAgent carrierAgent = getCarrierAgent( driverId );
		if(carrierAgent == null) return;
		carrierAgent.handleEvent(event, driverId );
	}


	@Override
	public void handleEvent(PersonArrivalEvent event) {
		final Id<Person> driverId = event.getPersonId();
		CarrierAgent carrierAgent = getCarrierAgent( driverId );
		if(carrierAgent == null) return;
		carrierAgent.handleEvent(event, driverId );
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		final Id<Person> driverId = event.getPersonId();
		CarrierAgent carrierAgent = getCarrierAgent( driverId );
		if(carrierAgent == null) return;
		carrierAgent.handleEvent(event, driverId );
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		vehicle2DriverEventHandler.handleEvent(event );
		final Id<Person> driverId = event.getPersonId();
		CarrierAgent carrierAgent = getCarrierAgent( driverId );
		if(carrierAgent == null) return;
		carrierAgent.handleEvent(event, driverId );
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		vehicle2DriverEventHandler.handleEvent(event );
		final Id<Person> driverId = event.getPersonId();
		CarrierAgent carrierAgent = getCarrierAgent( driverId );
		if(carrierAgent == null) return;
		carrierAgent.handleEvent(event, driverId );
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		final Id<Person> driverId = vehicle2DriverEventHandler.getDriverOfVehicle( event.getVehicleId() );
		CarrierAgent carrierAgent = getCarrierAgent( driverId );
		if(carrierAgent == null) return;
		carrierAgent.handleEvent(event, driverId );
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		final Id<Person> driverId = event.getPersonId();
		CarrierAgent carrierAgent = getCarrierAgent( driverId );
		if(carrierAgent == null) return;
		carrierAgent.handleEvent(event, driverId );
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		final Id<Person> driverId = event.getPersonId();
		CarrierAgent carrierAgent = getCarrierAgent( driverId );
		if(carrierAgent == null) return;
		carrierAgent.handleEvent(event, driverId );
	}
	private CarrierAgent getCarrierAgent(Id<Person> driverId) {
		CarrierAgent carrier = driverAgentMap.get(driverId);
		if(carrier != null){
			return carrier;
		}
		for(CarrierAgent ca : carrierAgents){
			if(ca.getDriverIds().contains(driverId)){
				driverAgentMap.put(driverId, ca);
				return ca;
			}
		}
		return null;
	}

	CarrierDriverAgent getDriver(Id<Person> driverId){
		CarrierAgent carrierAgent = getCarrierAgent(driverId);
		if(carrierAgent == null) throw new IllegalStateException("missing carrier agent. cannot find carrierAgent to driver " + driverId);
		return carrierAgent.getDriver(driverId);
	}

}
