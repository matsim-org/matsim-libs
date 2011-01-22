package playground.dressler.network;

import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;

public interface IndexedNodeI {
		
	Collection<IndexedLinkI> getInLinks();
	Collection<IndexedLinkI> getOutLinks();
	
	public void addLink(IndexedLinkI link);
	public int getIndex();
	
	public boolean equals(IndexedNodeI other);
	
	Node getMatsimNode();
	
	Id getId();
}
