package playground.sergioo.NetworksMatcher;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;


public class NodeNetwork implements Node {


	//Attributes

	private Coord coord;

	private final Id id;

	private final Network subNetwork;

	private final Map<Id, Link> inLinks;

	private final Map<Id, Link> outLinks;


	//Methods

	public NodeNetwork(Network subNetwork) {
		String idText = "";
		for(Node node:subNetwork.getNodes().values())
			idText+=node.getId()+"-";
		id = new IdImpl(idText);
		coord = new CoordImpl(0, 0);
		for(Node node:subNetwork.getNodes().values())
			coord.setXY(coord.getX()+node.getCoord().getX(), coord.getY()+node.getCoord().getY());
		coord.setXY(coord.getX()/subNetwork.getNodes().size(), coord.getY()/subNetwork.getNodes().size());
		this.subNetwork = subNetwork;
		inLinks = new HashMap<Id, Link>();
		outLinks = new HashMap<Id, Link>();
	}

	@Override
	public Coord getCoord() {
		return coord;
	}

	@Override
	public Id getId() {
		return id;
	}

	public Network getSubNetwork() {
		return subNetwork;
	}

	@Override
	public boolean addInLink(Link link) {
		inLinks.put(link.getId(), link);
		return true;
	}

	@Override
	public boolean addOutLink(Link link) {
		outLinks.put(link.getId(), link);
		return false;
	}

	@Override
	public Map<Id, ? extends Link> getInLinks() {
		return inLinks;
	}

	@Override
	public Map<Id, ? extends Link> getOutLinks() {
		return outLinks;
	}


}
