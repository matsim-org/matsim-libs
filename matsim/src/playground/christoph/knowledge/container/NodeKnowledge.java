package playground.christoph.knowledge.container;

import java.util.Map;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.PersonImpl;

public interface NodeKnowledge {

	public PersonImpl getPerson();
	
	public void setPerson(PersonImpl person);
	
	public void setNetwork(NetworkLayer network);
		
	public NetworkLayer getNetwork();
	
	public boolean knowsNode(NodeImpl node);
	
	public boolean knowsLink(LinkImpl link);
	
	public Map<Id, NodeImpl> getKnownNodes();
	
	public void setKnownNodes(Map<Id, NodeImpl> nodes);
	
	public void clearKnowledge();
}
