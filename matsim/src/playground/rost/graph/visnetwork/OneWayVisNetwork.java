package playground.rost.graph.visnetwork;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkLayer;

import playground.rost.eaflow.ea_flow.Flow;
import playground.rost.eaflow.ea_flow.TimeExpandedPath;
import playground.rost.eaflow.ea_flow.TimeExpandedPath.PathEdge;

public class OneWayVisNetwork {

	protected NetworkLayer network;
	protected Flow flow;
	
	protected Map<Link, OneWayLink> forwardLinks = new HashMap<Link, OneWayLink>();
	protected Map<Link, OneWayLink> backwardLinks = new HashMap<Link, OneWayLink>();
	
	public OneWayVisNetwork(NetworkLayer network, Flow flow)
	{	
		this.network = network;
		this.flow = flow;
		calcOneWayLinks();
		calcAccumalatedFlow();
	}
	
	public final Collection<OneWayLink> getOneWayLinks()
	{
		return forwardLinks.values();
	}
	
	protected void calcAccumalatedFlow()
	{
		boolean isForward;
		Link link;
		OneWayLink oWL;
		int flowOnPath;
		for(TimeExpandedPath tEPath : flow.getPaths())
		{
			flowOnPath = tEPath.getFlow();
			for(PathEdge pE : tEPath.getPathEdges())
			{
				link = pE.getEdge();
				if(forwardLinks.containsKey(link))
				{
					oWL = forwardLinks.get(link);
					isForward = true;
				}
				else if(backwardLinks.containsKey(link))
				{
					oWL = backwardLinks.get(link);
					isForward = false;
				}
				else
				{
					continue;
				}
				if(isForward)
				{
					oWL.augmentFlowOnLink(pE.getStartTime(), pE.getArrivalTime(), flowOnPath);
				}
				else
				{
					//PAY ATTENTION TO THE - in front of flowOnPath
					oWL.augmentFlowOnLink(pE.getStartTime(), pE.getArrivalTime(), - flowOnPath);
				}
			}
		}
	}
	
	protected void calcOneWayLinks()
	{
		Set<Link> linksToIgnore = new HashSet<Link>(); 
		for(Link link : network.getLinks().values())
		{
			if(forwardLinks.containsKey(link))
			{
				throw new RuntimeException("The Same link was contained 2 times");
			}
			if(linksToIgnore.contains(link))
			{
				continue;
			}
			Link backwardLink = null;
			Node toNode = link.getToNode();
			Node fromNode = link.getFromNode();
			for(Link possibleBackwardLink : toNode.getOutLinks().values())
			{
				if(possibleBackwardLink.getToNode().equals(fromNode))
				{
					backwardLink = possibleBackwardLink;
					break;
				}
			}
			if(backwardLink != null)
			{
				OneWayLink oWL = new OneWayLink(link, backwardLink);
				int maxTravelTime = flow.getMaxTravelTimeForLink(link);
				oWL.setMaxCapacity((int)(link.getCapacity(1) * maxTravelTime));
				forwardLinks.put(link, oWL);
				backwardLinks.put(backwardLink, oWL);
				linksToIgnore.add(backwardLink);
			}
		}
	}
	
}
