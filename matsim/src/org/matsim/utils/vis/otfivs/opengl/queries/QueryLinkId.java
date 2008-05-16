package org.matsim.utils.vis.otfivs.opengl.queries;

import org.matsim.events.Events;
import org.matsim.mobsim.QueueLink;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.network.Link;
import org.matsim.plans.Plans;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.vis.otfivs.interfaces.OTFDrawer;
import org.matsim.utils.vis.otfivs.interfaces.OTFQuery;


public class QueryLinkId implements OTFQuery {
	public String linkId = null;;
	double x;
	double y;

	public QueryLinkId(double x,double y) {
		this.x = x;
		this.y = y;
	}

	public void draw(OTFDrawer drawer) {
	}

	public void query(QueueNetworkLayer net, Plans plans, Events events) {
		double minDist = Double.POSITIVE_INFINITY, dist = 0;;
		Link link;
		for( QueueLink qlink : net.getLinks().values()) {
			link = qlink.getLink();
			CoordI middle = link.getCenter();
			double xDist = this.x - middle.getX();
			double yDist = this.y - middle.getY();
			dist = Math.sqrt(xDist*xDist + yDist*yDist);
			if(dist < minDist){
				minDist = dist;
				this.linkId = link.getId().toString();
			}
		}
	}

	public void remove() {
		// TODO Auto-generated method stub

	}

}
