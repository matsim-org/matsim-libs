package org.matsim.utils.vis.otfivs.interfaces;

import java.io.Serializable;

import org.matsim.events.Events;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.plans.Plans;

public interface OTFQuery extends Serializable{

	public void query(QueueNetworkLayer net, Plans plans, Events events) ;
	public void remove();
	public void draw(OTFDrawer drawer);
}
