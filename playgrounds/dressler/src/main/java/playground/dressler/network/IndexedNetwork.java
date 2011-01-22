package playground.dressler.network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

public class IndexedNetwork implements IndexedNetworkI {

	ArrayList<IndexedNodeI> nodes;
	ArrayList<IndexedLinkI> links;

	// map nodes to indices
	HashMap<Id, Integer> nodeNames = new LinkedHashMap<Id, Integer>();
	
	// map links to indices
	HashMap<Id, Integer> linkNames = new LinkedHashMap<Id, Integer>();


	public IndexedNetwork(Network network) {
		nodes = new ArrayList<IndexedNodeI>(network.getNodes().size());
		links = new ArrayList<IndexedLinkI>(network.getLinks().size());
		
		int c = 0;
		for (Node node: network.getNodes().values()) {
			IndexedNode iN = new IndexedNode(c, node);
			
			nodes.add(iN);
			nodeNames.put(node.getId(), c);
			
			c++;
		}
		
		c = 0;
		for (Link link : network.getLinks().values()) {
			IndexedNodeI from = nodes.get(nodeNames.get(link.getFromNode().getId()));
			IndexedNodeI to = nodes.get(nodeNames.get(link.getToNode().getId()));
			IndexedLink iL = new IndexedLink(c, link, from ,to);			
			
			links.add(iL);
			linkNames.put(link.getId(), c);
			
			from.addLink(iL);
			to.addLink(iL);
			
			c++;			
		}
		
	}

	@Override
	public int getLargestIndexLinks() {
		return links.size() - 1;
	}

	@Override
	public int getLargestIndexNodes() {
		return nodes.size() - 1;
	}

	@Override
	public Collection<IndexedLinkI> getLinks() {
		return links;
	}

	@Override
	public Collection<IndexedNodeI> getNodes() {
		return nodes;
	}

	@Override
	public IndexedNodeI getIndexedNode(Node node) {
		if (node == null) return null;
		
		Integer i = nodeNames.get(node.getId());
		if (i == null) return null;
		
		return nodes.get(i);
	}

	@Override
	public IndexedLinkI getIndexedLink(Link link) {
		if (link == null) return null;
		
		Integer i = linkNames.get(link.getId());
		if (i == null) return null;
		
		return links.get(i);
	}

	@Override
	public int getIndex(Node node) {
		return nodeNames.get(node.getId());		
	}

	@Override
	public int getIndex(Link link) {
		return linkNames.get(link.getId());
	}

	@Override
	public IndexedLinkI getIndexedLink(Id id) {
		Integer i = linkNames.get(id);
		if (i == null) return null;
		
		return links.get(i);
	}

	@Override
	public IndexedNodeI getIndexedNode(Id id) {
		Integer i = nodeNames.get(id);
		if (i == null) return null;
		
		return nodes.get(i);
	}

	
}
