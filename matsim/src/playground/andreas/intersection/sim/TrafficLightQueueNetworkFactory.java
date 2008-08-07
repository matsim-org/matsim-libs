package playground.andreas.intersection.sim;

import org.matsim.mobsim.queuesim.QueueLink;
import org.matsim.mobsim.queuesim.QueueNetwork;
import org.matsim.mobsim.queuesim.QueueNetworkFactory;
import org.matsim.mobsim.queuesim.QueueNode;
import org.matsim.network.Link;
import org.matsim.network.Node;

public final class TrafficLightQueueNetworkFactory implements QueueNetworkFactory<QueueNode, QueueLink> {

	/**
	 * @see org.matsim.mobsim.queuesim.QueueNetworkFactory#newQueueLink(org.matsim.network.Link, org.matsim.mobsim.queuesim.QueueNetwork)
	 */
	public QueueLink newQueueLink(Link link, QueueNetwork queueNetwork, QueueNode toQueueNode) {
		return new QLink(link, queueNetwork, toQueueNode);
	}

	/**
	 * @see org.matsim.mobsim.queuesim.QueueNetworkFactory#newQueueNode(org.matsim.network.Node, org.matsim.mobsim.queuesim.QueueNetwork)
	 */
	public QueueNode newQueueNode(Node node, QueueNetwork queueNetwork) {
		return new QNode(node, queueNetwork);
	}

}
