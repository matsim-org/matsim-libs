package tutorial.programming.ownMobsimAgent;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimAgent.State;

class MyMobsimAgent implements MobsimAgent {
	private Id<Person> id;

	MyMobsimAgent() {
		this.id = Id.createPersonId( "MyMobsimAgent") ;
	}

	@Override
	public Id<Link> getCurrentLinkId() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException() ;
	}

	@Override
	public Id getDestinationLinkId() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException() ;
	}

	@Override
	public Id<Person> getId() {
		return this.id ;
	}

	@Override
	public State getState() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException() ;
	}

	@Override
	public double getActivityEndTime() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException() ;
	}

	@Override
	public void endActivityAndComputeNextState(double now) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException() ;
	}

	@Override
	public void endLegAndComputeNextState(double now) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException() ;
	}

	@Override
	public void setStateToAbort(double now) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException() ;
	}

	@Override
	public Double getExpectedTravelTime() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException() ;
	}

	@Override
	public String getMode() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException() ;
	}

	@Override
	public void notifyArrivalOnLinkByNonNetworkMode(Id linkId) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException() ;
	}

}