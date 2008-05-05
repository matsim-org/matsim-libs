package teach.matsim08.sheet4;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.matsim.basic.v01.Id;
import org.matsim.interfaces.networks.basicNet.BasicLink;
import org.matsim.interfaces.networks.basicNet.BasicNode;
import org.matsim.utils.geometry.CoordI;

public class CANode implements BasicNode {

	private BasicNode basicNode;

	private List caInLinks = new ArrayList();
	private List caOutLinks = new ArrayList();
	
	
	public CANode(BasicNode basicNode) {
		this.basicNode = basicNode;
	}
	
	public void randomMove() {
		Collections.shuffle(caInLinks);
		for (Iterator it = caInLinks.iterator(); it.hasNext(); ) {
			CALink caInLink = (CALink) it.next();
			if (caInLink.getFirstVeh() != null) {
				CAVehicle veh = caInLink.getFirstVeh();
				int rnd = (int) (Math.random() * (double) caOutLinks.size());
				CALink caOutLink = (CALink) caOutLinks.get(rnd);
				if (caOutLink.hasSpace()) {
					caInLink.removeFirstVeh();
					caOutLink.addVeh(veh);
				}
			}
		}
	}
	
	public void addCaInLink(CALink caLink) {
		this.caInLinks.add(caLink);
	}
	
	public void addCaOutLink(CALink caLink) {
		this.caOutLinks.add(caLink);
	}
	
	//delegates
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
