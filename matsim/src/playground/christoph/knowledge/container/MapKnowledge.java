package playground.christoph.knowledge.container;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NodeImpl;

public class MapKnowledge extends BasicNodeKnowledge{

	protected boolean isWhiteList = true;
	
	private Map<Id, NodeImpl> nodes;
	
	public MapKnowledge()
	{
		this.nodes = new HashMap<Id, NodeImpl>();
	}
/*
	public MapKnowledge(Map<Id, NodeImpl> nodes)
	{
		int nodeCount = this.getNetwork().getNodes().size();
		
		// knows more than half of all nodes
		if (nodes.size() > nodeCount/2)
		{
			this.isWhiteList = false;
			
			Map<Id, NodeImpl> blackList = new HashMap<Id, NodeImpl>();
			blackList.putAll(this.network.getNodes());
			
			for (Id id : nodes.keySet())
			{
				blackList.remove(id);
			}
			this.nodes = blackList;
		}
		else
		{
			this.nodes = nodes;
		}
	}
*/	
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
	
	public void reset()
	{
		this.nodes = new HashMap<Id, NodeImpl>();
	}
	
	public void setKnownNodes(Map<Id, NodeImpl> nodes)
	{
		// If we have a network, we can check, whether using a Black- or WhiteList would be better.
		if (this.network != null)
		{
			int nodeCount = this.getNetwork().getNodes().size();
			
			// knows more than half of all nodes
			if (nodes.size() > nodeCount/2)
			{
				this.isWhiteList = false;
				
				Map<Id, NodeImpl> blackList = new HashMap<Id, NodeImpl>();
				blackList.putAll(this.network.getNodes());
				
				for (Id id : nodes.keySet())
				{
					blackList.remove(id);
				}
				this.nodes = blackList;
			}
			else
			{
				this.nodes = nodes;
			}
		}
		else
		{
			this.nodes = nodes;
		}
	}
}