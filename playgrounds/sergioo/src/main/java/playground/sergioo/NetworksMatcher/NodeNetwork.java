package playground.sergioo.NetworksMatcher;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NodeImpl;


public class NodeNetwork extends NodeImpl {
	private final Network subnetWork;
	protected NodeNetwork(Id id, Coord coord, Network subNetwork) {
		super(id, coord);
		this.subnetWork = subNetwork;
	}
}
