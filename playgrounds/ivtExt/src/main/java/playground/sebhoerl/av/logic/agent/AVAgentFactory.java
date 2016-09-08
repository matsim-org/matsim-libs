package playground.sebhoerl.av.logic.agent;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

import playground.sebhoerl.agentfsm.StateMachine;
import playground.sebhoerl.agentlock.LockEngine;

public class AVAgentFactory {
    final private LockEngine lockEngine; 
    final private StateMachine machine;
    final private EventsManager events;
    
    private int id = 0;
    
    public AVAgentFactory(EventsManager events, LockEngine lockEngine, StateMachine machine) {
        this.events = events;
        this.lockEngine = lockEngine;
        this.machine = machine;
    }
    
    public AVAgent createAgent(Id<Link> linkId) {
        String id = String.format("av%d", ++this.id);
        
        Id<Vehicle> vehicleId = Id.create(id, Vehicle.class);
        Vehicle vehicle = VehicleUtils.getFactory().createVehicle(vehicleId, VehicleUtils.getDefaultVehicleType());
        QVehicle mobsimVehicle = new QVehicle(vehicle);
        
        Id<Person> driverId = Id.create(id, Person.class);
        AVAgent agent = new AVAgent(events, lockEngine, machine, driverId, linkId, mobsimVehicle);
        
        mobsimVehicle.setDriver(agent);
        
        return agent;
    }
}
