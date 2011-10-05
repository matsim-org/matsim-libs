package playground.sergioo.NetworksMatcher.kernel.core;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkImpl;

public abstract class ComposedLink extends LinkImpl {
	
	
	//Attributes
	
	private final List<Link> links;
	
	
	//Methods
	
	public ComposedLink(Id id, Node from, Node to, Network network) {
		super(id, from, to, network, 0, 0, 0, 0);
		links = new ArrayList<Link>();
	}

	public List<Link> getLinks() {
		return links;
	}
	
	public double getAngle() {
		return Math.atan2(to.getCoord().getY()-from.getCoord().getY(), to.getCoord().getX()-from.getCoord().getX());
	}
	
}
