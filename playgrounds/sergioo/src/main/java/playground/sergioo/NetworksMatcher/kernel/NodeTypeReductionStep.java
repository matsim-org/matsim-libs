package playground.sergioo.NetworksMatcher.kernel;

import java.util.Iterator;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

import playground.sergioo.NetworksMatcher.kernel.core.ComposedLink;
import playground.sergioo.NetworksMatcher.kernel.core.ComposedNode;
import playground.sergioo.NetworksMatcher.kernel.core.MatchingComposedLink;
import playground.sergioo.NetworksMatcher.kernel.core.NetworksStep;
import playground.sergioo.NetworksMatcher.kernel.core.Region;
import playground.sergioo.NetworksMatcher.kernel.core.ComposedNode.Types;

public class NodeTypeReductionStep extends NetworksStep {

	
	
	//Attributes

	//Methods

	public NodeTypeReductionStep(Region region) {
		super("Node type reduction step", region);
		
	}

	@Override
	protected void process(Network oldNetworkA, Network oldNetworkB) {
		reductionProcess(networkA, oldNetworkA);
		reductionProcess(networkB, oldNetworkB);
	}

	private void reductionProcess(Network reducedDirectedGraph, Network directedGraph) {
		for(Node node:directedGraph.getNodes().values())
			switch(((ComposedNode)reducedDirectedGraph.getNodes().get(node.getId())).getType()) {
			case EMPTY:
				reducedDirectedGraph.removeNode(node.getId());
				break;
			case ONE_WAY_PASS:
				Link firstNext = ((ComposedNode)reducedDirectedGraph.getNodes().get(node.getId())).getOutLinks().values().iterator().next();
				Link firstPrevious = ((ComposedNode)reducedDirectedGraph.getNodes().get(node.getId())).getInLinks().values().iterator().next();
				Node next=firstNext.getToNode();
				Node previous=firstPrevious.getFromNode();
				while(((ComposedNode)next).getType().equals(Types.ONE_WAY_PASS) && !next.equals(firstPrevious.getToNode()))
					next = ((ComposedNode)next).getOutLinks().values().iterator().next().getToNode();
				while(((ComposedNode)previous).getType().equals(Types.ONE_WAY_PASS) && !previous.equals(firstNext.getFromNode()))
					previous = ((ComposedNode)previous).getInLinks().values().iterator().next().getFromNode();
				if(!next.equals(previous)) {
					MatchingComposedLink composedLink = new MatchingComposedLink(firstNext.getId(), firstPrevious.getFromNode(), firstNext.getToNode(), reducedDirectedGraph);
					composedLink.getLinks().addAll(((ComposedLink)firstNext).getLinks());
					composedLink.getLinks().addAll(((ComposedLink)firstPrevious).getLinks());
					reducedDirectedGraph.removeNode(node.getId());
					reducedDirectedGraph.addLink(composedLink);
				}
				break;
			case TWO_WAY_PASS:
				Iterator<Link> outLinksIterator = (Iterator<Link>) reducedDirectedGraph.getNodes().get(node.getId()).getOutLinks().values().iterator();
				ComposedLink firstNextA = (ComposedLink) outLinksIterator.next();
				ComposedLink firstNextB = (ComposedLink) outLinksIterator.next();
				Node nextA=firstNextA.getToNode();
				Node previousA=firstNextA.getFromNode();
				Node nextB=firstNextB.getToNode();
				Node previousB=firstNextA.getFromNode();
				while(((ComposedNode)nextA).getType().equals(Types.TWO_WAY_PASS) && !nextA.equals(firstNextB.getFromNode())){
					Node oldNextA = nextA;
					outLinksIterator = (Iterator<Link>) nextA.getOutLinks().values().iterator();
					nextA = outLinksIterator.next().getToNode();
					if(nextA.equals(previousA))
						nextA = outLinksIterator.next().getToNode();
					previousA = oldNextA;
				}
				while(((ComposedNode)nextB).getType().equals(Types.TWO_WAY_PASS) && !nextB.equals(firstNextA.getFromNode())) {
					Node oldNextB = nextB;
					outLinksIterator = (Iterator<Link>) nextB.getOutLinks().values().iterator();
					nextB = outLinksIterator.next().getToNode();
					if(nextB.equals(previousB))
						nextB = outLinksIterator.next().getToNode();
					previousB = oldNextB;
				}
				if(!nextA.equals(nextB)) {
					Iterator<Link> inLinksIterator = (Iterator<Link>) reducedDirectedGraph.getNodes().get(node.getId()).getInLinks().values().iterator();
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
	}
	
	
}
