package playground.mzilske.osm;

import java.util.Iterator;
import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;

public class Stitcher {

	boolean broken = false;
	
	private Network network;

	private LinkedList<Id> forwardRoute = new LinkedList<Id>();

	public Stitcher(Network network) {
		this.network = network;
	}

	public void addBoth(Way way) {
		System.out.println("WayBoth: " + way.getWayNodes().get(0) + " " + way.getWayNodes().get(way.getWayNodes().size()-1));
		addForwardLinks(way, forwardRoute);
	}

	public void addForward(Way way) {
		System.out.println("WayForward: " + way.getWayNodes().get(0) + " " + way.getWayNodes().get(way.getWayNodes().size()-1));
		addForwardLinks(way, forwardRoute);
	}

	public void addBackward(Way way) {
		System.out.println("WayBackward: " + way.getWayNodes().get(0) + " " + way.getWayNodes().get(way.getWayNodes().size()-1));
		addBackwardLinks(way, forwardRoute);
	}

	public LinkedList<Id> getForwardRoute() {
		if (broken) {
			return new LinkedList<Id>();
		} else {
			return forwardRoute ;
		}
	}

	public LinkedList<Id> getBackwardRoute() {
		return new LinkedList<Id>();
	}
	

	private void addBackwardLinks(Way way, LinkedList<Id> linkIdsR) {
		LinkedList<Tag> tags = new LinkedList<Tag>(way.getTags());
		Iterator<Tag> tagIterator = tags.descendingIterator();
		while (tagIterator.hasNext()) {
			Tag tag = tagIterator.next();
			if (tag.getKey().startsWith("matsim:backward:link-id")) {
				Id linkId = new IdImpl(tag.getValue());
				if (!linkIdsR.isEmpty()) {
					Link last = network.getLinks().get(linkIdsR.getLast());
					Link thisLink = network.getLinks().get(linkId);
					if (! thisLink.getFromNode().equals(last.getToNode())) {
						broken = true;
					}
				}
				linkIdsR.addFirst(linkId);
			}
		}
	}

	private void addForwardLinks(Way way, LinkedList<Id> linkIdsH) {
		for (Tag tag : way.getTags()) {
			if (tag.getKey().startsWith("matsim:forward:link-id")) {
				Id linkId = new IdImpl(tag.getValue());
				if (!linkIdsH.isEmpty()) {
					Link last = network.getLinks().get(linkIdsH.getLast());
					Link thisLink = network.getLinks().get(linkId);
					if (! thisLink.getFromNode().equals(last.getToNode())) {
						broken = true;
					}
				}
				linkIdsH.addLast(linkId);
			} 
		}
	}

}
