package playground.christoph.knowledge.container;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Node;

public class MapKnowledge extends BasicNodeKnowledge{

	private Map<Id, Node> nodes;
	
	public MapKnowledge()
	{
		this.nodes = new HashMap<Id, Node>();
	}

	public MapKnowledge(Map<Id, Node> nodes)
	{
		this.nodes = nodes;
	}
	
	public void addNode(Node node)
	{
		nodes.put(node.getId(), node);
	}
	
	public void removeNode(Node node)
	{
		nodes.remove(node.getId());
	}
	
	
	public boolean knowsNode(Node node)
	{
		return nodes.containsKey(node.getId());
	}

	
	public boolean knowsLink(Link link)
	{
		// if no Map found or the Map is empty -> Person knows the entire network, return true
		if ( nodes == null ) return true;
		if ( nodes.size() == 0) return true;
				
		if ( this.knowsNode(link.getFromNode()) && this.knowsNode(link.getToNode()) ) return true;
		else return false;
	}
	
	
	public Map<Id, Node> getKnownNodes() 
	{
		return nodes;
	}
}