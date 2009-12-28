package playground.christoph.network.mapping;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;

/*
 * 1:1 Mapping of a Node.
 */
public class SingleNodeMapping extends Mapping{

	private Node input;
	private Node output;
	
	public SingleNodeMapping(Id id, Node input, Node output)
	{
		this.setId(id);
		this.input = input;
		this.output = output;
	}
	
	@Override
	public Node getInput()
	{
		return input;
	}
	
	@Override
	public Node getOutput()
	{
		return output;
	}
	
	@Override
	public double getLength()
	{
		if (input instanceof MappingInfo)
		{
			return ((MappingInfo) input).getDownMapping().getLength();
		}
		// else: It is a single Node with Length 0
		else return 0.0;
	}
	
	@Override
	public List<MappingInfo> getMappedObjects()
	{
		if (input instanceof MappingInfo)
		{
			return ((MappingInfo) input).getDownMapping().getMappedObjects();
		}
//		else return null;
		else 
		{
			List<MappingInfo> list = new ArrayList<MappingInfo>();
			list.add((MappingInfo) output);
			return list;
		}
	}
}
