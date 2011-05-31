package utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.MatsimRandom;

import utils.NetworkFuser.LinkSet;


public class NetworkThinningForVis {
	
	private Network network;
	
	private List<Id> nodeIds;
	
	private MergingConstraint mergingConstraint = new MergingConstraint();
	
	public NetworkThinningForVis(Network network) {
		super();
		this.network = network;
		copyNodeIds();
	}

	private void copyNodeIds() {
		nodeIds = new ArrayList<Id>();
		for(Id id : network.getNodes().keySet()){
			nodeIds.add(makeId(id.toString()));
		}
		
	}
	
	private Id makeId(String id) {
		return new IdImpl(id);
	}
	
	public void thinOut(){
		for(Id nodeId : network.getNodes().keySet()){
			Node node = network.getNodes().get(nodeId);
			List<LinkSet> links2BeMerged = getLinks2BeMerged(node);
			if(!links2BeMerged.isEmpty()){
				for(LinkSet linkSet : links2BeMerged){
					if(mergingConstraint.judge(linkSet.getLink1(), linkSet.getLink2())){
						double randomFig = MatsimRandom.getRandom().nextDouble();
						if(randomFig < 0.5){
							network.removeLink(linkSet.getLink1().getId());
						}
						else{
							network.removeLink(linkSet.getLink2().getId());
						}
					}
				}
			}
		}
	}

	private List<LinkSet> getLinks2BeMerged(Node node) {
		Collection<Link> inLinks = (Collection<Link>) node.getInLinks().values();
		Collection<Link> outLinks = (Collection<Link>) node.getOutLinks().values();
		List<LinkSet> linkSets = new ArrayList<NetworkFuser.LinkSet>();
		for(Link inLink : inLinks){
			Link outLink = getProperOutLink(inLink,outLinks);
			if(outLink != null){
				linkSets.add(new LinkSet(inLink, outLink));
			}
		}
		return linkSets;
	}

	private Link getProperOutLink(Link inLink, Collection<Link> outLinks) {
		Id from = inLink.getFromNode().getId();
		Id to = inLink.getToNode().getId();
		for(Link outL : outLinks){
			Id fromOut = outL.getFromNode().getId();
			Id toOut = outL.getToNode().getId();
			if(from.equals(toOut) && to.equals(fromOut)){
				return outL;
			}
		}
		return null;
	}

}
