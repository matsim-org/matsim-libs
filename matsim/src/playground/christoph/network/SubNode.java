package playground.christoph.network;

import java.util.Map;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.network.BasicLink;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.network.BasicNodeImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NodeImpl;

public class SubNode extends BasicNodeImpl implements Node{

	private NodeImpl parentNode;
	private boolean equalsParent = false;
	
	public SubNode(NodeImpl node) 
	{
		super(node.getId(), node.getCoord());
		this.parentNode = node;
	}

	public SubNode(NodeImpl node, boolean equalsParent) 
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
	
	public NodeImpl getParentNode()
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
	public Map<Id, ? extends LinkImpl> getInLinks()
	{
		if (equalsParent) return parentNode.getInLinks();
			
		return (Map<Id, LinkImpl>) super.getInLinks();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<Id, ? extends LinkImpl> getOutLinks()
	{
		if (equalsParent) return parentNode.getInLinks();
		
		return (Map<Id, LinkImpl>)super.getOutLinks();
	}
}
