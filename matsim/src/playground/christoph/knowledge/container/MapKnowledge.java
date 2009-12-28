package playground.christoph.knowledge.container;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

public class MapKnowledge extends BasicNodeKnowledge{

	private static final Logger log = Logger.getLogger(MapKnowledge.class);
	
	public boolean isWhiteList = true;
	
	private Map<Id, Node> nodes;
	
	public MapKnowledge()
	{
		this.nodes = new HashMap<Id, Node>();
	}
	
	public void addNode(Node node)
	{
		if (isWhiteList) nodes.put(node.getId(), node);
		else nodes.remove(node.getId());
	}
	
	public void removeNode(Node node)
	{
		if (isWhiteList) nodes.remove(node.getId());
		else nodes.put(node.getId(), node);
	}
		
	public boolean knowsNode(Node node)
	{
		boolean knowsNode = nodes.containsKey(node.getId());
		if (isWhiteList) return knowsNode;
		else return !knowsNode;
	}
	
	public boolean knowsLink(Link link)
	{
		// if no Map found or the Map is empty -> Person knows the entire network, return true
		if ( nodes == null ) return true;
		if ( nodes.size() == 0) return !isWhiteList;
				
		if ( this.knowsNode(link.getFromNode()) && this.knowsNode(link.getToNode()) ) return true;
		else return false;
	}
	
	@Override
	public synchronized Map<Id, Node> getKnownNodes() 
	{
		if (isWhiteList) return nodes;
		else
		{
			Map<Id, Node> invertedNodes = new HashMap<Id, Node>();
			
			invertedNodes.putAll(this.network.getNodes());
			
			for(Node node : nodes.values())
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
	protected Map<Id, Node> getNodes()
	{
		return nodes;
	}
		
	@Override
	public void setKnownNodes(Map<Id, Node> nodes)
	{
		// If we have a network, we can check, whether using a Black- or WhiteList would be better.
		if (this.network != null)
		{
			int nodeCount = this.getNetwork().getNodes().size();
			
			// knows more than half of all nodes
			if (nodes.size() > nodeCount/2)
			{
				this.isWhiteList = false;
				
				Map<Id, Node> blackList = new HashMap<Id, Node>();
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

	public synchronized void clearKnowledge()
	{
		this.isWhiteList = true;
		//this.nodes.clear();
		this.nodes = new HashMap<Id, Node>();
	}
}