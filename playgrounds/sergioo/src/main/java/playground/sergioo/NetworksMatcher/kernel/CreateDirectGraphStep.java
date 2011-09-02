package playground.sergioo.NetworksMatcher.kernel;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

import playground.sergioo.NetworksMatcher.kernel.core.ComposedLink;
import playground.sergioo.NetworksMatcher.kernel.core.MatchingComposedNetwork;
import playground.sergioo.NetworksMatcher.kernel.core.ComposedNode;
import playground.sergioo.NetworksMatcher.kernel.core.MatchingComposedLink;
import playground.sergioo.NetworksMatcher.kernel.core.NetworksStep;
import playground.sergioo.NetworksMatcher.kernel.core.Region;
import playground.sergioo.NetworksMatcher.kernel.core.ComposedNode.Types;

public class CreateDirectGraphStep extends NetworksStep {

	
	
	//Attributes

	//Methods

	public CreateDirectGraphStep(Region region) {
		super(region);
		
	}

	@Override
	public MatchingComposedNetwork[] execute() {
		return new MatchingComposedNetwork[] {createDirectedGraph(networkA), createDirectedGraph(networkB)};
	}

	private MatchingComposedNetwork createDirectedGraph(Network network) {
		MatchingComposedNetwork directedGraph = new MatchingComposedNetwork();
		for(Node node:network.getNodes().values()) {
			Set<Node> nodes = new HashSet<Node>();
			nodes.add(node);
			directedGraph.addNode(new ComposedNode(nodes));
		}
		for(Link link:network.getLinks().values()) {
			MatchingComposedLink composedLink = new MatchingComposedLink(link, directedGraph);
			composedLink.setFromNode(directedGraph.getNodes().get(composedLink.getFromNode().getId()));
			composedLink.setToNode(directedGraph.getNodes().get(composedLink.getToNode().getId()));
			composedLink.getLinks().add(link);
			directedGraph.addLink(composedLink);
		}
		setNodeTypes(directedGraph);
		directedGraph = nodeReductionProcess(directedGraph);
		return directedGraph;
	}
	
	private void setNodeTypes(Network directedGraph) {
		for(Node node:directedGraph.getNodes().values())
			((ComposedNode)node).setType();
	}

	private MatchingComposedNetwork nodeReductionProcess(Network directedGraph) {
		MatchingComposedNetwork reducedDirectedGraph = new MatchingComposedNetwork();
		for(Node node:directedGraph.getNodes().values()) {
			ComposedNode newNode = new ComposedNode(((ComposedNode)node).getNodes());
			newNode.setType(((ComposedNode)node).getType());
			reducedDirectedGraph.addNode(newNode);
		}
		for(Link link:directedGraph.getLinks().values()) {
			MatchingComposedLink composedLink = new MatchingComposedLink(link, reducedDirectedGraph);
			composedLink.setFromNode(reducedDirectedGraph.getNodes().get(composedLink.getFromNode().getId()));
			composedLink.setToNode(reducedDirectedGraph.getNodes().get(composedLink.getToNode().getId()));
			composedLink.getLinks().addAll(((ComposedLink)link).getLinks());
			reducedDirectedGraph.addLink(composedLink);
		}
		for(Node node:directedGraph.getNodes().values())
			switch(((ComposedNode)node).getType()) {
			case EMPTY:
				reducedDirectedGraph.removeNode(node.getId());
				break;
			case ONE_WAY_PASS:
				ComposedLink firstNext = (ComposedLink) reducedDirectedGraph.getNodes().get(node.getId()).getOutLinks().values().iterator().next();
				ComposedLink firstPrevious = (ComposedLink) reducedDirectedGraph.getNodes().get(node.getId()).getInLinks().values().iterator().next();
				Node next=firstNext.getToNode();
				Node previous=firstPrevious.getFromNode();
				while(((ComposedNode)next).getType().equals(Types.ONE_WAY_PASS) && !next.equals(firstPrevious.getToNode()))
					next = next.getOutLinks().values().iterator().next().getToNode();
				while(((ComposedNode)previous).getType().equals(Types.ONE_WAY_PASS) && !previous.equals(firstNext.getFromNode()))
					previous = previous.getInLinks().values().iterator().next().getFromNode();
				if(!next.equals(previous)) {
					MatchingComposedLink composedLink = new MatchingComposedLink(firstNext.getId(), firstPrevious.getFromNode(), firstNext.getToNode(), reducedDirectedGraph);
					composedLink.getLinks().addAll(firstNext.getLinks());
					composedLink.getLinks().addAll(firstPrevious.getLinks());
					reducedDirectedGraph.removeNode(node.getId());
					reducedDirectedGraph.addLink(composedLink);
				}
				break;
			case TWO_WAY_PASS:
				Iterator<ComposedLink> outLinksIterator = (Iterator<ComposedLink>)reducedDirectedGraph.getNodes().get(node.getId()).getOutLinks().values().iterator();
				ComposedLink firstNextA = (ComposedLink) outLinksIterator.next();
				ComposedLink firstNextB = (ComposedLink) outLinksIterator.next();
				Node nextA=firstNextA.getToNode();
				Node previousA=firstNextA.getFromNode();
				Node nextB=firstNextB.getToNode();
				Node previousB=firstNextA.getFromNode();
				while(((ComposedNode)nextA).getType().equals(Types.TWO_WAY_PASS) && !nextA.equals(firstNextB.getFromNode())){
					Node oldNextA = nextA;
					outLinksIterator = (Iterator<ComposedLink>) nextA.getOutLinks().values().iterator();
					nextA = outLinksIterator.next().getToNode();
					if(nextA.equals(previousA))
						nextA = outLinksIterator.next().getToNode();
					previousA = oldNextA;
				}
				while(((ComposedNode)nextB).getType().equals(Types.TWO_WAY_PASS) && !nextB.equals(firstNextA.getFromNode())) {
					Node oldNextB = nextB;
					outLinksIterator = (Iterator<ComposedLink>) nextB.getOutLinks().values().iterator();
					nextB = outLinksIterator.next().getToNode();
					if(nextB.equals(previousB))
						nextB = outLinksIterator.next().getToNode();
					previousB = oldNextB;
				}
				if(!nextA.equals(nextB)) {
					Iterator<ComposedLink> inLinksIterator = (Iterator<ComposedLink>)reducedDirectedGraph.getNodes().get(node.getId()).getInLinks().values().iterator();
					ComposedLink firstPreviousA = (ComposedLink) inLinksIterator.next();
					ComposedLink firstPreviousB = (ComposedLink) inLinksIterator.next();
					MatchingComposedLink composedLinkA = new MatchingComposedLink(firstNextA.getId(), firstNextB.getToNode(), firstNextA.getToNode(), reducedDirectedGraph);
					composedLinkA.getLinks().addAll(firstNextA.getLinks());
					composedLinkA.getLinks().addAll((firstPreviousA.getFromNode().equals(firstNextA.getToNode())?firstPreviousB:firstPreviousA).getLinks());
					MatchingComposedLink composedLinkB = new MatchingComposedLink(firstNextB.getId(), firstNextA.getToNode(), firstNextB.getToNode(), reducedDirectedGraph);
					composedLinkB.getLinks().addAll(firstNextB.getLinks());
					composedLinkB.getLinks().addAll((firstPreviousB.getFromNode().equals(firstNextB.getToNode())?firstPreviousA:firstPreviousB).getLinks());
					reducedDirectedGraph.removeNode(node.getId());
					reducedDirectedGraph.addLink(composedLinkA);
					reducedDirectedGraph.addLink(composedLinkB);
				}
				break;
			}
		return reducedDirectedGraph;
	}
	
}
