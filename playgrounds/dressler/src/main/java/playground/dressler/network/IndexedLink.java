package playground.dressler.network;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public class IndexedLink implements IndexedLinkI {
	public int n;
	
	public IndexedNodeI from;
	public IndexedNodeI to;
	
	public Link link;
	
	public IndexedLink(int n, Link link, IndexedNodeI from, IndexedNodeI to) {
		this.n = n;
		this.link = link;
		this.from = from;
		this.to = to;
	}

	@Override
	public boolean equals(IndexedLinkI other) {
		if (!(other instanceof IndexedLink)) return false;
		return this.n == ((IndexedLink) other).n;
	}

	@Override
	public IndexedNodeI getFromNode() {
		return from;
	}
	
	@Override
	public IndexedNodeI getToNode() {
		return to;
	}

	@Override
	public int getIndex() {		
		return n;
	}

	@Override
	public Link getMatsimLink() {
		return link;
	}

	@Override
	public Id getId() { 
		return link.getId();
	}
}
