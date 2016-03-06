package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.Collection;
import java.util.List;

import matsimConnector.agents.Pedestrian;
import matsimConnector.engine.CAAgentFactory;
import matsimConnector.environment.TransitionArea;
import matsimConnector.events.CAAgentConstructEvent;
import matsimConnector.scenario.CAEnvironment;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.vehicles.Vehicle;
import org.matsim.vis.snapshotwriters.VisData;

public class QCALink extends AbstractQLink {

	private final Link link;
	private final QNetwork qNetwork;
	//private QLinkInternalI qLink;
	private final CAEnvironment environmentCA;
	private final CAAgentFactory agentFactoryCA;
	private final TransitionArea transitionArea;
	private CALane qlane;

	public QCALink(Link link, QNetwork network, QLinkI qLink, CAEnvironment environmentCA, CAAgentFactory agentFactoryCA, TransitionArea transitionArea) {
		super(link, network);
		this.link = link;
		//this.qLink = qLink;
		this.qNetwork = network;
		this.environmentCA = environmentCA;
		this.agentFactoryCA = agentFactoryCA;
		this.transitionArea = transitionArea;
		this.qlane = new CALane() ;
	}
	
	@Override
	QLaneI getAcceptingQLane() {
		return this.qlane ;
	}
	
	public Id<Link> getLinkId(){
		return link.getId();
	}

	@Override
	public Link getLink() {
		// TODO Auto-generated method stub
		return link;
	}

	@Override
	public void recalcTimeVariantAttributes(double time) {
		// NOT NEEDED
		throw new RuntimeException("Method not needed for the moment");
	}

	@Override
	public Collection<MobsimVehicle> getAllNonParkedVehicles() {
		// NOT NEEDED
		throw new RuntimeException("Method not needed for the moment");
	}

	@Override
	public VisData getVisData() {
		// NOT NEEDED
		throw new RuntimeException("Method not needed for the moment");
	}

	@Override
	QNode getToNode() {
		// NOT NEEDED
		throw new RuntimeException("Method not needed for the moment");
	}

	@Override
	boolean doSimStep(double now) {
		// NOT NEEDED
		throw new RuntimeException("Method not needed for the moment");
	}

	public void notifyMoveOverBorderNode(QVehicle vehicle, Id<Link> nextLinkId){
		double now = this.qNetwork.simEngine.getMobsim().getSimTimer().getTimeOfDay();
		network.simEngine.getMobsim().getEventsManager().processEvent(new LinkLeaveEvent(
				now, vehicle.getId(), this.link.getId()));
		network.simEngine.getMobsim().getEventsManager().processEvent(new LinkEnterEvent(
				now, vehicle.getId(), nextLinkId));
	}
	
	@Override
	boolean isNotOfferingVehicle() {
		// NOT NEEDED
		throw new RuntimeException("Method not needed for the moment");
	}
	
	public TransitionArea getTransitionArea() {
		return transitionArea;
	}
	
	@Override
	List<QLaneI> getOfferingQLanes() {
		throw new RuntimeException("not implemented") ;
	}

	class CALane extends QLaneI {

		@Override
		void addFromWait(QVehicle veh, double now) {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		boolean isAcceptingFromWait() {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		void updateRemainingFlowCapacity() {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		boolean isActive() {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		double getSimulatedFlowCapacity() {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		void recalcTimeVariantAttributes(double now) {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		QVehicle getVehicle(Id<Vehicle> vehicleId) {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		double getStorageCapacity() {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		VisData getVisData() {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		void addTransitSlightlyUpstreamOfStop(QVehicle veh) {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		void changeUnscaledFlowCapacityPerSecond(double val, double now) {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		void changeEffectiveNumberOfLanes(double val, double now) {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		boolean doSimStep(double now) {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		void clearVehicles() {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		Collection<MobsimVehicle> getAllVehicles() {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		void addFromUpstream(QVehicle veh) {
			Pedestrian pedestrian = agentFactoryCA.buildPedestrian(environmentCA.getId(),veh,transitionArea);		
			
			double now = qNetwork.simEngine.getMobsim().getSimTimer().getTimeOfDay();
//			qNetwork.simEngine.getMobsim().getEventsManager().processEvent(new LinkEnterEvent(
//					now, veh.getId(), getLink().getId()));
			// now done by QNode
			qNetwork.simEngine.getMobsim().getEventsManager().processEvent(new CAAgentConstructEvent(
					now, pedestrian));
		}

		@Override
		boolean isNotOfferingVehicle() {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		QVehicle popFirstVehicle() {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		QVehicle getFirstVehicle() {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		double getLastMovementTimeOfFirstVehicle() {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		boolean hasGreenForToLink(Id<Link> toLinkId) {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		boolean isAcceptingFromUpstream() {
			return transitionArea.acceptPedestrians();
		}
		
	}

}
