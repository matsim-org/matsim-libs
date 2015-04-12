/* *********************************************************************** *
 * project: org.matsim.*
 * LinkConversionData.java
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

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.signals.model.SignalSystem;

public class LinkConversionData {

	private Id<SignalSystem> ssid;
	Map<Id<Link>,Id<Link>> convmap;
	public Id<SignalSystem> getSsid() {
		return ssid;
	}
	public void setSsid(String ssid) {
		this.ssid = Id.create(ssid, SignalSystem.class);
	}
	public Map<Id<Link>, Id<Link>> getConvmap() {
		return convmap;
	}

	
		
	
	
}

