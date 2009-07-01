package playground.christoph.knowledge.container;

import java.util.Map;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Network;
import org.matsim.core.api.network.Node;
import org.matsim.core.population.PersonImpl;

public interface NodeKnowledge {

	public PersonImpl getPerson();
	
	public void setPerson(PersonImpl person);
	
	public void setNetwork(Network network);
	
	public Network getNetwork();
	
	public boolean knowsNode(Node node);
	
	public boolean knowsLink(Link link);
	
	public Map<Id, Node> getKnownNodes();
}
