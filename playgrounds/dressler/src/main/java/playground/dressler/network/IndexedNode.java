package playground.dressler.network;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;

public class IndexedNode implements IndexedNodeI {
	public int n;
	public Node node;
	Collection<IndexedLinkI> inLinks;
	Collection<IndexedLinkI> outLinks;

	public IndexedNode(int n, Node node) {
		this.n = n;
		this.node = node;
		inLinks = new ArrayList<IndexedLinkI>();
		outLinks = new ArrayList<IndexedLinkI>();
	}
	
	@Override
	public Node getMatsimNode() {
		return node;
	}
		
	@Override
	public Collection<IndexedLinkI> getInLinks() { 
		return inLinks;
	}

	@Override
	public Collection<IndexedLinkI> getOutLinks() {
		return outLinks;
	}

	@Override
	public void addLink(IndexedLinkI link) {		
		if (this.equals(link.getFromNode())) {
			outLinks.add(link);
		} else if (this.equals(link.getToNode())) {
			inLinks.add(link);
		} else {
			throw new IllegalArgumentException("This link does not belong to this node!");
		}
	}

	@Override
	public boolean equals(IndexedNodeI other) {
		if (!(other instanceof IndexedNode)) return false;
		return this.n == ((IndexedNode) other).n;
	}

	@Override
	public int getIndex() {		
		return n;
	}

	@Override
	public Id getId() {
		return node.getId();
	}
}
