package playground.christoph.network.mapping;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

/*
 * Mapping of a Chain Link-Node-Link-...-Node-Link
 * to a Link.
 */
public class ChainMapping extends Mapping{

	private List<MappingInfo> input;
	private Link output;
	
	public ChainMapping(Id id, List<MappingInfo> input, Link output)
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
	public Link getOutput()
	{
		return output;
	}

	@Override
	public double getLength()
	{
		double length = 0.0;
		for (MappingInfo mappingInfo : input)
		{
			length = length + mappingInfo.getDownMapping().getLength();
		}
		return length;
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
