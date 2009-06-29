package playground.christoph.knowledge.container;

import java.util.Map;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Network;
import org.matsim.core.api.network.Node;
import org.matsim.core.api.population.Person;

public interface NodeKnowledge {

	public Person getPerson();
	
	public void setPerson(Person person);
	
	public void setNetwork(Network network);
	
	public Network getNetwork();
	
	public boolean knowsNode(Node node);
	
	public boolean knowsLink(Link link);
	
	public Map<Id, Node> getKnownNodes();
}
