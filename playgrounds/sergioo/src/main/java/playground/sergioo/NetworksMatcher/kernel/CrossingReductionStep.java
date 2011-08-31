package playground.sergioo.NetworksMatcher.kernel;

import java.util.Set;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

import playground.sergioo.NetworksMatcher.kernel.core.ComposedLink;
import playground.sergioo.NetworksMatcher.kernel.core.ComposedNetwork;
import playground.sergioo.NetworksMatcher.kernel.core.ComposedNode;
import playground.sergioo.NetworksMatcher.kernel.core.NetworksStep;
import playground.sergioo.NetworksMatcher.kernel.core.NodesMatching;
import playground.sergioo.NetworksMatcher.kernel.core.Region;

public class CrossingReductionStep extends NetworksStep {


	//Attributes

	private Set<NodesMatching> nodesMatchings;


	//Methods

	
	public CrossingReductionStep(Region region, Set<NodesMatching> nodesMatchings) {
		super(region);
		this.nodesMatchings = nodesMatchings;
	}

	@Override
	protected ComposedNetwork[] execute() {
		ComposedNetwork[] networks = new ComposedNetwork[] {networkA.clone(), networkB.clone()};
		for(NodesMatching matching:nodesMatchings) {
			ComposedNode composedNodeA = matching.getComposedNodeA();
			if(composedNodeA.getNodes().size()>1) {
				networks[0].addNode(composedNodeA);
				for(Node node:composedNodeA.getNodes()) {
					networks[0].removeNode(node.getId());
					for(Link link:node.getInLinks().values()) {
						boolean insideLink = false;
						for(Node node2:composedNodeA.getNodes())
							if(link.getFromNode().getId().equals(node2.getId()))
								insideLink = true;
						networks[0].removeLink(link.getId());
						if(!insideLink) {
							ComposedLink composedLink = new ComposedLink(link, networks[0]);
							Node fromNode = networks[0].getNodes().get(composedLink.getFromNode().getId());
							if(fromNode==null)
								fromNode = networks[0].getNodes().get(((ComposedNode)composedLink.getFromNode()).getContainerNode().getId());
							composedLink.setFromNode(fromNode);
							composedLink.setToNode(composedNodeA);
							composedLink.getLinks().addAll(((ComposedLink)link).getLinks());
							networks[0].addLink(composedLink);
						}
					}
					for(Link link:node.getOutLinks().values()) {
						boolean insideLink = false;
						for(Node node2:composedNodeA.getNodes())
							if(link.getToNode().getId().equals(node2.getId()))
								insideLink = true;
						networks[0].removeLink(link.getId());
						if(!insideLink) {
							ComposedLink composedLink = new ComposedLink(link, networks[0]);
							composedLink.setFromNode(composedNodeA);
							Node toNode = networks[0].getNodes().get(composedLink.getToNode().getId());
							if(toNode==null)
								toNode = networks[0].getNodes().get(((ComposedNode)composedLink.getToNode()).getContainerNode().getId());
							composedLink.setToNode(toNode);
							composedLink.getLinks().addAll(((ComposedLink)link).getLinks());
							networks[0].addLink(composedLink);
						}
					}			
				}
			}
			ComposedNode composedNodeB = matching.getComposedNodeB();
			if(composedNodeB.getNodes().size()>1) {
				networks[1].addNode(composedNodeB);
				for(Node node:composedNodeB.getNodes()) {
					networks[1].removeNode(node.getId());
					for(Link link:node.getInLinks().values()) {
						boolean insideLink = false;
						for(Node node2:composedNodeB.getNodes())
							if(link.getFromNode().getId().equals(node2.getId()))
								insideLink = true;
						networks[1].removeLink(link.getId());
						if(!insideLink) {
							ComposedLink composedLink = new ComposedLink(link, networks[1]);
							Node fromNode = networks[1].getNodes().get(composedLink.getFromNode().getId());
							if(fromNode==null)
								fromNode = networks[1].getNodes().get(((ComposedNode)composedLink.getFromNode()).getContainerNode().getId());
							composedLink.setFromNode(fromNode);
							composedLink.setToNode(composedNodeB);
							composedLink.getLinks().addAll(((ComposedLink)link).getLinks());
							networks[1].addLink(composedLink);
						}
					}
					for(Link link:node.getOutLinks().values()) {
						boolean insideLink = false;
						for(Node node2:composedNodeB.getNodes())
							if(link.getToNode().getId().equals(node2.getId()))
								insideLink = true;
						networks[1].removeLink(link.getId());
						if(!insideLink) {
							ComposedLink composedLink = new ComposedLink(link, networks[1]);
							composedLink.setFromNode(composedNodeB);
							Node toNode = networks[1].getNodes().get(composedLink.getToNode().getId());
							if(toNode==null)
								toNode = networks[1].getNodes().get(((ComposedNode)composedLink.getToNode()).getContainerNode().getId());
							composedLink.setToNode(toNode);
							composedLink.getLinks().addAll(((ComposedLink)link).getLinks());
							networks[1].addLink(composedLink);
						}
					}			
				}
			}
		}
		return networks;
	}

	
}
