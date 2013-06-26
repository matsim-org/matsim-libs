package playground.kai.usecases.janus;
import java.util.Map;

import org.janusproject.kernel.Kernel;
import org.janusproject.kernel.agent.Agent;
import org.janusproject.kernel.agent.Kernels;
import org.janusproject.kernel.status.Status;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.DriverAgent;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;


public class MyMobsimDriverAgent implements DriverAgent, MobsimAgent {
	
	@SuppressWarnings("serial")
	class JanusAgent extends Agent {
		
		@Override
		public Status live() {
			print("hello world!\n");
			return null;
		}

	}

	private Id currentLinkId;
	private MobsimVehicle veh;
	private State state;
	private Network network;
	
	MyMobsimDriverAgent( Network network ) {
		JanusAgent a = new JanusAgent();
		Kernel k = Kernels.get();
		k.launchLightAgent(a);
		this.network = network ;
	}

	@Override
	public Id getCurrentLinkId() {
		return this.currentLinkId ;
	}

	@Override
	public Id getDestinationLinkId() {
		return null ;
	}

	@Override
	public void setVehicle(MobsimVehicle veh) {
		this.veh = veh ;
	}

	@Override
	public MobsimVehicle getVehicle() {
		return this.veh ;
	}

	@Override
	public Id getPlannedVehicleId() {
		return null ; // yyyy
	}

	@Override
	public Id getId() {
		return new IdImpl("test driver");
	}

	@Override
	public double getActivityEndTime() {
		return 0. ;
	}

	@Override
	public void endActivityAndComputeNextState(double now) {
		this.state = State.LEG ;
	}

	@Override
	public void endLegAndComputeNextState(double now) {
		throw new RuntimeException("should never end leg") ;
	}

	@Override
	public void abort(double now) {
		this.state = State.ABORT ;
	}

	@Override
	public Double getExpectedTravelTime() {
		return 0. ;
	}

	@Override
	public String getMode() {
		return TransportMode.car ;
	}

	@Override
	public void notifyArrivalOnLinkByNonNetworkMode(Id linkId) {
		throw new UnsupportedOperationException("should not happen") ;
	}

	@Override
	public Id chooseNextLinkId() {
		Link currentLink = network.getLinks().get(this.currentLinkId ) ;
		Node toNode = currentLink.getToNode() ;
		Map<Id, ? extends Link> outgoingLinks = toNode.getOutLinks() ;
		int idx = MatsimRandom.getRandom().nextInt(outgoingLinks.size()) ;
		return outgoingLinks.get(idx).getId() ;
	}

	@Override
	public void notifyMoveOverNode(Id newLinkId) {
		this.currentLinkId = newLinkId ;
	}

	@Override
	public State getState() {
		return this.state ;
	}

}
