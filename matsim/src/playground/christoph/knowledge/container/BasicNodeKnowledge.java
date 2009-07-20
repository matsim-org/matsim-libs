package playground.christoph.knowledge.container;

import java.util.Map;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.PersonImpl;

public abstract class BasicNodeKnowledge implements NodeKnowledge{

	protected PersonImpl person;
	protected NetworkLayer network;
	
	public PersonImpl getPerson()
	{
		return person;
	}

	public void setPerson(PersonImpl person)
	{
		this.person = person;
	}

	public NetworkLayer getNetwork()
	{
		return network;
	}
	
	public void setNetwork(NetworkLayer network)
	{
		this.network = network;
	}
	
	public abstract void setKnownNodes(Map<Id, NodeImpl> nodes);
	
	public abstract Map<Id, NodeImpl> getKnownNodes();
}
