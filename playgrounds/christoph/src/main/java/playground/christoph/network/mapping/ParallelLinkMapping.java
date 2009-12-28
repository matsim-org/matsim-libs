package playground.christoph.network.mapping;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

/*
 * Mapping of n parallel Link to one Link.
 */
public class ParallelLinkMapping extends Mapping{

	private List<Link> input;
	private Link output;
	
	public ParallelLinkMapping(Id id, List<Link> input, Link output)
	{
		this.setId(id);
		this.input = input;
		this.output = output;
	}
	
	@Override
	public List<Link> getInput()
	{
		return input;
	}
	
	@Override
	public Link getOutput()
	{
		return output;
	}

	@Override
	public double getLength()
	{
		double length = 0.0;
		
		for (Link link : input)
		{
			if (link instanceof MappingInfo)
			{
				length = length + ((MappingInfo) link).getDownMapping().getLength();
			}
			else length = length + link.getLength();
		}
		return length / input.size();
	}
	
	@Override
	public List<MappingInfo> getMappedObjects()
	{
		List<MappingInfo> list = new ArrayList<MappingInfo>();

		for (Link link : input)
		{
			MappingInfo mappingInfo = (MappingInfo) link;
			list.addAll(mappingInfo.getDownMapping().getMappedObjects());
		}
		return list;
	}
}
