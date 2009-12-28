package playground.christoph.network.mapping;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

/*
 * 1:1 Mapping of a Link.
 */
public class SingleLinkMapping extends Mapping{

	private Link input;
	private Link output;
	
	public SingleLinkMapping(Id id, Link input, Link output)
	{
		this.setId(id);
		this.input = input;
		this.output = output;
	}
	
	@Override
	public Link getInput()
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
		if (input instanceof MappingInfo)
		{
			return ((MappingInfo) input).getDownMapping().getLength();
		}
		else return input.getLength();
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