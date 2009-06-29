package playground.christoph.knowledge.container;

import org.matsim.core.api.network.Network;
import org.matsim.core.api.population.Person;

public abstract class BasicNodeKnowledge implements NodeKnowledge{

	protected Person person;
	protected Network network;
	
	@Override
	public Person getPerson()
	{
		return person;
	}

	@Override
	public void setPerson(Person person)
	{
		this.person = person;
	}

	@Override
	public Network getNetwork()
	{
		return network;
	}
	
	@Override
	public void setNetwork(Network network)
	{
		this.network = network;
	}
	

}
