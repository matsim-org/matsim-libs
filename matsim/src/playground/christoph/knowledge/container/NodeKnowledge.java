package playground.christoph.knowledge.container;

import java.util.Map;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.experimental.network.Link;
import org.matsim.core.api.experimental.network.Node;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.PersonImpl;

public interface NodeKnowledge {

	public PersonImpl getPerson();
	
	public void setPerson(PersonImpl person);
	
	public void setNetwork(NetworkLayer network);
	
	public NetworkLayer getNetwork();
	
	public boolean knowsNode(Node node);
	
	public boolean knowsLink(Link link);
	
	public Map<Id, Node> getKnownNodes();
}
