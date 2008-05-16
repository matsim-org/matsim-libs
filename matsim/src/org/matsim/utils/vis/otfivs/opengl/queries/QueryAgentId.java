package org.matsim.utils.vis.otfivs.opengl.queries;

import java.util.LinkedList;
import java.util.List;

import org.matsim.events.Events;
import org.matsim.mobsim.QueueLink;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.plans.Plans;
import org.matsim.utils.vis.otfivs.interfaces.OTFDrawer;
import org.matsim.utils.vis.otfivs.interfaces.OTFQuery;
import org.matsim.utils.vis.snapshots.writers.PositionInfo;


public class QueryAgentId implements OTFQuery {
	double x;
	double y;
	public String agentId;

	public QueryAgentId(double x,double y) {
		this.x = x;
		this.y = y;
	}

	public void draw(OTFDrawer drawer) {
	}

	public void query(QueueNetworkLayer net, Plans plans, Events events) {
		double minDist = Double.POSITIVE_INFINITY, dist = 0;;
		for( QueueLink qlink : net.getLinks().values()) {
			List<PositionInfo> positions = new LinkedList<PositionInfo>();
			qlink.getVehiclePositions(positions);
			for(PositionInfo info : positions) {
				//if (info.getVehicleState() == VehicleState.Parking) continue;
				double xDist = this.x - info.getEasting();
				double yDist = this.y - info.getNorthing();
				dist = Math.sqrt(xDist*xDist + yDist*yDist);
				if(dist < minDist){
					minDist = dist;
					this.agentId = info.getAgentId().toString();
				}
			}
		}
	}

	public void remove() {
		// TODO Auto-generated method stub

	}

}
