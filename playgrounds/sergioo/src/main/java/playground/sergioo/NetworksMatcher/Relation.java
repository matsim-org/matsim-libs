package playground.sergioo.NetworksMatcher;
import org.matsim.api.core.v01.network.Network;


public class Relation {
	private final Network subNetwork1;
	private final Network subNetwork2;
	protected Relation(Network subNetwork1, Network subNetwork2) {
		super();
		this.subNetwork1 = subNetwork1;
		this.subNetwork2 = subNetwork2;
	}
}
