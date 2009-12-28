package playground.christoph.knowledge.container;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;

public abstract class BasicNodeKnowledge implements NodeKnowledge{

	protected Person person;
	protected Network network;
	
	public Person getPerson()
	{
		return person;
	}

	public void setPerson(Person person)
	{
		this.person = person;
	}

	public Network getNetwork()
	{
		return network;
	}
	
	public void setNetwork(Network network)
	{
		this.network = network;
	}
	
	public abstract void setKnownNodes(Map<Id, Node> nodes);
	
	public abstract Map<Id, Node> getKnownNodes();
}
