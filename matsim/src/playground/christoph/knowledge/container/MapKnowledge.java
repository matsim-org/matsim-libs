package playground.christoph.knowledge.container;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NodeImpl;

public class MapKnowledge extends BasicNodeKnowledge{

	private Map<Id, NodeImpl> nodes;
	
	public MapKnowledge()
	{
		this.nodes = new HashMap<Id, NodeImpl>();
	}

	public MapKnowledge(Map<Id, NodeImpl> nodes)
	{
		this.nodes = nodes;
	}
	
	public void addNode(NodeImpl node)
	{
		nodes.put(node.getId(), node);
	}
	
	public void removeNode(NodeImpl node)
	{
		nodes.remove(node.getId());
	}
	
	
	public boolean knowsNode(NodeImpl node)
	{
		return nodes.containsKey(node.getId());
	}

	
	public boolean knowsLink(LinkImpl link)
	{
		// if no Map found or the Map is empty -> Person knows the entire network, return true
		if ( nodes == null ) return true;
		if ( nodes.size() == 0) return true;
				
		if ( this.knowsNode(link.getFromNode()) && this.knowsNode(link.getToNode()) ) return true;
		else return false;
	}
	
	
	public Map<Id, NodeImpl> getKnownNodes() 
	{
		return nodes;
	}
}