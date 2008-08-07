package playground.andreas.intersection.sim;

import org.matsim.mobsim.QueueLink;
import org.matsim.mobsim.QueueNetworkFactory;
import org.matsim.mobsim.QueueNetwork;
import org.matsim.mobsim.QueueNode;
import org.matsim.network.Link;
import org.matsim.network.Node;

public final class TrafficLightQueueNetworkFactory implements QueueNetworkFactory<QueueNode, QueueLink> {

	/**
	 * @see org.matsim.mobsim.QueueNetworkFactory#newQueueLink(org.matsim.network.Link, org.matsim.mobsim.QueueNetwork)
	 */
	public QueueLink newQueueLink(Link link, QueueNetwork queueNetwork, QueueNode toQueueNode) {
		return new QLink(link, queueNetwork, toQueueNode);
	}

	/**
	 * @see org.matsim.mobsim.QueueNetworkFactory#newQueueNode(org.matsim.network.Node, org.matsim.mobsim.QueueNetwork)
	 */
	public QueueNode newQueueNode(Node node, QueueNetwork queueNetwork) {
		return new QNode(node, queueNetwork);
	}

}
