/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.ikaddoura.utils.pt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;

/**
 * Generates or modifies transit departures.
 * 
 * @author ikaddoura
 */
public class DeparturesGenerator {
	private final static Logger log = Logger.getLogger(DeparturesGenerator.class);

	private TransitSchedule transitSchedule;
	private TransitScheduleFactory sf = new TransitScheduleFactoryImpl();
	
	/**
	 * Creates and adds departures for each given transit line of a given schedule.
	 * 
	 */
	public void addDepartures(TransitSchedule schedule, List<Id> lineIDs, double headway_sec, double startTime, double endTime, double pausenzeit) {
		this.transitSchedule = schedule;
		
		for (Id transitLineId : lineIDs){
			log.info("Transit line Id: " + transitLineId);
			Map<Id, TransitRoute> routeId2transitRoute = new HashMap<Id, TransitRoute>();
			
			for (TransitRoute transitRoute : this.transitSchedule.getTransitLines().get(transitLineId).getRoutes().values()){
				routeId2transitRoute.put(transitRoute.getId(), transitRoute);
			}
			
			// modify departures of all transit routes of that line.

			// assuming one cycle to consist of two routes TODO: RuntimeException if more/less than two routes for one line!
			// assuming the two routes to be equal! TODO: RuntimeException if two routes have a different routeTravelTime!
			
			List<Id> routeIDs = new ArrayList<Id>();
			routeIDs.addAll(routeId2transitRoute.keySet());
			
			int lastStop = routeId2transitRoute.get(routeIDs.get(0)).getStops().size()-1;
			double routeTravelTime = routeId2transitRoute.get(routeIDs.get(0)).getStops().get(lastStop).getArrivalOffset();
			double umlaufzeit_sec = Math.round((routeTravelTime + pausenzeit) * 2.0);
			int numberOfBuses = (int) Math.ceil(umlaufzeit_sec / headway_sec);
			
			log.info("RouteTravelTime: "+ Time.writeTime(routeTravelTime, Time.TIMEFORMAT_HHMMSS));
			log.info("Umlaufzeit: "+ Time.writeTime(umlaufzeit_sec, Time.TIMEFORMAT_HHMMSS));
			log.info("Takt: "+ Time.writeTime(headway_sec, Time.TIMEFORMAT_HHMMSS));
			log.info("Required number of public vehicles: " + numberOfBuses);
			
			List<Id> vehicleIDs = createVehicleIDs(numberOfBuses, transitLineId);
				
			int routeNr = 0;
			for (Id routeId : routeId2transitRoute.keySet()){
				double firstDepartureTime = 0.0;
				if (routeNr == 1){
					firstDepartureTime = startTime;
					log.info(routeId.toString() + ": first departure: "+ Time.writeTime(firstDepartureTime, Time.TIMEFORMAT_HHMMSS));
				}
				else if (routeNr == 0){
					firstDepartureTime = startTime + umlaufzeit_sec/2;
					log.info(routeId.toString() + ": first departure: "+ Time.writeTime(firstDepartureTime, Time.TIMEFORMAT_HHMMSS));
		
				}
				int vehicleIndex = 0;
				int depNr = 0;
				for (double departureTime = firstDepartureTime; departureTime < endTime ; ){
					Departure departure = this.sf.createDeparture(new IdImpl(depNr), departureTime);
					departure.setVehicleId(vehicleIDs.get(vehicleIndex));
					routeId2transitRoute.get(routeId).addDeparture(departure);
					departureTime = departureTime + headway_sec;
					depNr++;
					if (vehicleIndex == numberOfBuses - 1){
						vehicleIndex = 0;
					}
					else {
						vehicleIndex++;
					}
				}				
				routeNr++;
			}			
		}	
	}

	public List<Id> createVehicleIDs(int numberOfBuses, Id transitLineId){
		List<Id> vehicleIDs = new ArrayList<Id>();
		for (int vehicleNr=1 ; vehicleNr <= numberOfBuses ; vehicleNr++){
			vehicleIDs.add(new IdImpl(transitLineId + "_bus_" + vehicleNr));
		}
		return vehicleIDs;
	}

}
