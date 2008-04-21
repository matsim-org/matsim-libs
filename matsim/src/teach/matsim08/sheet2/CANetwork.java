package teach.matsim08.sheet2;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.matsim.basic.v01.Id;
import org.matsim.interfaces.networks.basicNet.BasicLink;
import org.matsim.interfaces.networks.basicNet.BasicNet;
import org.matsim.interfaces.networks.basicNet.BasicNode;



public class CANetwork implements BasicNet {


	private Map caLinks = new HashMap();
	private Map caNodes = new HashMap();

	public CANetwork(BasicNet basicNet) {
		Map links = basicNet.getLinks() ;
		Map nodes = basicNet.getNodes();
		for (Iterator it = nodes.values().iterator(); it.hasNext(); ) {
			BasicNode n = (BasicNode)it.next();
			CANode caNode = new CANode(n);
			this.caNodes.put(n.getId(), caNode);
		}
		for (Iterator it = links.values().iterator(); it.hasNext();) {
			BasicLink basicLink = (BasicLink) it.next();
			CALink caLink = new CALink(basicLink);
			this.caLinks.put(basicLink.getId(), caLink);
			caLink.build();
			caLink.randomFill(0.99) ;
		}
	}

	public Map<Id, ? extends BasicLink> getLinks() {
		return caLinks;
	}

	public Map<Id, ? extends BasicNode> getNodes() {
		return caNodes;
	}

	public void connect() {
	}

}
