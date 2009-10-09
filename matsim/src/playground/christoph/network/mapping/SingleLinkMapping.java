package playground.christoph.network.mapping;

import org.matsim.api.basic.v01.Id;
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
	
	public Link getInput()
	{
		return input;
	}
	
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
}
