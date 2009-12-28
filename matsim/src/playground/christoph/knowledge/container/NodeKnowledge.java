package playground.christoph.knowledge.container;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;

public interface NodeKnowledge {

	public Person getPerson();
	
	public void setPerson(Person person);
	
	public void setNetwork(Network network);
		
	public Network getNetwork();
	
	public boolean knowsNode(Node node);
	
	public boolean knowsLink(Link link);
	
	public Map<Id, Node> getKnownNodes();
	
	public void setKnownNodes(Map<Id, Node> nodes);
	
	public void clearKnowledge();
}
