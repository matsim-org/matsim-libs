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
import org.matsim.mobsim.QueueNetwork;
import org.matsim.population.Population;
import org.matsim.utils.vis.otfvis.data.OTFServerQuad;

public interface OTFQuery extends Serializable{
	public enum Type {AGENT,LINK,OTHER};
	public void setId(String id);
	// while this returns true, the query is send to the server EVERY Tick
	public boolean isAlive();
	public void query(QueueNetwork net, Population plans, Events events, OTFServerQuad quad) ;
	public void remove();
	public void draw(OTFDrawer drawer);
	public Type getType();
}
