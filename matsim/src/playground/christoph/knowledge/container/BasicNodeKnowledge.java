package playground.christoph.knowledge.container;

import org.matsim.core.network.NetworkLayer;
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
	

}
