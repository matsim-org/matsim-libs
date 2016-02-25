package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLinkImpl.LaneFactory;

public final class AssignmentEmulatingQLaneNetworkFactory implements NetsimNetworkFactory {
	@Override
	public QNode createNetsimNode(Node node, QNetwork network) {
		return new QNode(node, network);
	}

	@Override
	public QLinkI createNetsimLink(Link link, QNetwork network, QNode queueNode) {
		LaneFactory roadFactory = new LaneFactory() {
			@Override
			public QLaneI createLane(QLinkImpl qLinkImpl) {
				VehicleQ<QVehicle> vehicleQueue = new FIFOVehicleQ() ; 
				return new AssignmentEmulatingQLane(qLinkImpl, vehicleQueue, qLinkImpl.getLink().getId() ) ;
			}
		} ;
		return new QLinkImpl(link, network, queueNode, roadFactory) ;
	}
}