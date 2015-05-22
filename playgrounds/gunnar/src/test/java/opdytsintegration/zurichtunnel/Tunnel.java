package opdytsintegration.zurichtunnel;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class Tunnel {

	final Node fromNode;

	final Node toNode;

	final int lanes;

	final double maxSpeed_km_h;

	final Id<Link> link1Id;

	final Id<Link> link2Id;

	final String name;

	final double capacity_veh_h;

	Tunnel(Node fromNode, Node toNode, int lanes, double maxSpeed_km_h,
			final Id<Link> link1Id, final Id<Link> link2Id, final String name) {
		this.fromNode = fromNode;
		this.toNode = toNode;
		this.lanes = lanes;
		this.maxSpeed_km_h = maxSpeed_km_h;
		this.link1Id = link1Id;
		this.link2Id = link2Id;
		this.name = name;
		this.capacity_veh_h = this.maxSpeed_km_h * 15.0
				/ (this.maxSpeed_km_h + 15.0) * 140.0 * this.lanes;
	}

	@Override
	public String toString() {
		return this.name;
	}

}
