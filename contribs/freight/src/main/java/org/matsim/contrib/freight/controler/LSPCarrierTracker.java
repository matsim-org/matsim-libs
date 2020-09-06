package org.matsim.contrib.freight.controler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.events.eventsCreator.LSPEventCreator;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.contrib.freight.controler.CarrierAgent.CarrierDriverAgent;



public final class LSPCarrierTracker implements ActivityStartEventHandler, ActivityEndEventHandler, PersonDepartureEventHandler, PersonArrivalEventHandler,
		       LinkEnterEventHandler,
 LinkLeaveEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler, PersonEntersVehicleEventHandler,  PersonLeavesVehicleEventHandler{

	
	private final LSPFreightControlerListener listener;
	
	private final Carriers carriers;

	private final Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler();

	private final Collection<CarrierAgent> carrierResourceAgents = new ArrayList<CarrierAgent>();
	
	private final Map<Id<Person>, CarrierAgent> driverAgentMap = new HashMap<Id<Person>, CarrierAgent>();

	private final Collection<LSPEventCreator> LSPEventCreators;
	
	public LSPCarrierTracker( Carriers carriers, Network network, LSPFreightControlerListener listener, Collection<LSPEventCreator> creators ) {
		this.carriers = carriers;
		this.LSPEventCreators = creators;
		createCarrierResourceAgents();
		this.listener = listener;
	}

	private void createCarrierResourceAgents() {
		for (Carrier carrier : carriers.getCarriers().values()) {
			CarrierAgent carrierResourceAgent = new CarrierAgent(this, carrier, delegate);
			carrierResourceAgents.add(carrierResourceAgent);
		}
	}

	/**
	 * Returns the entire set of selected carrier plans.
	 * 
	 * @return collection of plans
	 * @see Plan , CarrierPlan
	 */
	public Collection<Plan> createPlans() {
		List<Plan> vehicleRoutes = new ArrayList<>();
		for ( CarrierAgent carrierResourceAgent : carrierResourceAgents) {
			List<Plan> plansForCarrier = carrierResourceAgent.createFreightDriverPlans();
			vehicleRoutes.addAll(plansForCarrier);
		}
		return vehicleRoutes;
	}
	
	@Override
	public void reset(int iteration) {
		delegate.reset(iteration);
	}

	private void processEvent(Event event) {
		listener.processEvent(event);
	}
	
	public void notifyEventHappened(Event event, Carrier carrier, Activity activity, ScheduledTour scheduledTour, Id<Person> driverId, int activityCounter) {
		for(LSPEventCreator LSPEventCreator : LSPEventCreators) {
			Event customEvent = LSPEventCreator.createEvent(event, carrier, activity, scheduledTour, driverId, activityCounter);
			if(customEvent != null) {
				processEvent(customEvent);
			}
		}
	}
		
	@Override
	public void handleEvent(ActivityEndEvent event) {
		CarrierAgent carrierResourceAgent = getCarrierResourceAgent(event.getPersonId() );
		if(carrierResourceAgent == null) return;
		carrierResourceAgent.handleEvent(event);
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		CarrierAgent carrierResourceAgent = getCarrierResourceAgent(delegate.getDriverOfVehicle(event.getVehicleId() ) );
		if(carrierResourceAgent == null) return;
		carrierResourceAgent.handleEvent(event);
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		CarrierAgent carrierResourceAgent = getCarrierResourceAgent(event.getPersonId() );
		if(carrierResourceAgent == null) return;
		carrierResourceAgent.handleEvent(event);
	}


	@Override
	public void handleEvent(PersonArrivalEvent event) {
		CarrierAgent carrierResourceAgent = getCarrierResourceAgent(event.getPersonId() );
		if(carrierResourceAgent == null) return;
		carrierResourceAgent.handleEvent(event);
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		CarrierAgent carrierResourceAgent = getCarrierResourceAgent(event.getPersonId() );
		if(carrierResourceAgent == null) return;
		carrierResourceAgent.handleEvent(event);
	}

	private CarrierAgent getCarrierResourceAgent( Id<Person> driverId ) {
		if(driverAgentMap.containsKey(driverId)){
			return driverAgentMap.get(driverId);
		}
		for( CarrierAgent ca : carrierResourceAgents){
			if(ca.getDriverIds().contains(driverId)){
				driverAgentMap.put(driverId, ca);
				return ca;
			}
		}
		return null;	
	}
	
	public CarrierDriverAgent getDriver(Id<Person> driverId){
		CarrierAgent carrierAgent = getCarrierResourceAgent(driverId );
		if(carrierAgent == null) throw new IllegalStateException("missing carrier agent. cannot find carrierAgent to driver " + driverId);
		return carrierAgent.getDriver(driverId);
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		delegate.handleEvent(event);
		CarrierAgent carrierResourceAgent = getCarrierResourceAgent(event.getPersonId() );
		if(carrierResourceAgent == null) return;
		carrierResourceAgent.handleEvent(event);
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		delegate.handleEvent(event);
		CarrierAgent carrierResourceAgent = getCarrierResourceAgent(event.getPersonId() );
		if(carrierResourceAgent == null) return;
		carrierResourceAgent.handleEvent(event);
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		CarrierAgent carrierResourceAgent = getCarrierResourceAgent(delegate.getDriverOfVehicle(event.getVehicleId() ) );
		if(carrierResourceAgent == null) return;
		carrierResourceAgent.handleEvent(event);	
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		CarrierAgent carrierResourceAgent = getCarrierResourceAgent(event.getPersonId() );
		if(carrierResourceAgent == null) return;
		carrierResourceAgent.handleEvent(event);
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		CarrierAgent carrierResourceAgent = getCarrierResourceAgent(event.getPersonId() );
		if(carrierResourceAgent == null) return;
		carrierResourceAgent.handleEvent(event);
	}
	
}
