/* *********************************************************************** *
 * project: org.matsim.*
 * DgCommodities
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
package playground.dgrether.koehlerstrehlersignal.data;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;


/**
 * @author dgrether
 *
 */
public class DgCommodities {
	
	private static final Logger log = Logger.getLogger(DgCommodities.class);
	
	private Map<Id, DgCommodity> commodities = new HashMap<Id, DgCommodity>();
	
	public DgCommodities(){
	}
	
	public void addCommodity(DgCommodity co){
		if (this.commodities.containsKey(co.getId())){
			log.warn("commodity " + co.getId() + " already exists.");
		}
		this.commodities.put(co.getId(), co);
	}
	
	public Map<Id, DgCommodity> getCommodities(){
		return this.commodities;
	}

}
