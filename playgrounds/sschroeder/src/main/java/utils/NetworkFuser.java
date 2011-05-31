package utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;

public class NetworkFuser {
	
	static class LinkSet {
		private Link link1;
		private Link link2;
		public LinkSet(Link link1, Link link2) {
			super();
			this.link1 = link1;
			this.link2 = link2;
		}
		public Link getLink1() {
			return link1;
		}
		public Link getLink2() {
			return link2;
		}
		
	}
	
	private static Logger log = Logger.getLogger(NetworkFuser.class);
	
	private Network network;

	private LinkMerger linkMerger = new LinkMerger();
	
	private MergingConstraint mergingConstraint = new MergingConstraint();
	
	private List<Id> nodeIds;
	
	public NetworkFuser(Network network) {
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

	public void setLinkMerger(LinkMerger linkMerger) {
		this.linkMerger = linkMerger;
	}

	public void setMergingConstraint(MergingConstraint mergingConstraint) {
		this.mergingConstraint = mergingConstraint;
	}

	public void fuse(){
		log.info("start fusing");
		for(Id id : nodeIds){
			log.debug("node=" + id);
			Node node = network.getNodes().get(id);
			Set<Node> adjacentNodes = new HashSet<Node>();
			getAdjacentNodes(node, adjacentNodes);
			log.debug("#adjacentNodes=" + adjacentNodes.size());
			if(adjacentNodes.size() == 2){
				List<LinkSet> links2merge = new ArrayList<LinkSet>();
				getLinkSets(node,links2merge,adjacentNodes);
				boolean allLinksCanBeMerged = false;
				for(LinkSet lSet : links2merge){
					if(mergingConstraint.judge(lSet.link1, lSet.link2)){
						allLinksCanBeMerged = true;
					}
					else{
						allLinksCanBeMerged = false;
						break;
					}
				}
				if(allLinksCanBeMerged){
					for(LinkSet lSet : links2merge){
						Link link1 = lSet.link1;
						Link link2 = lSet.link2;
						Link newLink = linkMerger.merge(link1, link2, network);
						network.removeLink(link1.getId());
						log.debug("remove " + link1.getId());
						network.removeLink(link2.getId());
						log.debug("remove " + link2.getId());
						network.addLink(newLink);
						log.debug("links (" + link1.getId() + ", " + link2.getId() + ") are merged to " + newLink.getId());
					}
					network.removeNode(id);
					log.debug("remove node " + id);
				}
			}
			else{
				continue;
			}
		}
	}

	private void getLinkSets(Node node, List<LinkSet> links2merge, Set<Node> adjacentNodes) {
		Collection<Link> inLinks = (Collection<Link>) node.getInLinks().values();
		Collection<Link> outLinks = (Collection<Link>) node.getOutLinks().values();
		if(inLinks.size() == 1 && outLinks.size() == 1){
			links2merge.add(new LinkSet(inLinks.iterator().next(), outLinks.iterator().next()));
		}
		else if(inLinks.size() == 2 && outLinks.size() == 2){
			Object[] inLinkArr = inLinks.toArray();
			Object[] outLinkArr = outLinks.toArray();
			
			if(((Link)inLinkArr[0]).getFromNode().getId().equals(((Link)outLinkArr[0]).getToNode().getId())){
				links2merge.add(makeLinkSet(inLinkArr[0],outLinkArr[1]));
				links2merge.add(makeLinkSet(inLinkArr[1],outLinkArr[0]));
			}
			else{
				links2merge.add(makeLinkSet(inLinkArr[0],outLinkArr[0]));
				links2merge.add(makeLinkSet(inLinkArr[1],outLinkArr[1]));
			}
		}
	}

	private LinkSet makeLinkSet(Object link, Object link2) {
		return new LinkSet((Link)link,(Link)link2);
	}

	private void getAdjacentNodes(Node node, Set<Node> adjacentNodes) {
		for(Link l : node.getInLinks().values()){
			adjacentNodes.add(l.getFromNode());
		}
		for(Link l : node.getOutLinks().values()){
			adjacentNodes.add(l.getToNode());
		}	
	}
}
