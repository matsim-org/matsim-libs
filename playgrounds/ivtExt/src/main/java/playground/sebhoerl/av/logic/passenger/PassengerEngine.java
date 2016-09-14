package playground.sebhoerl.av.logic.passenger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.vehicles.Vehicle;

import playground.sebhoerl.agentlock.events.Event;
import playground.sebhoerl.agentlock.events.EventListener;
import playground.sebhoerl.av.logic.agent.AVAgent;
import playground.sebhoerl.av.logic.events.DropoffEvent;
import playground.sebhoerl.av.logic.events.PickupEvent;
import playground.sebhoerl.av.logic.service.Service;

public class PassengerEngine implements MobsimEngine, EventListener {
    final private EventsManager events;
    
    private InternalInterface internalInterface;
    
    public PassengerEngine(EventsManager events) {
        this.events = events;
    }
    
    public void notifyEvent(Event event) {
        if (event instanceof PickupEvent) handlePickup(((PickupEvent)event).getService());
        if (event instanceof DropoffEvent) handleDropoff(((DropoffEvent)event).getService());
    }
    
    public void handlePickup(Service service) {
        AVAgent agent = service.getDriverAgent();
        PassengerAgent passenger = (PassengerAgent) service.getPassengerAgent();
        
        passenger.setVehicle(agent.getVehicle());
        agent.getVehicle().addPassenger(passenger);
        
        Id<Vehicle> vehicleId = service.getDriverAgent().getVehicle().getId();
        events.processEvent(new PersonEntersVehicleEvent(service.getPickupTime(), passenger.getId(), vehicleId));
    }
    
    public void handleDropoff(Service service) {
        double now = service.getDropoffTime();
        
        MobsimAgent mobsimAgent = service.getPassengerAgent();
        PassengerAgent passengerAgent = (PassengerAgent) mobsimAgent;
        
        AVAgent agent = service.getDriverAgent();
        
        passengerAgent.setVehicle(null);
        agent.getVehicle().removePassenger(passengerAgent);
               
        events.processEvent(new TeleportationArrivalEvent(now, passengerAgent.getId(), service.getDropoffDriveDistance()));
    
        Id<Vehicle> vehicleId = service.getDriverAgent().getVehicle().getId();
        events.processEvent(new PersonLeavesVehicleEvent(now, passengerAgent.getId(), vehicleId));
        
        mobsimAgent.notifyArrivalOnLinkByNonNetworkMode(service.getRequest().getDropoffLinkId());
        mobsimAgent.endLegAndComputeNextState(now);
        internalInterface.arrangeNextAgentState(mobsimAgent);
    }
    
    // Mobsim, only for the internal interface
    
    @Override
    public void doSimStep(double time) {}

    @Override
    public void onPrepareSim() {}

    @Override
    public void afterSim() {}

    @Override
    public void setInternalInterface(InternalInterface internalInterface) {
        this.internalInterface = internalInterface;
    }

}
