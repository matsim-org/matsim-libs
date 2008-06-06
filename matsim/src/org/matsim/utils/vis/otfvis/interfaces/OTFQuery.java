/* *********************************************************************** *
 * project: org.matsim.*
 * OTFQuery.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.utils.vis.otfvis.interfaces;

import java.io.Serializable;

import org.matsim.events.Events;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.plans.Plans;

public interface OTFQuery extends Serializable{

	public void query(QueueNetworkLayer net, Plans plans, Events events) ;
	public void remove();
	public void draw(OTFDrawer drawer);
}
