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
import org.matsim.core.basic.v01.IdImpl;

public class LinkConversionsData {
	private Map<Id,Id> cdata;
	private Id ssid;

	public LinkConversionsData(Id ssid){
		this.ssid=ssid;
		cdata = new HashMap<Id,Id>();
	}
	
	public Id getSsid() {
		return ssid;
	}	
	
	public Id getConv(Id olid){
		if (cdata.get(olid)==null) {
			System.out.println("Link "+olid +" not found, will let untouched");
			return olid;
		}
		return cdata.get(olid);
	}
	public void setConv(Id old, Id newid){
		cdata.put(old, newid);
	}

}

