/* *********************************************************************** *
 * project: org.matsim.*
 * DgNetwork
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

import org.matsim.api.core.v01.Id;


/**
 * @author dgrether
 *
 */
public class DgNetwork {

	private Map<Id, DgCrossing> crossings = new HashMap<Id, DgCrossing>();
	private Map<Id, DgStreet> streets = new HashMap<Id, DgStreet>();

	public void addCrossing(DgCrossing crossing) {
		this.crossings.put(crossing.getId(), crossing);
	}
	
	public Map<Id, DgCrossing> getCrossings(){
		return this.crossings;
	}

	public void addStreet(DgStreet street) {
		this.streets.put(street.getId(), street);
	}
	
	public Map<Id, DgStreet> getStreets(){
		return this.streets;
	}

}
