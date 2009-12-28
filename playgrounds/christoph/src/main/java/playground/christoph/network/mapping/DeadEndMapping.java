package playground.christoph.network.mapping;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;

/*
 * Mapping of a Dead End (Link-Node).
 */
public class DeadEndMapping extends Mapping{

	private List<MappingInfo> input;
	private Node output;
	
	public DeadEndMapping(Id id, List<MappingInfo> input, Node output)
	{
		this.setId(id);
		this.input = input;
		this.output = output;
	}
	
	@Override
	public List<MappingInfo> getInput()
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
		return 0.0;
	}
	
	@Override
	public List<MappingInfo> getMappedObjects()
	{
		List<MappingInfo> list = new ArrayList<MappingInfo>();

		for (MappingInfo mappingInfo : input)
		{
			list.addAll(mappingInfo.getDownMapping().getMappedObjects());
		}
		return list;
	}
}
