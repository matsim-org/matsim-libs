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
	
	public ComposedLink(Link link, Node from, Node to, Network network) {
		super(link.getId(), from, to, network, 0, 0, 0, 0);
		links = new ArrayList<Link>();
		links.add(link);
	}
	
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

	public double getAngle(boolean in) {
		return getAngle(this, in);
	}
	
	public double getAngle(ComposedLink link, boolean in) {
		if(link.getLinks().size()==1 && link.getLinks().get(0).getClass().equals(LinkImpl.class))
			return Math.atan2(link.getLinks().get(0).getToNode().getCoord().getY()-link.getLinks().get(0).getFromNode().getCoord().getY(), link.getLinks().get(0).getToNode().getCoord().getX()-link.getLinks().get(0).getFromNode().getCoord().getX());
		else if(in) {
			for(Link subLink:link.getLinks())
				if(subLink.getToNode().getId().equals(link.getToNode().getId()))
					if(subLink.getClass().equals(LinkImpl.class))
						return Math.atan2(link.getLinks().get(0).getToNode().getCoord().getY()-link.getLinks().get(0).getFromNode().getCoord().getY(), link.getLinks().get(0).getToNode().getCoord().getX()-link.getLinks().get(0).getFromNode().getCoord().getX());
					else		
						return getAngle((ComposedLink) subLink, in);
		}
		else
			for(Link subLink:link.getLinks())
				if(subLink.getFromNode().getId().equals(link.getFromNode().getId()))
					if(subLink.getClass().equals(LinkImpl.class))
						return Math.atan2(link.getLinks().get(0).getToNode().getCoord().getY()-link.getLinks().get(0).getFromNode().getCoord().getY(), link.getLinks().get(0).getToNode().getCoord().getX()-link.getLinks().get(0).getFromNode().getCoord().getX());
					else		
						return getAngle((ComposedLink) subLink, in);
		return Double.NaN;
	}
	
}
