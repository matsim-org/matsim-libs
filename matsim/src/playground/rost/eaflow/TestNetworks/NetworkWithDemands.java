package playground.rost.eaflow.TestNetworks;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;

import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkLayer;

public class NetworkWithDemands {
	public NetworkLayer network;
	public Map<Node, Integer> demands = new HashMap<Node, Integer>();
	public Node superSink;
	
	public Set<Node> sources = new HashSet<Node>();
	
	protected int totalDemands;
	
	public NetworkWithDemands(NetworkLayer network, Node sink)
	{
		this.network = network;
		this.superSink = sink;
		this.totalDemands = 0;
	}
	
	public void addDemand(Node node, int demand)
	{
		addDemand(node, demand, true);
	}
	
	public void addDemand(Node node, int demand, boolean overwrite)
	{
		if(node == null)
			throw new RuntimeException("null nodes should not have demands ;)");
		if(node.equals(superSink))
			throw new RuntimeException("sink must not have demands!");
		Integer currentDemand = 0;
		if(demands.containsKey(node))
		{
			currentDemand = demands.get(node);
			if(overwrite)
			{
				totalDemands -= currentDemand;
				currentDemand = 0;
			}
		}
		sources.add(node);
		totalDemands += demand;
		currentDemand += demand;
		demands.put(node, currentDemand);
	}
	
	public int getTotalDemands()
	{
		return totalDemands;
	}
}
