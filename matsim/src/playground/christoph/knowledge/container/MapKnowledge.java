package playground.christoph.knowledge.container;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NodeImpl;

public class MapKnowledge extends BasicNodeKnowledge{

	private static final Logger log = Logger.getLogger(MapKnowledge.class);
	
	public boolean isWhiteList = true;
	
	private Map<Id, NodeImpl> nodes;
	
	public MapKnowledge()
	{
		this.nodes = new HashMap<Id, NodeImpl>();
	}
	
	public void addNode(NodeImpl node)
	{
		if (isWhiteList) nodes.put(node.getId(), node);
		else nodes.remove(node.getId());
	}
	
	public void removeNode(NodeImpl node)
	{
		if (isWhiteList) nodes.remove(node.getId());
		else nodes.put(node.getId(), node);
	}
		
	public boolean knowsNode(NodeImpl node)
	{
		boolean knowsNode = nodes.containsKey(node.getId());
		if (isWhiteList) return knowsNode;
		else return !knowsNode;
	}
	
	public boolean knowsLink(LinkImpl link)
	{
		// if no Map found or the Map is empty -> Person knows the entire network, return true
		if ( nodes == null ) return true;
		if ( nodes.size() == 0) return !isWhiteList;
				
		if ( this.knowsNode(link.getFromNode()) && this.knowsNode(link.getToNode()) ) return true;
		else return false;
	}
	
	public synchronized Map<Id, NodeImpl> getKnownNodes() 
	{
		if (isWhiteList) return nodes;
		else
		{
			Map<Id, NodeImpl> invertedNodes = new HashMap<Id, NodeImpl>();
			
			invertedNodes.putAll(this.network.getNodes());
			
			for(NodeImpl node : nodes.values())
			{
				invertedNodes.remove(node.getId());
			}
			
			return invertedNodes;
		}
	}
	
	/*
	 * Returns the Map of Nodes that describe the Knowledge.
	 * Please note: this Map could be a White- or a BlackList.
	 * To get always a WhiteList please use getKnownNodes()
	 */
	protected Map<Id, NodeImpl> getNodes()
	{
		return nodes;
	}
	
	public synchronized void reset()
	{
		this.nodes = new HashMap<Id, NodeImpl>();
//		this.nodes.clear();
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
			log.warn("No Network found");
			this.nodes = nodes;
		}
	}
}