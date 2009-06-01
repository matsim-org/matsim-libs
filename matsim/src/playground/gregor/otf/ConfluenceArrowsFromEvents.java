package playground.gregor.otf;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Network;
import org.matsim.core.api.network.Node;
import org.matsim.core.events.LinkEnterEvent;
import org.matsim.core.events.handler.LinkEnterEventHandler;
import org.matsim.core.utils.geometry.CoordImpl;

public class ConfluenceArrowsFromEvents implements LinkEnterEventHandler{


	private Map<Node,NodeInfo> infos = new HashMap<Node,NodeInfo>();
	private SimpleBackgroundTextureDrawer sbg;
	private Network network;
	
	public ConfluenceArrowsFromEvents(SimpleBackgroundTextureDrawer sbg, Network network) {
		this.sbg = sbg;
		this.network = network;
	}
	
	
	
	public void handleEvent(LinkEnterEvent event) {
		Link l = event.getLink();
		if (l == null) {
			l = this.network.getLink(event.getLinkId());
		}
		NodeInfo ni = this.infos.get(l.getFromNode());
		if (ni == null) {
			ni = new NodeInfo();
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
				
				this.sbg.addLocation(l.getCoord(), angle, 2*l.getLength()/3);
				
			}
			
		}
		
		
	}
	
	
	private static class NodeInfo {
		Set<Link> outLinks = new HashSet<Link>();
	}
}
