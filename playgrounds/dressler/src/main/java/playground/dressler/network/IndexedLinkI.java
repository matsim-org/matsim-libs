package playground.dressler.network;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public interface IndexedLinkI {
	
	
	IndexedNodeI getFromNode();
	IndexedNodeI getToNode();
	
	public int getIndex();
	
	public boolean equals(IndexedLinkI other);
		
	Link getMatsimLink();
	
	Id getId();
}
