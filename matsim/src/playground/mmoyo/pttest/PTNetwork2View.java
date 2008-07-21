package playground.mmoyo.pttest;

import java.util.Iterator;
import java.util.List;

import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkWriter;
import org.matsim.network.Node;

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
		for (Iterator<PTLine> iterPTLines = ptLineList.iterator(); iterPTLines.hasNext();) {
			PTLine ptLine = iterPTLines.next();
			boolean firstLink = true;
			for (Iterator<String> iter = ptLine.getRoute().iterator(); iter.hasNext();) {
				Link l = this.cityNet.getLink(iter.next());
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
			this.createNode(node.getId().toString(), Double.toString(node.getCoord().getX()), Double.toString(node.getCoord().getY()), node.getType());
		}
	}

	private void addLink(Link l) {
		if (this.getLink(l.getId().toString()) == null) {
			this.createLink(l.getId().toString(), l.getFromNode().getId().toString(), l.getToNode().getId().toString(), String.valueOf(l.getLength()), String.valueOf(l.getFreespeed(0)), String.valueOf(l.getCapacity(0)), String.valueOf(l.getLanesAsInt(0)), l.getOrigId(), l.getType());
		}
	}

	public void writePTNetwork() {
		NetworkWriter networkWriter = new NetworkWriter(this,NETWORKFILENAME);
		networkWriter.write();
	}
	
	public void printLinks() {
		for (org.matsim.network.Link l : this.getLinks().values()) {
			System.out.println("(" + l.getFromNode().getId().toString() + ")----" + l.getId().toString() + "--->(" + l.getToNode().getId().toString() + ")   " + "      (" + ((PTNode) l.getFromNode()).getIdFather().toString()+ ")----" + l.getId().toString() + "--->(" + ((PTNode) l.getToNode()).getIdFather().toString() + ")");
		}
	}
	
}