package playground.christoph.network.mapping;

import java.util.List;

import org.matsim.api.core.v01.Id;

/*
 * Every time Nodes and / or Links form a SubNetwork are
 * transformed this Transformation should be stored in a
 * Mapping Object.  
 */
public abstract class Mapping {

	public static int mappingId = 0;

	private Id id; 
	
	public void setId(Id id)
	{
		this.id = id;
	}
	
	public Id getId()
	{
		return id;
	}
	
	public abstract Object getInput();
	
	public abstract Object getOutput();
	
	public abstract double getLength();
	
	public abstract List<MappingInfo> getMappedObjects();
}
