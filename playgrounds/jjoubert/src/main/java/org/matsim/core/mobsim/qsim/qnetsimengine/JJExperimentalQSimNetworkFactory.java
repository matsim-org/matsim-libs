package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLinkImpl.LaneFactory;
import org.matsim.lanes.data.v20.Lane;

public final class JJExperimentalQSimNetworkFactory implements NetsimNetworkFactory<QNode, QLinkInternalI> {
	@Override
	public QNode createNetsimNode(Node node, QNetwork network) {
		return new QNode(node, network);
	}

	@Override
	public QLinkInternalI createNetsimLink(Link link, QNetwork network, QNode queueNode) {
		LaneFactory laneFactory = new LaneFactory() {
			@Override
			public QLaneI createLane(QLinkImpl qLinkImpl) {
				QueueWithBuffer.Builder builder = new QueueWithBuffer.Builder( qLinkImpl ) ;
				builder.setVehicleQueue(new JJExperimentalVehicleQ());
				return builder.build() ;
			}
		} ;
		return new QLinkImpl(link, network, queueNode, laneFactory) ;
	}
}