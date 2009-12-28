package playground.christoph.network.transformation;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;

public abstract class TransformationImpl implements Transformation{
	
	/*
	 * for example:
	 * Map<LinkId, Link>
	 * Map<StartNodeId, List<Node>> 
	 */
	protected Map<Id, ?> transformableNodes;
	
	/*
	 * for example:
	 * Map<LinkId, Link>
	 * Map<StartNodeId, List<Link>> 
	 */
	protected Map<Id, ?> transformableLinks;
	
	protected Network network;
	
	protected static int idCounter;
	
	protected boolean printStatistics = false;
		
	public void printStatistics(boolean value)
	{
		this.printStatistics = value;
	}
	
	public boolean printStatistics()
	{
		return this.printStatistics;
	}
	
//	public Map<Id, Node> getTransformableNodes()
//	{
//		return this.transformableNodes;
//	}
//	
//	public Map<Id, List<Link>> getTransformableLinks()
//	{
//		return this.transformableLinks;
//	}
}
