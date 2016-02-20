package tutorial.programming.ownMobsimAgentUsingRouter;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.facilities.Facility;
import org.matsim.vehicles.Vehicle;

import java.util.Map;
import java.util.Random;

/**
 * MobsimDriverAgent that (1) selects a random destination; (2) computes best path to it at every turn; (3) at destination selects a new random 
 * destination.
 * 
 * Not tested ...
 * 
 * @author nagel
 */

class MyMobsimAgent implements MobsimDriverAgent {
	private Id<Person> id;
	private MyGuidance guidance;
	private Id<Link> linkId;
	private MobsimVehicle vehicle;
	private MobsimTimer mobsimTimer;
	private Id<Link> destinationLinkId;
	private Scenario scenario;
	private Random rnd = new Random(4711) ;

	MyMobsimAgent(MyGuidance guidance, MobsimTimer mobsimTimer, Scenario scenario) {
		this.id = Id.createPersonId( "MyMobsimAgent") ;
		this.guidance = guidance ;
		this.mobsimTimer = mobsimTimer ;
		this.scenario = scenario ;
		
		this.linkId = getRandomLink() ;
		this.destinationLinkId = getRandomLink() ;
	}

	@Override
	public Id<Link> getCurrentLinkId() {
		return this.linkId ;
	}

	@Override
	public Id<Link> getDestinationLinkId() {
		return this.destinationLinkId ;
	}

	@Override
	public Id<Person> getId() {
		return this.id ;
	}

	@Override
	public State getState() {
		return MobsimAgent.State.LEG ;
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
		throw new UnsupportedOperationException() ;
	}

	@Override
	public void setStateToAbort(double now) {
		throw new UnsupportedOperationException() ;
	}

	@Override
	public Double getExpectedTravelTime() {
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
		return this.guidance.getBestOutgoingLink( this.linkId, this.destinationLinkId, this.mobsimTimer.getTimeOfDay()  ) ;
	}

	@Override
	public void notifyMoveOverNode(Id<Link> newLinkId) {
		this.linkId = newLinkId ;
	}

	@Override
	public boolean isWantingToArriveOnCurrentLink() {
		if ( this.linkId.equals( this.destinationLinkId ) ) {
			getRandomLink();
		}
		return false ;
	}

	private Id<Link> getRandomLink() {
		// if we are at the final destination, select a random new destination:
		Map<Id<Link>, ? extends Link> links = this.scenario.getNetwork().getLinks() ;
		int idx = rnd.nextInt(links.size()) ;
		int cnt = 0 ;
		for ( Link link : links.values() ) {
			if ( cnt== idx ) {
				return link.getId() ;
			}
		}
		throw new RuntimeException("should not happen");
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
		return null ;
	}

	@Override
	public Facility<? extends Facility<?>> getCurrentFacility() {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public Facility<? extends Facility<?>> getDestinationFacility() {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

}