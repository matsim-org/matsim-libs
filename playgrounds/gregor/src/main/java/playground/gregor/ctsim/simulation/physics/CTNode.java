package playground.gregor.ctsim.simulation.physics;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;

public class CTNode implements CTNetworkEntity {

	private final CTCell cell;
	private final Node n;
	private Id<Node> id;

	public CTNode(Id<Node> id, Node n, CTNetwork net) {
		this.id = id;
		this.n = n;
		this.cell = new CTCell(n.getCoord().getX(), n.getCoord().getY(), net, this);
		this.cell.r = 192;
	}

	public CTCell getCTCell() {
		return this.cell;
	}
}
