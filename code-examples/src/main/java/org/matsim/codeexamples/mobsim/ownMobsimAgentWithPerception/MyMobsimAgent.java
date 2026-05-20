package org.matsim.codeexamples.mobsim.ownMobsimAgentWithPerception;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.facilities.Facility;
import org.matsim.vehicles.Vehicle;

/**
 * MobsimDriverAgent that asks for a "best" outgoing link at each intersection.
 * 
 * @author nagel
 */
class MyMobsimAgent implements MobsimDriverAgent {
	private final Id<Vehicle> plannedVehicleId;
	private final MobsimTimer simTimer;
	private Id<Person> id;
	private MyGuidance guidance;
	private Id<Link> linkId;
	private MobsimVehicle vehicle;
	private State state = State.LEG ;
	
	MyMobsimAgent(MyGuidance guidance, Id<Link> startingLinkId, Id<Vehicle> vehicleId, MobsimTimer simTimer) {
		plannedVehicleId = vehicleId;
		this.simTimer = simTimer;
		this.id = Id.createPersonId( "MyMobsimAgent") ;
		this.guidance = guidance ;
		this.linkId = startingLinkId ;
		
	}

	@Override
	public Id<Link> getCurrentLinkId() {
		return this.linkId ;
	}

	@Override
	public Id<Link> getDestinationLinkId() {
		return null ;
	}

	@Override
	public Id<Person> getId() {
		return this.id ;
	}

	@Override
	public State getState() {
		return this.state ;
	}

	@Override
	public double getActivityEndTime() {
		return Double.POSITIVE_INFINITY ;
	}

	@Override
	public void endActivityAndComputeNextState(double now) {
		throw new UnsupportedOperationException() ;
	}

	@Override
	public void endLegAndComputeNextState(double now) {
		this.state = State.ACTIVITY ;
	}

	@Override
	public void setStateToAbort(double now) {
		throw new UnsupportedOperationException() ;
	}

	@Override
	public OptionalTime getExpectedTravelTime() {
		return null ;
	}

    @Override
    public Double getExpectedTravelDistance() {
        return null;
    }

    @Override
	public String getMode() {
		return TransportMode.car ;
	}

	@Override
	public void notifyArrivalOnLinkByNonNetworkMode(Id<Link> linkId) {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public Id<Link> chooseNextLinkId() {
		return this.guidance.getBestOutgoingLink( this.linkId ) ;
	}

	@Override
	public void notifyMoveOverNode(Id<Link> newLinkId) {
		this.linkId = newLinkId ;
	}

	@Override
	public boolean isWantingToArriveOnCurrentLink() {
		if ( this.simTimer.getTimeOfDay() > 8*3600. ) {
			return true ;
		} else {
			return false ;
		}
	}

	@Override
	public void setVehicle(MobsimVehicle veh) {
		this.vehicle = veh ;
	}

	@Override
	public MobsimVehicle getVehicle() {
		return this.vehicle ;
	}

	@Override
	public Id<Vehicle> getPlannedVehicleId() {
		return this.plannedVehicleId ;
	}

	@Override
	public Facility getCurrentFacility() {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public Facility getDestinationFacility() {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

}
