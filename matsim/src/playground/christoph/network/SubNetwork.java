package playground.christoph.network;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;

public class SubNetwork implements Network {

	protected Map<Id, Node> nodes;
	protected Map<Id, Link> links;
	
	protected Network network;
	
	protected boolean isInitialized = false;
	
	public SubNetwork(Network network)
	{
		this.network = network;
	}
	
	public void initialize()
	{
		nodes = new TreeMap<Id, Node>();
		links = new TreeMap<Id, Link>();
	}
	
	public void initialize(int nodesCount)
	{
		nodes = new HashMap<Id, Node>((int)(nodesCount * 1.1), 0.95f);
		links = new TreeMap<Id, Link>();
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
//		this.nodes.clear();
//		this.links.clear();
		this.nodes = new HashMap<Id, Node>();
		this.links = new HashMap<Id, Link>();

		this.isInitialized = false;
	}
	
	public NetworkFactory getFactory()
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

	public Map<Id, Link> getLinks()
	{
		return links;
	}

	public Map<Id, Node> getNodes()
	{
		return nodes;
	}

	public void addNode(Node node)
	{
		nodes.put(node.getId(), node);
	}

	public void addLink(Link link)
	{
		links.put(link.getId(), link);
	}
}
