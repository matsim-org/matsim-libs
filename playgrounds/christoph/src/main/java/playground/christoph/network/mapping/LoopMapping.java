package playground.christoph.network.mapping;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

/*
 * Mapping of a Chain Node-Link-...-Node-Link
 * to a Node. The last Link is connected to the
 * first Node again.
 * 
 * [TODO] find a way to estimate the Length of
 * the Mapped Loop. Problem: How much of the
 * Nodes does an Agent have to drive to get from
 * a mapped Node to another? 
 */
public class LoopMapping extends Mapping{

	private List<MappingInfo> input;
	private Node output;
	
	private double length = 0.0;
	
	public LoopMapping(Id id, List<MappingInfo> input, Node output)
	{
		this.setId(id);
		this.input = input;
		this.output = output;
		
		calculateLength();
	}
	
	/*
	 * Estimate the length:
	 * We assume that an Agent drives typically
	 * half of the Length of the Loop.
	 */
	private void calculateLength()
	{
		List<MappingInfo> copy = new ArrayList<MappingInfo>();
		copy.addAll(input);
		copy.add(input.get(0));
		
		Iterator<MappingInfo> iter = copy.iterator();
		Node n1 = (Node) iter.next();
		while (iter.hasNext())
		{
			Node n2 = (Node) iter.next();
			
			for (Link link : n1.getInLinks().values())
			{
				if (link.getFromNode().equals(n2))
				{
					length = length + link.getLength();
					break;
				}
			}
			for (Link link : n2.getInLinks().values())
			{
				if (link.getFromNode().equals(n1))
				{
					length = length + link.getLength();
					break;
				}
			}
			
			n1 = n2;
		}
			
		// Divide by two to get the mean of forward and backward loop
		length = length / 2;
		
		/*
		 * If the input Nodes have also a LoopMapping, they also
		 * have a length > 0.
		 */
		for (MappingInfo mappingInfo : input)
		{
			length = length + mappingInfo.getDownMapping().getLength();
		}
		
		//Divide by two to get half of the mean loop length
		length = length / 2;
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
