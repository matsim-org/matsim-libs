/* *********************************************************************** *
 * project: org.matsim.*
 * LinkEvent.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.events;

import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Person;
import org.matsim.plans.Plans;
import org.xml.sax.helpers.AttributesImpl;

abstract class LinkEvent extends BasicEvent{
	
	public String linkId;
	public transient Link link;
	
	LinkEvent(double time, String aID, String lID, Person agent, Link lnk) { 
		super(time, aID, agent); 
		linkId = lID; 
		link = lnk;
	}
	
	LinkEvent(double time, String aID, String lID) { 
		super(time, aID); 
		linkId = lID; 
	}
	
	protected AttributesImpl getAttributesImpl() {
		AttributesImpl attr = new AttributesImpl();

		long time = (long)this.time; // DS TODO output sollte auf "double" umgestellt werden
		attr.addAttribute("","","time", "", Long.toString(time));
		attr.addAttribute("","","agent", "", agentId);
		attr.addAttribute("","","link", "", linkId);
		return attr;
	}

	protected String asString() {
		return getTimeString(this.time) + agentId + "\t0\t" + linkId + "\t0\t"; // FLAG + DESCRIPTION is mising here: concat later
	}
	
	protected void rebuildLinkData(Plans population, NetworkLayer network) {
		agent = population.getPerson(agentId);
		link = (Link)network.getLocation(linkId);
	}

}
