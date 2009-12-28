package playground.gregor.snapshots.postprocessors;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.evacuation.otfvis.drawer.OTFBackgroundTexturesDrawer;


public class ConfluenceArrowsFromEvents implements LinkEnterEventHandler{

	protected static final double PI_HALF = Math.PI/2;
	protected static final double TWO_PI = 2 * Math.PI;
	protected final Map<Node, NodeInfo> infos = new HashMap<Node, NodeInfo>();
	protected final OTFBackgroundTexturesDrawer sbg;
	private final Network network;
	
	public ConfluenceArrowsFromEvents(OTFBackgroundTexturesDrawer sbg, Network network) {
		this.sbg = sbg;
		this.network = network;
	}
	
	public void handleEvent(LinkEnterEvent event) {
		Link l = this.network.getLinks().get(event.getLinkId());
		NodeInfo ni = this.infos.get(l.getFromNode());
		if (ni == null) {
			ni = new NodeInfo();
			ni.node = l.getFromNode();
			this.infos.put(l.getFromNode(), ni);
		}
		ni.outLinks.add(l);
	}

	public void reset(int iteration) {
		// TODO Auto-generated method stub
	}

	public void createArrows() {
		for (NodeInfo ni : this.infos.values()) {
			if (ni.outLinks.size() == 1) {
				Link l = ni.outLinks.iterator().next();
				double xDiff = l.getToNode().getCoord().getX() - l.getFromNode().getCoord().getX();
				CoordImpl f = (CoordImpl) l.getFromNode().getCoord();
				CoordImpl t = (CoordImpl) l.getToNode().getCoord();
				double euclLength = f.calcDistance(t);
				
				double cangle = xDiff / euclLength;
				double angle;
				if (l.getToNode().getCoord().getY() > l.getFromNode().getCoord().getY() ) {
						angle = Math.acos(cangle);
					
				} else {
					angle = 2*Math.PI - Math.acos(cangle);
				}
				double theta = 0.0;
				double dx = f.getX() - t.getX();
				double dy = f.getY() - t.getY();
				if (dx > 0) {
					theta = Math.atan(dy/dx);
				} else if (dx < 0) {
					theta = Math.PI + Math.atan(dy/dx);
				} else { // i.e. DX==0
					if (dy > 0) {
						theta = PI_HALF;
					} else {
						theta = -PI_HALF;
					}
				}
				if (theta < 0.0) theta += TWO_PI;
				
				Coord c = new CoordImpl(l.getCoord().getX() + Math.sin(theta) * 10,l.getCoord().getY() - Math.cos(theta)*10);
				
				double effLinkLength = Math.min(l.getLength(),100);
				this.sbg.addLocation(c, angle, 2*effLinkLength/3);
				
			}
			
		}
		
		
	}
	
	
	static class NodeInfo {
		Set<Link> outLinks = new HashSet<Link>();
		Node node;
	}
}
