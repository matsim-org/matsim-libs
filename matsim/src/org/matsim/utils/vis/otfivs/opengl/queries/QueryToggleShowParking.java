package org.matsim.utils.vis.otfivs.opengl.queries;

import org.matsim.events.Events;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.plans.Plans;
import org.matsim.utils.vis.otfivs.handler.OTFLinkAgentsHandler;
import org.matsim.utils.vis.otfivs.interfaces.OTFDrawer;
import org.matsim.utils.vis.otfivs.interfaces.OTFQuery;


public class QueryToggleShowParking implements OTFQuery {

	// This is not a real query it just toggles the rendering of vehicles while activities
	
	public void draw(OTFDrawer drawer) {
		
	}

	public void query(QueueNetworkLayer net, Plans plans, Events events) {
		OTFLinkAgentsHandler.showParked = !OTFLinkAgentsHandler.showParked;
	}

	public void remove() {
		
	}

}
