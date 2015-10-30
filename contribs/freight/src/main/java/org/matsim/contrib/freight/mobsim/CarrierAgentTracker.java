package org.matsim.contrib.freight.mobsim;

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
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.events.ShipmentDeliveredEvent;
import org.matsim.contrib.freight.events.ShipmentPickedUpEvent;
import org.matsim.contrib.freight.mobsim.CarrierAgent.CarrierDriverAgent;
import org.matsim.contrib.freight.scoring.CarrierScoringFunctionFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scoring.ScoringFunction;

/**
 * This keeps track of all carrierAgents during simulation.
 * 
 * @author mzilske, sschroeder
 *
 */
public class CarrierAgentTracker implements ActivityStartEventHandler, ActivityEndEventHandler, PersonDepartureEventHandler, PersonArrivalEventHandler,  LinkEnterEventHandler {

	private final Carriers carriers;

	private final EventsManager eventsManager;

	private final Collection<CarrierAgent> carrierAgents = new ArrayList<CarrierAgent>();
	
	private Map<Id,CarrierAgent> driverAgentMap = new HashMap<Id, CarrierAgent>();

	public CarrierAgentTracker(Carriers carriers, Network network, CarrierScoringFunctionFactory carrierScoringFunctionFactory) {
		this.carriers = carriers;
		createCarrierAgents(carrierScoringFunctionFactory);
		eventsManager = EventsUtils.createEventsManager();
	}

	private void createCarrierAgents(CarrierScoringFunctionFactory carrierScoringFunctionFactory) {
		for (Carrier carrier : carriers.getCarriers().values()) {
			ScoringFunction carrierScoringFunction = carrierScoringFunctionFactory.createScoringFunction(carrier);
			CarrierAgent carrierAgent = new CarrierAgent(this, carrier, carrierScoringFunction);
			carrierAgents.add(carrierAgent);
		}
	}

	public EventsManager getEventsManager() {
		return eventsManager;
	}

	/**
	 * Returns the entire set of selected carrier plans.
	 * 
	 * @return collection of plans
	 * @see Plan, CarrierPlan
	 */
	Collection<MobSimVehicleRoute> createPlans() {
		List<MobSimVehicleRoute> vehicleRoutes = new ArrayList<MobSimVehicleRoute>();
		for (CarrierAgent carrierAgent : carrierAgents) {
			List<MobSimVehicleRoute> plansForCarrier = carrierAgent.createFreightDriverPlans();
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
		// TODO Auto-generated method stub

	}

	private CarrierAgent findCarrierAgent(Id id) {
		for (CarrierAgent agent : carrierAgents) {
			if (agent.getId().equals(id)) {
				return agent;
			}
		}
		return null;
	}

	private void processEvent(Event event) {
		eventsManager.processEvent(event);
	}

	/**
	 * Informs the world that a shipment has been picked up.
	 * 
	 * <p>Is called by carrierAgent in charge of picking up shipments. It throws an ShipmentPickedupEvent which can be listened to
	 * with an ShipmentPickedUpListener.
	 * 
	 * @param carrierId
	 * @param driverId
	 * @param shipment
	 * @param time
	 * @see ShipmentPickedUpEvent, ShipmentPickedUpEventHandler
	 */
	public void notifyPickedUp(Id carrierId, Id driverId, CarrierShipment shipment, double time) {
		processEvent(new ShipmentPickedUpEvent(carrierId, driverId, shipment, time));
	}

	public void notifyDelivered(Id carrierId, Id driverId, CarrierShipment shipment, double time) {
		processEvent(new ShipmentDeliveredEvent(carrierId, driverId, shipment,time));
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		CarrierAgent carrierAgent = getCarrierAgent(event.getPersonId());
		if(carrierAgent == null) return;
		carrierAgent.handleEvent(event);
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		CarrierAgent carrierAgent = getCarrierAgent(event.getDriverId());
		if(carrierAgent == null) return;
		carrierAgent.handleEvent(event);
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		CarrierAgent carrierAgent = getCarrierAgent(event.getPersonId());
		if(carrierAgent == null) return;
		carrierAgent.handleEvent(event);
	}


	@Override
	public void handleEvent(PersonArrivalEvent event) {
		CarrierAgent carrierAgent = getCarrierAgent(event.getPersonId());
		if(carrierAgent == null) return;
		carrierAgent.handleEvent(event);
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		CarrierAgent carrierAgent = getCarrierAgent(event.getPersonId());
		if(carrierAgent == null) return;
		carrierAgent.handleEvent(event);
	}

	private CarrierAgent getCarrierAgent(Id driverId) {
		if(driverAgentMap.containsKey(driverId)){
			return driverAgentMap.get(driverId);
		}
		for(CarrierAgent ca : carrierAgents){
			if(ca.getDriverIds().contains(driverId)){
				driverAgentMap.put(driverId, ca);
				return ca;
			}
		}
		return null;	
	}
	
	CarrierDriverAgent getDriver(Id driverId){
		CarrierAgent carrierAgent = getCarrierAgent(driverId);
		if(carrierAgent == null) throw new IllegalStateException("missing carrier agent. cannot find carrierAgent to driver " + driverId);
		return carrierAgent.getDriver(driverId);
	}
}
