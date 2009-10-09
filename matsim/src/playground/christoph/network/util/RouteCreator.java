package playground.christoph.network.util;

import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.population.routes.NetworkRouteWRefs;

import playground.christoph.network.mapping.MappingInfo;

/*
 * A Route that has been created on a simplified, mapped
 * Network has to be mapped back to the underlying Network
 * to be executed by the QueueSimulation.
 * 
 * Input is a Route on the Mapped Network so we have to
 * find the corresponding Route on the underlying Network.
 */
public class RouteCreator {
	
	private NetworkRouteWRefs route;
	
	public void test()
	{
		List<Node> nodes = route.getNodes();
				
		for (Node node : nodes)
		{
			getParentNode(node);
		}
		
		List<Link> links = route.getLinks();
		
		for (Link link : links)
		{
			getParentLink(link);
		}
	}
	
	private Node getParentNode(Node node)
	{
		/*
		 *  If the Node contains some Mapping Information we try to
		 *  find its parent.
		 */
		if (node instanceof MappingInfo)
		{
			MappingInfo mappingInfo = (MappingInfo) node;
			
			mappingInfo.getDownMapping().getInput();
			return node;
		}
		else return node;
	}
	
	private Link getParentLink(Link link)
	{
		return link;
	}
}
