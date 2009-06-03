package playground.mmoyo.PTRouter;

import java.util.List;

import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Node;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;

/**
 * A simple console visualizer for small networks
 */
public class PTNetwork2View extends NetworkLayer {
	private static final String NETWORKFILENAME="c://PTnetwork.xml";
	private NetworkLayer cityNet;

	public PTNetwork2View(NetworkLayer cityNet) {
		super();
		this.cityNet = cityNet;
	}

	public PTNetwork2View() {
		super();
	}

	public void createPTNView(List<PTLine> ptLineList) {
		for (PTLine ptLine : ptLineList) {
			boolean firstLink = true;
			for (String strIdLink : ptLine.getRoute()) {
				Link l = this.cityNet.getLink(strIdLink);
				if (firstLink) {
					addNode(l.getFromNode());
				}
				addNode(l.getToNode());
				addLink(l);
				firstLink = false;
			}
		}
		writePTNetwork();
	}

	private void addNode(Node node){
		if (this.getNode(node.getId().toString()) == null) {
			this.createNode(node.getId(), node.getCoord()).setType(node.getType());
		}
	}

	private void addLink(Link l) {
		if (this.getLink(l.getId()) == null) {
			this.createLink(l.getId(), l.getFromNode(), l.getToNode(), l.getLength(), 1.0, 1.0, 1.0, l.getOrigId(), l.getType());
		}
	}

	public void writePTNetwork(){
		NetworkWriter networkWriter = new NetworkWriter(this,NETWORKFILENAME);
		networkWriter.write();
	}
	
	public void printLinks() {
		for (org.matsim.core.api.network.Link l : this.getLinks().values()) {
			System.out.println("(" + l.getFromNode().getId().toString() + ")----" + l.getId().toString() + "--->(" + l.getToNode().getId().toString() + ")   " + "      (" + ((PTNode) l.getFromNode()).getIdStation().toString()+ ")----" + l.getId().toString() + "--->(" + ((PTNode) l.getToNode()).getIdStation().toString() + ")");
		}
	}
	
}