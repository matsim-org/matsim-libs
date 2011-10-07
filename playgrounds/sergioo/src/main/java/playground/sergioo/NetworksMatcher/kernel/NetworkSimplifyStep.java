package playground.sergioo.NetworksMatcher.kernel;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;

import playground.sergioo.NetworksMatcher.kernel.core.NetworksStep;
import playground.sergioo.NetworksMatcher.kernel.core.NodesMatching;
import playground.sergioo.NetworksMatcher.kernel.core.Region;

public class NetworkSimplifyStep extends NetworksStep {


	//Attributes

	private Set<NodesMatching> nodesMatchings;
	
	
	//Methods
	
	public NetworkSimplifyStep(Set<NodesMatching> nodesMatchings, Region region) {
		super("Network simplify step", region);
		this.nodesMatchings = nodesMatchings;
	}

	@Override
	protected void process(Network oldNetworkA, Network oldNetworkB) {
		networkA = NetworkImpl.createNetwork();
		networkB = NetworkImpl.createNetwork();
		for(Node node:oldNetworkA.getNodes().values())
			if(isMatchedNode(node, true)) {
				manageNode(networkA, null, node, new HashSet<Node>(), true);
				break;
			}
		for(Node node:oldNetworkB.getNodes().values())
			if(isMatchedNode(node, false)) {
				manageNode(networkB, null, node, new HashSet<Node>(), false);
				break;
			}
	}

	private void manageNode(Network network, Node beginningNode, Node actualNode, Set<Node> alreadyVisited, boolean inA) {
		if(beginningNode==null) {
			if(!network.getNodes().containsKey(actualNode.getId())) {
				network.addNode(network.getFactory().createNode(actualNode.getId(), actualNode.getCoord()));
				Set<Link> links = new HashSet<Link>(actualNode.getOutLinks().values());
				for(Link link:links)
					manageNode(network, actualNode, link.getToNode(), alreadyVisited, inA);
			}
		}
		else if(isMatchedNode(actualNode, inA)) {
			if(!beginningNode.getId().equals(actualNode.getId()) && !network.getLinks().containsKey(new IdImpl("M"+beginningNode.getId()+"<->"+actualNode.getId()))) {
				network.addLink(network.getFactory().createLink(new IdImpl("M"+beginningNode.getId()+"<->"+actualNode.getId()), beginningNode, actualNode));
				manageNode(network, null, actualNode, alreadyVisited, inA);
			}
		}
		else if(!alreadyVisited.contains(actualNode)) {
			alreadyVisited.add(actualNode);
			Set<Link> links = new HashSet<Link>(actualNode.getOutLinks().values());
			for(Link link:links)
				manageNode(network, beginningNode, link.getToNode(), alreadyVisited, inA);
		}
	}
	
	private boolean isMatchedNode(Node node, boolean inA) {
		for(NodesMatching nodesMatching:nodesMatchings) {
			Node compareNode = inA?nodesMatching.getComposedNodeA():nodesMatching.getComposedNodeB();
			if(compareNode.getId().equals(node.getId()))
				return true;
		}
		return false;
	}
	

}
