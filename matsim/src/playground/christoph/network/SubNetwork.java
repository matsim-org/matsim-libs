package playground.christoph.network;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkBuilder;
import org.matsim.core.network.LinkImpl;

public class SubNetwork implements Network {

	protected final Map<Id, SubNode> nodes = new TreeMap<Id, SubNode>();
	protected final Map<Id, LinkImpl> links = new TreeMap<Id, LinkImpl>();
	
	protected Network network;
	
	protected boolean isInitialized = false;
	
	public SubNetwork(Network network)
	{
		this.network = network;
	}
	
	public boolean isInitialized()
	{
		return isInitialized;
	}
	
	public void setInitialized()
	{
		this.isInitialized = true;
	}
	
	public void reset()
	{
		this.nodes.clear();
		this.links.clear();
		this.isInitialized = false;
	}
	
	public NetworkBuilder getBuilder()
	{
		return null;
	}

	public double getCapacityPeriod()
	{
		return network.getCapacityPeriod();
	}

	public double getEffectiveLaneWidth()
	{
		return network.getEffectiveLaneWidth();
	}

	public Map<Id, LinkImpl> getLinks()
	{
		return links;
	}

	public Map<Id, SubNode> getNodes()
	{
		return nodes;
	}

	public void addSubNode(SubNode subNode)
	{
		nodes.put(subNode.getId(), subNode);
	}
	
	public void addLink(LinkImpl link)
	{
		links.put(link.getId(), link);
	}
}
