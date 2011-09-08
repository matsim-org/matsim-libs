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


	//Attributes

	private List<Integer> linksMatchingIndices;

	private boolean smallABigB = true;


	//Methods

	protected IncidentLinksNodesMatching(Set<Node> nodesA, Set<Node> nodesB) {
		super(nodesA, nodesB);
		composedNodeA.setIncidentLinks();
		composedNodeB.setIncidentLinks();
	}

	public List<Integer> getLinksMatchingIndices() {
		return linksMatchingIndices;
	}

	public boolean isSmallABigB() {
		return smallABigB;
	}

	public boolean linksAnglesMatches() {
		List<Link> linksSmall = composedNodeA.getIncidentLinks();
		List<Link> linksBig = composedNodeB.getIncidentLinks();
		if(linksSmall.size()>linksBig.size()) {
			smallABigB = false;
			List<Link> linksTemp = linksSmall;
			linksSmall = linksBig;
			linksBig = linksTemp;
		}
		for(Link linkBig:linksBig)
			((MatchingComposedLink)linkBig).setIncident(true);
		return linksAnglesMatches(linksSmall, linksBig, new ArrayList<Integer>());
	}

	private boolean linksAnglesMatches(List<Link> linksSmall, List<Link> linksBig, List<Integer> indicesBig) {
		if(linksSmall.size() == 0) {
			int numChanges=0;
			for(int i=0; i<indicesBig.size()-1; i++)
				if(indicesBig.get(i)>indicesBig.get(i+1))
					numChanges++;
			if(indicesBig.get(indicesBig.size()-1)>indicesBig.get(0))
				numChanges++;
			if(numChanges==1) {
				linksMatchingIndices = indicesBig;
				for(int i:indicesBig)
					if(smallABigB)
						if(composedNodeB.getId().equals(linksBig.get(i).getFromNode().getId()))
							((MatchingComposedLink)linksBig.get(i)).setFromMatched(true);
						else
							((MatchingComposedLink)linksBig.get(i)).setToMatched(true);
					else
						if(composedNodeA.getId().equals(linksBig.get(i).getFromNode().getId()))
							((MatchingComposedLink)linksBig.get(i)).setFromMatched(true);
						else
							((MatchingComposedLink)linksBig.get(i)).setToMatched(true);
				return true;
			}
			else
				return false;
		}
		else
			for(int b=0; b<linksBig.size(); b++) {
				double anglesDifference = Math.abs(((ComposedLink)linksSmall.get(0)).getAngle()-((ComposedLink)linksBig.get(b)).getAngle());
				if(anglesDifference>Math.PI)
					anglesDifference = 2*Math.PI - anglesDifference;
				if(!indicesBig.contains(b) && anglesDifference<minAngle) {
					List<Link> newLinksSmall = new ArrayList<Link>(linksSmall);
					if(smallABigB)
						if(composedNodeA.getId().equals(linksSmall.get(0).getFromNode().getId()))
							((MatchingComposedLink)linksSmall.get(0)).setFromMatched(true);
						else
							((MatchingComposedLink)linksSmall.get(0)).setToMatched(true);
					else
						if(composedNodeB.getId().equals(linksSmall.get(0).getFromNode().getId()))
							((MatchingComposedLink)linksSmall.get(0)).setFromMatched(true);
						else
							((MatchingComposedLink)linksSmall.get(0)).setToMatched(true);
					((MatchingComposedLink)linksSmall.get(0)).setIncident(true);
					newLinksSmall.remove(linksSmall.get(0));
					List<Integer> newIndicesBig = new ArrayList<Integer>(indicesBig);
					newIndicesBig.add(b);
					if(linksAnglesMatches(newLinksSmall, linksBig, newIndicesBig))
						return true;
				}
			}
		return false;
	}


}
