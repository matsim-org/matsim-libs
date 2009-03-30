package playground.balmermi.routeset2;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.network.BasicNode;
import org.matsim.core.api.network.Link;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;

public class StreetSegment extends LinkImpl {
	
	public final Set<Link> links = new HashSet<Link>();

	public StreetSegment(Id id, BasicNode from, BasicNode to, NetworkLayer network, double length, double freespeed, double capacity, double lanes) {
		super(id, from, to, network, length, freespeed, capacity, lanes);
	}
}
