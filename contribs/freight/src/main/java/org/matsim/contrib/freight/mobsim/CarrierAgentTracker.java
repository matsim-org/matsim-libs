package org.matsim.contrib.freight.mobsim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.carrier.Shipment;
import org.matsim.contrib.freight.events.ShipmentDeliveredEvent;
import org.matsim.contrib.freight.events.ShipmentPickedUpEvent;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.events.EventsUtils;
import org.matsim.population.algorithms.PlanAlgorithm;

public class CarrierAgentTracker implements ActivityEndEventHandler, LinkEnterEventHandler, ActivityStartEventHandler
{

    private Carriers carriers;

    private Collection<CarrierAgent> carrierAgents = new ArrayList<CarrierAgent>();

    private Network network;

    private CarrierAgentFactory carrierAgentFactory;

    private EventsManager eventsManager;

    public CarrierAgentTracker(Carriers carriers, PlanAlgorithm router, Network network, CarrierAgentFactory carrierAgentFactory) {
        this.carriers = carriers;
        this.network = network;
        this.carrierAgentFactory = carrierAgentFactory;
        createCarrierAgents();
        eventsManager = EventsUtils.createEventsManager();
    }

    public EventsManager getEventsManager(){
        return eventsManager;
    }

    private void processEvent(Event event){
        eventsManager.processEvent(event);
    }

    @Override
    public void handleEvent(ActivityEndEvent event) {
        Id personId = event.getPersonId();
        String activityType = event.getActType();
        for (CarrierAgent carrierAgent : carrierAgents) {
            if (carrierAgent.getDriverIds().contains(personId)) {
                carrierAgent.activityEndOccurs(personId, activityType, event.getTime());
            }
        }
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        Id personId = event.getPersonId();
        Id linkId = event.getLinkId();
        double distance = network.getLinks().get(linkId).getLength();
        for (CarrierAgent carrierAgent : carrierAgents) {
            if (carrierAgent.getDriverIds().contains(personId)) {
                carrierAgent.tellDistance(personId, distance);
                carrierAgent.tellLink(personId, linkId);
            }
        }
    }

    @Override
    public void handleEvent(ActivityStartEvent event) {
        Id personId = event.getPersonId();
        String activityType = event.getActType();
        for (CarrierAgent carrierAgent : carrierAgents) {
            if (carrierAgent.getDriverIds().contains(personId)) {
                carrierAgent.activityStartOccurs(personId, activityType, event.getTime());
            }
        }
    }

    private void createCarrierAgents() {
        for (Carrier carrier : carriers.getCarriers().values()) {
            CarrierAgent carrierAgent = carrierAgentFactory.createAgent(this,carrier);
            carrierAgents.add(carrierAgent);
        }
    }

    public void notifyPickup(Id carrierId, Id driverId, Shipment shipment, double time) {
        processEvent(new ShipmentPickedUpEvent(carrierId, driverId, shipment, time));
    }

    public void notifyDelivery(Id carrierId, Id driverId, Shipment shipment, double time) {
        processEvent(new ShipmentDeliveredEvent(carrierId, driverId, shipment, time));
    }

    private CarrierAgent findCarrierAgent(Id id) {
        for(CarrierAgent agent : carrierAgents){
            if(agent.getId().equals(id)){
                return agent;
            }
        }
        return null;
    }

    public void calculateCosts() {
        for(Carrier carrier : carriers.getCarriers().values()){
            CarrierAgent agent = findCarrierAgent(carrier.getId());
            agent.calculateCosts();
        }

    }

    public Collection<Plan> createPlans() {
        List<Plan> plans = new ArrayList<Plan>();
        for (CarrierAgent carrierAgent : carrierAgents) {
            List<Plan> plansForCarrier = carrierAgent.createFreightDriverPlans();
            plans.addAll(plansForCarrier);
        }
        return plans;
    }

    @Override
    public void reset(int iteration) {
        // TODO Auto-generated method stub

    }

}
