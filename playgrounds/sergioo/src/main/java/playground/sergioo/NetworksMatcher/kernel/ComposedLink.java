package playground.sergioo.NetworksMatcher.kernel;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkImpl;

public class ComposedLink extends LinkImpl {
	
	
	//Attributes
	private List<Link> links;
	
	//Methods
	protected ComposedLink(Id id, Node from, Node to, Network network,
			double length, double freespeed, double capacity, double lanes) {
		super(id, from, to, network, length, freespeed, capacity, lanes);
		links = new ArrayList<Link>();
	}
	
}
