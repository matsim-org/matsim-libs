package playground.christoph.knowledge.container;

import org.matsim.core.api.network.Network;
import org.matsim.core.population.PersonImpl;

public abstract class BasicNodeKnowledge implements NodeKnowledge{

	protected PersonImpl person;
	protected Network network;
	
	
	public PersonImpl getPerson()
	{
		return person;
	}

	
	public void setPerson(PersonImpl person)
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
	

}
