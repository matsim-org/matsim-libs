package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLinkImpl.LaneFactory;

public final class JJExperimentalQSimNetworkFactory implements NetsimNetworkFactory<QNode, QLinkInternalI> {
	@Override
	public QNode createNetsimNode(Node node, QNetwork network) {
		return new QNode(node, network);
	}

	@Override
	public QLinkInternalI createNetsimLink(Link link, QNetwork network, QNode queueNode) {
		LaneFactory laneFactory = new LaneFactory() {
			@Override
			public QLaneInternalI createLane(QLinkImpl qLinkImpl) {
				VehicleQ<QVehicle> vehicleQueue = new JJExperimentalVehicleQ() ; 
				return new QueueWithBuffer(qLinkImpl, vehicleQueue, qLinkImpl.getLink().getId() ) ;
			}
		} ;
		return new QLinkImpl(link, network, queueNode, laneFactory) ;
	}
}