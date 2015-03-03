package playground.anhorni.rc.signals;

import java.util.List;
import java.util.Vector;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

public class Intersection {
	
	private List<Id<Link>> linkIds = new Vector<Id<Link>> ();
	private Id<Node> nodeId;
	
	public Intersection(Id<Node> nodeId) {
		this.nodeId = nodeId;
	}
			
	public void addLinkId(Id<Link> linkId) {
		this.linkIds.add(linkId);
	}

	public List<Id<Link>> getLinkIds() {
		return linkIds;
	}
}
