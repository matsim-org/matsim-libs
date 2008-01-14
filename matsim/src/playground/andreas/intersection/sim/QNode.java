package playground.andreas.intersection.sim;

import org.matsim.network.Node;

public class QNode {
	
	private Node node;
	
	public QNode(String id, String x, String y, String type) {
		this.node = new Node(id, x, y, type);
	}

	public Node getNode() {
		return this.node;
	}

}
