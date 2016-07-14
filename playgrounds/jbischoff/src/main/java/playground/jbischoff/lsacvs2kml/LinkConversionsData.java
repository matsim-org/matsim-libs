/* *********************************************************************** *
 * project: org.matsim.*
 * LinkConversionsData.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.jbischoff.lsacvs2kml;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.signals.model.SignalSystem;

public class LinkConversionsData {
	private Map<Id<Link>,Id<Link>> cdata;
	private Id<SignalSystem> ssid;

	public LinkConversionsData(Id<SignalSystem> ssid){
		this.ssid=ssid;
		cdata = new HashMap<>();
	}
	
	public Id<SignalSystem> getSsid() {
		return ssid;
	}	
	
	public Id<Link> getConv(Id<Link> olid){
		if (cdata.get(olid)==null) {
			System.out.println("Link "+olid +" not found, will let untouched");
			return olid;
		}
		return cdata.get(olid);
	}
	public void setConv(Id<Link> old, Id<Link> newid){
		cdata.put(old, newid);
	}

}

