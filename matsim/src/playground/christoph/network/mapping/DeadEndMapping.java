package playground.christoph.network.mapping;

import java.util.List;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.core.v01.network.Node;

/*
 * Mapping of a Dead End (Link-Node).
 */
public class DeadEndMapping extends Mapping{

	private List<Object> input;
	private Node output;
	
	public DeadEndMapping(Id id, List<Object> input, Node output)
	{
		this.setId(id);
		this.input = input;
		this.output = output;
	}
	
	public List<Object> getInput()
	{
		return input;
	}
	
	public Node getOutput()
	{
		return output;
	}

	@Override
	public double getLength()
	{
		return 0.0;
	}
}
