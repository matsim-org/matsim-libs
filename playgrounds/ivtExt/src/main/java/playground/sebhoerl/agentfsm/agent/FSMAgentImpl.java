package playground.sebhoerl.agentfsm.agent;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.facilities.Facility;
import org.matsim.vehicles.Vehicle;

import playground.sebhoerl.agentfsm.StateMachine;
import playground.sebhoerl.agentlock.LockEngine;
import playground.sebhoerl.agentlock.agent.AbstractLockAgent;
import playground.sebhoerl.agentlock.lock.AgentLock;
import playground.sebhoerl.agentlock.lock.LockHandle;

public class FSMAgentImpl extends AbstractLockAgent implements MobsimDriverAgent, MobsimPassengerAgent, FSMAgent {    
    final private Id<Person> id;
    final private EventsManager events;
    
    protected AgentLock lock;
    protected FSMAgentController controller;

    private MobsimVehicle vehicle;
    private Id<Link> currentLinkId;
    
    private Leg leg;
    private int routeIndex;
    private NetworkRoute route;
    
    public FSMAgentImpl(EventsManager events, LockEngine lockEngine, StateMachine machine, Id<Person> id, Id<Link> startLinkId) {
        this.id = id;
        this.currentLinkId = startLinkId;
        this.events = events;
        this.lock = new AgentLock(lockEngine, this);
        this.controller = new FSMAgentController(this, lock, machine);
    }
    
    public void setController(FSMAgentController controller) {
    	this.controller = controller;
    }
    
    public void startLeg(Leg leg) {
    	this.leg = leg;
    	routeIndex = 0;

    	if (leg.getRoute() instanceof NetworkRoute) {
    		route = (NetworkRoute) leg.getRoute();
    	}
    }
    
    @Override
    public LockHandle computeNextState(double now) {
        return controller.computeNextState(now);
    }

    @Override
    public void endLegAndComputeNextState(double now) {
        // Post this event here.
        
    	events.processEvent(new PersonArrivalEvent(now, id, currentLinkId, leg.getMode()));
    	super.endLegAndComputeNextState(now); 
    }

    @Override
    public Double getExpectedTravelTime() {
        return leg.getTravelTime();
    }

    @Override
    public Double getExpectedTravelDistance() {
        return leg.getRoute().getDistance();
    }

    @Override
    public void notifyArrivalOnLinkByNonNetworkMode(Id<Link> linkId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Id<Link> getCurrentLinkId() {
        return currentLinkId;
    }

    @Override
    public Id<Link> getDestinationLinkId() {
        return leg.getRoute().getEndLinkId();
    }

    @Override
    public String getMode() {
        return leg.getMode();
    }

    @Override
    public Id<Person> getId() {
        return id;
    }

    @Override
    public Id<Link> chooseNextLinkId() {
    	if (route == null) {
    		throw new IllegalStateException();
    	}
    	
        if (routeIndex >= route.getLinkIds().size()) {
            return route.getEndLinkId();
        }
        
        return route.getLinkIds().get(routeIndex);
    }

    @Override
    public void notifyMoveOverNode(Id<Link> newLinkId) {
    	if (route == null) {
    		throw new IllegalStateException();
    	}
    	
        routeIndex++;
        currentLinkId = newLinkId;
    }

    @Override
    public boolean isWantingToArriveOnCurrentLink() {
        return super.isLockWantingToArriveOnCurrentLink(currentLinkId.equals(route.getEndLinkId()));
    }

    @Override
    public void setVehicle(MobsimVehicle veh) {
        vehicle = veh;
    }

    @Override
    public MobsimVehicle getVehicle() {
        return vehicle;
    }

    @Override
    public Id<Vehicle> getPlannedVehicleId() {
        return null;
    }
    
    @Override
    public Facility<? extends Facility<?>> getCurrentFacility() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public Facility<? extends Facility<?>> getDestinationFacility() {
        throw new UnsupportedOperationException();
    }
}