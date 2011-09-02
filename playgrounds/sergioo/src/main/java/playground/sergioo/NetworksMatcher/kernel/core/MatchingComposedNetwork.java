package playground.sergioo.NetworksMatcher.kernel.core;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkImpl;

public class MatchingComposedNetwork extends NetworkImpl implements Cloneable {

	//Attributes

	//Static method
	
	public static MatchingComposedNetwork convert(Network networkImpl) {
		MatchingComposedNetwork network = new MatchingComposedNetwork();
		for(Node node:networkImpl.getNodes().values()) {
			Set<Node> nodes = new HashSet<Node>();
			nodes.add(node);
			network.addNode(new ComposedNode(nodes));
		}
		for(Link link:networkImpl.getLinks().values()) {
			ComposedLink composedLink = new MatchingComposedLink(link.getId(), network.getNodes().get(link.getFromNode().getId()), network.getNodes().get(link.getToNode().getId()), network);
			network.addLink(composedLink);
		}
		return network;
	}
	
	public static MatchingComposedNetwork convert(Network networkImpl, Set<String> modes) {
		MatchingComposedNetwork network = new MatchingComposedNetwork();
		for(Node node:networkImpl.getNodes().values()) {
			Set<Node> nodes = new HashSet<Node>();
			nodes.add(node);
			network.addNode(new ComposedNode(nodes));
		}
		for(Link link:networkImpl.getLinks().values()) {
			boolean withNeededMode = false;
			for(String mode:modes)
				if(link.getAllowedModes().contains(mode))
					withNeededMode = true;
			if(withNeededMode) {
				ComposedLink composedLink = new MatchingComposedLink(link.getId(), network.getNodes().get(link.getFromNode().getId()), network.getNodes().get(link.getToNode().getId()), network);
				network.addLink(composedLink);
			}
		}
		return network;
	}
	
	//Methods
	
	public MatchingComposedNetwork clone() {
		MatchingComposedNetwork network = new MatchingComposedNetwork();
		for(Node node:getNodes().values()) {
			network.addNode(((ComposedNode)node).clone());
		}
		for(Link link:getLinks().values()) {
			ComposedLink composedLink = new MatchingComposedLink(link.getId(), network.getNodes().get(link.getFromNode().getId()), network.getNodes().get(link.getToNode().getId()), network);
			network.addLink(composedLink);
		}
		return network;
	}
	
}
