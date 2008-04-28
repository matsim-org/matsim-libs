package teach.matsim08.sheet3;
import java.util.Map;

import org.matsim.basic.v01.Id;
import org.matsim.interfaces.networks.basicNet.BasicLink;
import org.matsim.interfaces.networks.basicNet.BasicNode;
import org.matsim.utils.geometry.CoordI;

public class CANode implements BasicNode {

	private BasicNode basicNode;

	public CANode(BasicNode basicNode) {
		this.basicNode = basicNode;

	}

	public boolean addInLink(BasicLink link) {
		return basicNode.addInLink(link);
	}

	public boolean addOutLink(BasicLink link) {
		return basicNode.addOutLink(link);
	}

	public CoordI getCoord() {
		return basicNode.getCoord();
	}

	public Id getId() {
		return basicNode.getId();
	}

	public Map<Id, ? extends BasicLink> getInLinks() {
		return basicNode.getInLinks();
	}

	public Map<Id, ? extends BasicLink> getOutLinks() {
		return basicNode.getOutLinks();
	}

}
