package playground.dressler.network;

import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

public interface IndexedNetworkI {
	Collection<IndexedNodeI> getNodes();
	Collection<IndexedLinkI> getLinks();
	
	//public void buildFromMatsim(Network network);
	
	public int getLargestIndexNodes();
	public int getLargestIndexLinks();
	
	public IndexedNodeI getIndexedNode(Node node);
	public IndexedNodeI getIndexedNode(Id id);
	
	public IndexedLinkI getIndexedLink(Link link);
	public IndexedLinkI getIndexedLink(Id id);
	
	public int getIndex(Node node);
	public int getIndex(Link link);
		
}
