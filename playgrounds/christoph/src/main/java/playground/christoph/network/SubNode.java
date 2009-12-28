package playground.christoph.network;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NodeImpl;

public class SubNode extends NodeImpl {

	private Node parentNode;
	private boolean equalsParent = false;
	
	public SubNode(Node node) 
	{
		super(node.getId(), node.getCoord(), null);
		this.parentNode = node;
	}

	public SubNode(Node node, boolean equalsParent) 
	{
		super(node.getId(), node.getCoord(), null);
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
	public void addInLinkNoCheck(Link inlink)
	{
		Id linkid = inlink.getId();
		this.inlinks.put(linkid, inlink);
	}

	/*
	 * Adds the OutLink without Checking if it is already part
	 * of the OutLinksMap 
	 */
	public void addOutLinkNoCheck(Link outlink) 
	{
		Id linkid = outlink.getId();
		this.outlinks.put(linkid, outlink);
	}
	
	@Override
	public Map<Id, ? extends Link> getInLinks()
	{
		if (equalsParent) return parentNode.getInLinks();
			
		return super.getInLinks();
	}

	@Override
	public Map<Id, ? extends Link> getOutLinks()
	{
		if (equalsParent) return parentNode.getInLinks();
		
		return super.getOutLinks();
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
