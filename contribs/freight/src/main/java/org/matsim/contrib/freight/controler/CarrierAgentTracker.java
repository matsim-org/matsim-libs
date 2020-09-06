package org.matsim.contrib.freight.controler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.controler.CarrierAgent.CarrierDriverAgent;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.scoring.ScoringFunction;

/**
 * This keeps track of all carrierAgents during simulation.
 * 
 * @author mzilske, sschroeder
 *
 */
class CarrierAgentTracker implements ActivityStartEventHandler, ActivityEndEventHandler, PersonDepartureEventHandler, PersonArrivalEventHandler,  LinkEnterEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {
	private static final Logger log = Logger.getLogger( CarrierAgentTracker.class ) ;

	private final Carriers carriers;

	private final Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler();

	private final Collection<CarrierAgent> carrierAgents = new ArrayList<CarrierAgent>();
	
	private final Map<Id<Person>, CarrierAgent> driverAgentMap = new HashMap<Id<Person>, CarrierAgent>();

	public CarrierAgentTracker(Carriers carriers, Network network, CarrierScoringFunctionFactory carrierScoringFunctionFactory) {
		log.warn( "calling ctor; carrierScoringFunctionFactory=" + carrierScoringFunctionFactory.getClass() );
		this.carriers = carriers;
		createCarrierAgents(carrierScoringFunctionFactory);
	}

	private void createCarrierAgents(CarrierScoringFunctionFactory carrierScoringFunctionFactory) {
		for (Carrier carrier : carriers.getCarriers().values()) {
			log.warn( "" );
			log.warn( "about to create scoring function for carrierId=" + carrier.getId() );
			ScoringFunction carrierScoringFunction = carrierScoringFunctionFactory.createScoringFunction(carrier);
			log.warn( "have now created scoring function for carrierId=" + carrier.getId() );
			log.warn( "" );
			CarrierAgent carrierAgent = new CarrierAgent( carrier, carrierScoringFunction, delegate);
			carrierAgents.add(carrierAgent);
		}
	}

	/**
	 * Returns the entire set of selected carrier plans.
	 * 
	 * @return collection of plans
	 * @see Plan, CarrierPlan
	 */
	Collection<Plan> createPlans() {
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
		delegate.reset(iteration);
	}

	private CarrierAgent findCarrierAgent(Id<Carrier> id) {
		for (CarrierAgent agent : carrierAgents) {
			if (agent.getId().equals(id)) {
				return agent;
			}
		}
		return null;
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		CarrierAgent carrierAgent = getCarrierAgent(event.getPersonId());
		if(carrierAgent == null) return;
		carrierAgent.handleEvent(event);
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		CarrierAgent carrierAgent = getCarrierAgent(delegate.getDriverOfVehicle(event.getVehicleId()));
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

	private CarrierAgent getCarrierAgent(Id<Person> driverId) {
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
	
	CarrierDriverAgent getDriver(Id<Person> driverId){
		CarrierAgent carrierAgent = getCarrierAgent(driverId);
		if(carrierAgent == null) throw new IllegalStateException("missing carrier agent. cannot find carrierAgent to driver " + driverId);
		return carrierAgent.getDriver(driverId);
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		delegate.handleEvent(event);
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		delegate.handleEvent(event);
	}
}
