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
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine.NetsimInternalInterface;
import org.matsim.lanes.data.v20.Lane;
import org.matsim.vehicles.Vehicle;
import org.matsim.vis.snapshotwriters.VisData;

public class QCALink extends AbstractQLink {

	private final NetsimEngineContext context;
	//private QLinkInternalI qLink;
	private final CAEnvironment environmentCA;
	private final CAAgentFactory agentFactoryCA;
	private final TransitionArea transitionArea;
	private CALane qlane;

	public QCALink(Link link, CAEnvironment environmentCA, CAAgentFactory agentFactoryCA, TransitionArea transitionArea, NetsimEngineContext context, 
			NetsimInternalInterface netsimEngine) {
		super(link, null, context, netsimEngine);
		this.context = context ;
		this.environmentCA = environmentCA;
		this.agentFactoryCA = agentFactoryCA;
		this.transitionArea = transitionArea;
		this.qlane = new CALane() ;
	}
	
	@Override
	QLaneI getAcceptingQLane() {
		return this.qlane ;
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
	boolean doSimStep() {
		// NOT NEEDED
		throw new RuntimeException("Method not needed for the moment");
	}

	public void notifyMoveOverBorderNode(QVehicle vehicle, Id<Link> nextLinkId){
		double now = context.getSimTimer().getTimeOfDay();
		context.getEventsManager().processEvent(new LinkLeaveEvent(
				now, vehicle.getId(), getLink().getId()));
		context.getEventsManager().processEvent(new LinkEnterEvent(
				now, vehicle.getId(), nextLinkId));
	}
	
	@Override
	boolean isNotOfferingVehicle() {
		return this.qlane.isNotOfferingVehicle() ;
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
		boolean isAcceptingFromUpstream() {
			return transitionArea.acceptPedestrians();
		}
		@Override
		void addFromUpstream(QVehicle veh) {
			double now = context.getSimTimer().getTimeOfDay() ;
			
			Pedestrian pedestrian = agentFactoryCA.buildPedestrian(environmentCA.getId(),veh,transitionArea);		
			
//			qNetwork.simEngine.getMobsim().getEventsManager().processEvent(new LinkEnterEvent(
//					now, veh.getId(), getLink().getId()));
			// now done by QNode
			context.getEventsManager().processEvent(new CAAgentConstructEvent(
					now, pedestrian));
		}

		// === simulation logic (I am a bit surprised that it works completely without) ===
		@Override
		boolean doSimStep() {
			throw new RuntimeException("not implemented") ;
		}
		@Override
		boolean isActive() {
			throw new RuntimeException("not implemented") ;
		}
		@Override
		void clearVehicles() {
			throw new RuntimeException("not implemented") ;
		}

		// === information about the link (might be useful in some cases) ===
		@Override
		Collection<MobsimVehicle> getAllVehicles() {
			throw new RuntimeException("not implemented") ;
		}
		@Override
		VisData getVisData() {
			throw new RuntimeException("not implemented") ;
		}
		@Override
		QVehicle getVehicle(Id<Vehicle> vehicleId) {
			throw new RuntimeException("not implemented") ;
		}
		@Override
		double getSimulatedFlowCapacityPerTimeStep() {
			throw new RuntimeException("not implemented") ;
		}

		@Override
		double getStorageCapacity() {
			throw new RuntimeException("not implemented") ;
		}

		// === functionality for arrival/departure area ===
		@Override
		void addFromWait(QVehicle veh) {
			throw new RuntimeException("not implemented") ;
		}
		@Override
		boolean isAcceptingFromWait() {
			throw new RuntimeException("not implemented") ;
		}
		@Override
		void addTransitSlightlyUpstreamOfStop(QVehicle veh) {
			throw new RuntimeException("not implemented") ;
		}

		// === functionality for downstream end (not needed) ===
		@Override
		boolean isNotOfferingVehicle() {
			throw new RuntimeException("not implemented") ;
		}
		@Override
		QVehicle popFirstVehicle() {
			throw new RuntimeException("not implemented") ;
		}
		@Override
		QVehicle getFirstVehicle() {
			throw new RuntimeException("not implemented") ;
		}
		@Override
		double getLastMovementTimeOfFirstVehicle() {
			throw new RuntimeException("not implemented") ;
		}
		@Override
		boolean hasGreenForToLink(Id<Link> toLinkId) {
			throw new RuntimeException("not implemented") ;
		}
		
		// === time-dependent link characteristics (not needed) ===
		@Override
		void changeUnscaledFlowCapacityPerSecond(double val) {
			throw new RuntimeException("not implemented") ;
		}
		@Override
		void changeEffectiveNumberOfLanes(double val) {
			throw new RuntimeException("not implemented") ;
		}
		@Override
		public Id<Lane> getId() {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}
		@Override
		void changeSpeedMetersPerSecond(double val) {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}
		@Override
		double getLoadIndicator() {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}


	}

	@Override
	public void recalcTimeVariantAttributes() {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

}
