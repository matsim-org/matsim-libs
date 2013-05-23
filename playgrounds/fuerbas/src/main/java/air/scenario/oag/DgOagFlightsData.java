/* *********************************************************************** *
 * project: org.matsim.*
 * DgOagFlightsData
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package air.scenario.oag;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;


/**
 * @author dgrether
 *
 */
public class DgOagFlightsData {

	
	private static final Logger log = Logger.getLogger(DgOagFlightsData.class);
	
	private Map<String, DgOagFlight> flightDesignatorFlightMap = new HashMap<String, DgOagFlight>();
	
	public void addFlight(DgOagFlight flight) {
		if (this.flightDesignatorFlightMap.containsKey(flight.getFlightDesignator())){
			log.warn("Flight designator " + flight.getFlightDesignator() + " already exists, will be overwritten...");
		}
		this.flightDesignatorFlightMap.put(flight.getFlightDesignator(), flight);
	}

	public Map<String, DgOagFlight> getFlightDesignatorFlightMap(){
		return this.flightDesignatorFlightMap;
	}
	
}
