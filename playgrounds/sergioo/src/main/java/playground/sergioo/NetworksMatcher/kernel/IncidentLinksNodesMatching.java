package playground.sergioo.NetworksMatcher.kernel;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

import playground.sergioo.NetworksMatcher.kernel.core.ComposedLink;
import playground.sergioo.NetworksMatcher.kernel.core.MatchingComposedLink;
import playground.sergioo.NetworksMatcher.kernel.core.NodesMatching;

public class IncidentLinksNodesMatching extends NodesMatching {


	//Static attributes

	public static double minAngle;

	public static boolean rightSide = false;
	

	//Attributes

	private List<Integer> linksMatchingIndices;


	//Methods

	protected IncidentLinksNodesMatching(Set<Node> nodesA, Set<Node> nodesB) {
		super(nodesA, nodesB);
	}

	public List<Integer> getLinksMatchingIndices() {
		return linksMatchingIndices;
	}
	
	public boolean linksAnglesMatches() {
		if(linksAnglesMatches(composedNodeA.getInLinksList(), composedNodeB.getInLinksList(), new ArrayList<Integer>(), true) && linksAnglesMatches(composedNodeA.getOutLinksList(), composedNodeB.getOutLinksList(), new ArrayList<Integer>(), false)) {
			for(Link linkSmall:composedNodeA.getInLinks().values()) {
				((MatchingComposedLink)linkSmall).setToMatched(true);
				((MatchingComposedLink)linkSmall).setIncident(true);
			}
			for(Link linkSmall:composedNodeA.getOutLinks().values()) {
				((MatchingComposedLink)linkSmall).setFromMatched(true);
				((MatchingComposedLink)linkSmall).setIncident(true);
			}
			return true;
		}
		else
			return false;
	}

	private boolean linksAnglesMatches(List<Link> linksSmall, List<Link> linksBig, List<Integer> indicesBig, boolean in) {
		if(linksSmall.size() == 0) {
			int numChanges=0;
			for(int i=0; i<indicesBig.size()-1; i++)
				if(indicesBig.get(i)>indicesBig.get(i+1))
					numChanges++;
			if(indicesBig.get(indicesBig.size()-1)>indicesBig.get(0))
				numChanges++;
			if(numChanges==1) {
				linksMatchingIndices = indicesBig;
				for(int i:indicesBig) {
					if(composedNodeB.getId().equals(linksBig.get(i).getFromNode().getId()))
						((MatchingComposedLink)linksBig.get(i)).setFromMatched(true);
					else
						((MatchingComposedLink)linksBig.get(i)).setToMatched(true);
					((MatchingComposedLink)linksBig.get(i)).setIncident(true);
				}
				return true;
			}
			else
				return false;
		}
		else
			for(int b=0; b<linksBig.size(); b++) {
				double anglesDifference = Math.abs(((ComposedLink)linksSmall.get(0)).getAngle(in)-((ComposedLink)linksBig.get(b)).getAngle(in));
				if(anglesDifference>Math.PI)
					anglesDifference = 2*Math.PI - anglesDifference;
				if(!indicesBig.contains(b) && anglesDifference<minAngle) {
					List<Link> newLinksSmall = new ArrayList<Link>(linksSmall);
					newLinksSmall.remove(linksSmall.get(0));
					List<Integer> newIndicesBig = new ArrayList<Integer>(indicesBig);
					newIndicesBig.add(b);
					if(linksAnglesMatches(newLinksSmall, linksBig, newIndicesBig, in))
						return true;
				}
			}
		return false;
	}


}
