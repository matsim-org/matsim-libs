package playground.sebhoerl.av.logic.agent;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.vehicles.Vehicle;

import playground.sebhoerl.agentfsm.agent.FSMAgentImpl;
import playground.sebhoerl.agentfsm.StateMachine;
import playground.sebhoerl.agentlock.LockEngine;
import playground.sebhoerl.av.logic.service.Service;

/**
 * Driver agent for autonomous taxis, should only compute it's own state.
 * Interactions with passengers and the "world" are handled in AVEngine through
 * the AVService instance.
 */
public class AVAgent extends FSMAgentImpl {
	private MobsimVehicle vehicle;
	private Service service;
	
	public AVAgent(EventsManager events, LockEngine lockEngine, StateMachine machine, Id<Person> id, Id<Link> startLinkId, MobsimVehicle vehicle) {
		super(events, lockEngine, machine, id, startLinkId);
		this.vehicle = vehicle;
	}

    @Override
    public Id<Vehicle> getPlannedVehicleId() {
        return vehicle.getId();
    }
    
    @Override
    public void setVehicle(MobsimVehicle vehicle) {}
    
    @Override
    public MobsimVehicle getVehicle() {
    	return vehicle;
    }
    
    public void handleService(Service service) {
        if (!controller.getCurrentStateId().equals("Idle")) {
        	throw new IllegalStateException(controller.getCurrentStateId());
        }
        
        this.service = service;
        controller.release();
    }
    
    public Service getService() {
    	return service;
    }
}
