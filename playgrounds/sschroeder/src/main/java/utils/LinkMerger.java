package utils;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.LinkFactory;
import org.matsim.core.network.LinkFactoryImpl;

public class LinkMerger {
	
	private LinkFactory linkFactory;
	
	public LinkMerger() {
		super();
		linkFactory = new LinkFactoryImpl();
	}

	public Link merge(Link link1, Link link2, Network network){
		Link link = linkFactory.createLink(link1.getId(), link1.getFromNode(), link2.getToNode(), network, link1.getLength() + link2.getLength(), 
				link1.getFreespeed(), link1.getCapacity(), link1.getNumberOfLanes());
		return link;
	}

}
