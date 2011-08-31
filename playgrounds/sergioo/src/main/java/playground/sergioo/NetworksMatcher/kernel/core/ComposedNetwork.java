package playground.sergioo.NetworksMatcher.kernel.core;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkImpl;

public class ComposedNetwork extends NetworkImpl implements Cloneable {

	//Attributes

	//Static method
	
	public static ComposedNetwork convert(Network networkImpl) {
		ComposedNetwork network = new ComposedNetwork();
		for(Node node:networkImpl.getNodes().values()) {
			Set<Node> nodes = new HashSet<Node>();
			nodes.add(node);
			network.addNode(new ComposedNode(nodes));
		}
		for(Link link:networkImpl.getLinks().values()) {
			ComposedLink composedLink = new ComposedLink(link.getId(), network.getNodes().get(link.getFromNode().getId()), network.getNodes().get(link.getToNode().getId()), network);
			network.addLink(composedLink);
		}
		return network;
	}
	
	//Methods
	
	public ComposedNetwork clone() {
		ComposedNetwork network = new ComposedNetwork();
		for(Node node:getNodes().values()) {
			network.addNode(((ComposedNode)node).clone());
		}
		for(Link link:getLinks().values()) {
			ComposedLink composedLink = new ComposedLink(link.getId(), network.getNodes().get(link.getFromNode()), network.getNodes().get(link.getToNode()), network);
			network.addLink(composedLink);
		}
		return network;
	}
	
}
