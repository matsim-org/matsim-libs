package playground.sergioo.NetworksMatcher.kernel;

import java.util.Set;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

import playground.sergioo.NetworksMatcher.kernel.core.ComposedNode;
import playground.sergioo.NetworksMatcher.kernel.core.NetworksStep;
import playground.sergioo.NetworksMatcher.kernel.core.NodesMatching;
import playground.sergioo.NetworksMatcher.kernel.core.Region;

public class CrossingReductionStep extends NetworksStep {


	//Attributes

	private Set<NodesMatching> nodesMatchings;


	//Methods

	
	public CrossingReductionStep(Region region, Set<NodesMatching> nodesMatchings) {
		super("Crossing reduction step", region);
		this.nodesMatchings = nodesMatchings;
	}
	
	@Override
	public void process(Network oldNetworkA, Network oldNetworkB) {
		for(NodesMatching matching:nodesMatchings) {
			processNode(matching.getComposedNodeA(), networkA);
			processNode(matching.getComposedNodeB(), networkB);
		}
	}
	
	public void processNode(ComposedNode composedNode, Network network) {
		if(composedNode.getNodes().size()>1) {
			network.addNode(composedNode);
			for(Node node:composedNode.getNodes())
				network.removeNode(node.getId());
			for(Link link:composedNode.getInLinks().values()) {
				link.setToNode(composedNode);
				Node fromNode = network.getNodes().get(link.getFromNode().getId());
				if(fromNode==null) {
					fromNode = network.getNodes().get(((ComposedNode)link.getFromNode()).getContainerNode().getId());
					link.setFromNode(fromNode);
				}
				network.addLink(link);
			}
			for(Link link:composedNode.getOutLinks().values()) {
				link.setFromNode(composedNode);
				Node toNode = network.getNodes().get(link.getToNode().getId());
				if(toNode==null) {
					toNode = network.getNodes().get(((ComposedNode)link.getToNode()).getContainerNode().getId());
					link.setToNode(toNode);
				}
				network.addLink(link);		
			}
		}
	}

	
}
