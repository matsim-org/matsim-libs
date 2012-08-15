/* *********************************************************************** *
 * project: org.matsim.*
 * DgAirportCapacityData
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
package air.scenario;

import java.util.HashMap;
import java.util.Map;


/**
 * @author dgrether
 *
 */
public class DgAirportsCapacityData {

		private Map<String, DgAirportCapacity> airportMap = new HashMap<String, DgAirportCapacity>();

		private DgAirportCapacity defaultAirportCapacity;
	
		private double capacityPeriodSeconds;
		
		public DgAirportsCapacityData(double capacityPeriodSeconds){
			this.capacityPeriodSeconds = capacityPeriodSeconds;
			this.defaultAirportCapacity = new DgAirportCapacity("default", capacityPeriodSeconds);
		}
		
		public double getCapacityPeriodSeconds(){
			return this.capacityPeriodSeconds;
		}
		
		
		public DgAirportCapacity getAirportCapacity(String airportCode){
			DgAirportCapacity airportCapacity = this.airportMap.get(airportCode);
			 if (airportCapacity != null) {
				 return airportCapacity;
			 }
			 return defaultAirportCapacity;
		}
	
		public void addAirportCapacity(DgAirportCapacity airportCapacity){
			this.airportMap.put(airportCapacity.getAirportCode(), airportCapacity);
		}
		
}
