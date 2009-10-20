package playground.christoph.network;

import java.util.Map;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.network.BasicLink;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.network.BasicNodeImpl;
import org.matsim.core.network.NodeImpl;

public class SubNode extends BasicNodeImpl implements Node{

	private Node parentNode;
	private boolean equalsParent = false;
	
	public SubNode(Node node) 
	{
		super(node.getId(), node.getCoord());
		this.parentNode = node;
	}

	public SubNode(Node node, boolean equalsParent) 
	{
		super(node.getId(), node.getCoord());
		this.parentNode = node;
		
		if (equalsParent)
		{
			this.equalsParent = true;
			
			this.inlinks = null;
			this.outlinks = null;
		}
	}
	
	public Node getParentNode()
	{
		return parentNode;
	}
	
	/*
	 * Adds the InLink without Checking if it is already part
	 * of the InLinksMap 
	 */
	public void addInLinkNoCheck(BasicLink inlink)
	{
		Id linkid = inlink.getId();
		this.inlinks.put(linkid, inlink);
	}

	/*
	 * Adds the OutLink without Checking if it is already part
	 * of the OutLinksMap 
	 */
	public void addOutLinkNoCheck(BasicLink outlink) 
	{
		Id linkid = outlink.getId();
		this.outlinks.put(linkid, outlink);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public Map<Id, ? extends Link> getInLinks()
	{
		if (equalsParent) return parentNode.getInLinks();
			
		return (Map<Id, Link>) super.getInLinks();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<Id, ? extends Link> getOutLinks()
	{
		if (equalsParent) return parentNode.getInLinks();
		
		return (Map<Id, Link>)super.getOutLinks();
	}
	
	@Override
	public boolean equals(final Object other) 
	{
		if (other instanceof Node)
		{
			return this.id.equals(((Node)other).getId());
		}
		return false;
	}
}
