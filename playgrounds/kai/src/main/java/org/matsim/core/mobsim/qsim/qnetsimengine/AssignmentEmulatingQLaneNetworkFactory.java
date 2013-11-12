package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLinkImpl.RoadFactory;

public final class AssignmentEmulatingQLaneNetworkFactory implements NetsimNetworkFactory<QNode, QLinkInternalI> {
	@Override
	public QNode createNetsimNode(Node node, QNetwork network) {
		return new QNode(node, network);
	}

	@Override
	public QLinkInternalI createNetsimLink(Link link, QNetwork network, QNode queueNode) {
		RoadFactory roadFactory = new RoadFactory() {
			@Override
			public QLaneInternalI createRoad(QLinkImpl qLinkImpl) {
				VehicleQ<QVehicle> vehicleQueue = new FIFOVehicleQ() ; 
				return new AssignmentEmulatingQLane(qLinkImpl, vehicleQueue, qLinkImpl.getLink().getId() ) ;
			}
		} ;
		return new QLinkImpl(link, network, queueNode, roadFactory) ;
	}
}