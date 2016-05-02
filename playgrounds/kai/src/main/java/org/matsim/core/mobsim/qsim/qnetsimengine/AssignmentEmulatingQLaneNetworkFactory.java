package org.matsim.core.mobsim.qsim.qnetsimengine;

import javax.inject.Inject;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLinkImpl.LaneFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine.NetsimInternalInterface;
import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.DefaultLinkSpeedCalculator;
import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.LinkSpeedCalculator;
import org.matsim.core.mobsim.qsim.qnetsimengine.vehicleq.FIFOVehicleQ;
import org.matsim.core.mobsim.qsim.qnetsimengine.vehicleq.VehicleQ;
import org.matsim.core.network.NetworkImpl;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfoFactory;
import org.matsim.vis.snapshotwriters.SnapshotLinkWidthCalculator;

public final class AssignmentEmulatingQLaneNetworkFactory extends QNetworkFactory {
	@Inject private QSimConfigGroup qsimConfig ;
	@Inject private EventsManager events ;
	@Inject Network network ;
	@Inject Scenario scenario ;
	
	private NetsimInternalInterface netsimEngine;
	private NetsimEngineContext context;

	@Override
	public QNode createNetsimNode(Node node) {
		QNode.Builder builder = new QNode.Builder( netsimEngine, context ) ;
		return builder.build( node ) ;
	}

	@Override
	public QLinkI createNetsimLink(Link link, QNode queueNode) {
		LaneFactory roadFactory = new LaneFactory() {
			@Override
			public QLaneI createLane(AbstractQLink qLinkImpl) {
				VehicleQ<QVehicle> vehicleQueue = new FIFOVehicleQ() ; 
				LinkSpeedCalculator linkSpeedCalculator = new DefaultLinkSpeedCalculator() ;
				return new AssignmentEmulatingQLane(qLinkImpl, vehicleQueue, qLinkImpl.getLink().getId(), context, netsimEngine, linkSpeedCalculator ) ;
			}
		} ;
		QLinkImpl.Builder linkBuilder = new QLinkImpl.Builder(context, netsimEngine) ;
		linkBuilder.setLaneFactory(roadFactory);
		return linkBuilder.build(link, queueNode) ; 
	}

	@Override
	void initializeFactory(AgentCounter agentCounter1, MobsimTimer mobsimTimer1, NetsimInternalInterface netsimEngine1) {
		this.netsimEngine = netsimEngine1 ;
		double effectiveCellSize = ((NetworkImpl) network).getEffectiveCellSize() ;
		SnapshotLinkWidthCalculator linkWidthCalculator = new SnapshotLinkWidthCalculator();
		linkWidthCalculator.setLinkWidthForVis( qsimConfig.getLinkWidthForVis() );
		if (! Double.isNaN(network.getEffectiveLaneWidth())){
			linkWidthCalculator.setLaneWidth( network.getEffectiveLaneWidth() );
		}
		AgentSnapshotInfoFactory snapshotInfoFactory = new AgentSnapshotInfoFactory(linkWidthCalculator);
		AbstractAgentSnapshotInfoBuilder snapshotInfoBuilder = QNetsimEngine.createAgentSnapshotInfoBuilder( scenario, linkWidthCalculator );
		this.context = new NetsimEngineContext(events, effectiveCellSize, agentCounter1, snapshotInfoBuilder, qsimConfig, mobsimTimer1, linkWidthCalculator ) ;
	}
}