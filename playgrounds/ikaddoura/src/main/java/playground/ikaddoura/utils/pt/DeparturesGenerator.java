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
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.vehicles.Vehicle;

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
	 * Creates departures for each given transit line of a given schedule according to the given headway, the start and end time of service and the slack time.
	 * 
	 */
	public void addDepartures(TransitSchedule schedule, List<Id<TransitLine>> lineIDs, double headway_sec, double startTime, double endTime, double pausenzeit) {
		this.transitSchedule = schedule;
		
		if (headway_sec < 1){
			throw new RuntimeException("Headway is less than 1 sec. Aborting...");
		}
		
		for (Id<TransitLine> transitLineId : lineIDs){
			log.info("Transit line Id: " + transitLineId);
			Map<Id<TransitRoute>, TransitRoute> routeId2transitRoute = new HashMap<>();
			
			for (TransitRoute transitRoute : this.transitSchedule.getTransitLines().get(transitLineId).getRoutes().values()){
				routeId2transitRoute.put(transitRoute.getId(), transitRoute);
			}
			
			if (routeId2transitRoute.size() > 2) {
				throw new RuntimeException("A transit line consists of more than two transit routes. So far it is expected that a transit line consists of two identical" +
						" transit routes (one for each direction). Can't garantee correct calculation of departure times. Aborting...");
			}
			
			if (routeId2transitRoute.size() < 2) {
				throw new RuntimeException("A transit line consists of less than two transit routes. So far it is expected that a transit line consists of two identical" +
						" transit routes (one for each direction). Can't garantee correct calculation of departure times. Aborting...");
			}
						
			List<Id<TransitRoute>> routeIDs = new ArrayList<>();
			routeIDs.addAll(routeId2transitRoute.keySet());
			
			int lastStop0 = routeId2transitRoute.get(routeIDs.get(0)).getStops().size()-1;
			double routeTravelTime0 = routeId2transitRoute.get(routeIDs.get(0)).getStops().get(lastStop0).getArrivalOffset();
			
			int lastStop1 = routeId2transitRoute.get(routeIDs.get(1)).getStops().size()-1;
			double routeTravelTime1 = routeId2transitRoute.get(routeIDs.get(1)).getStops().get(lastStop1).getArrivalOffset();
			
			if (routeTravelTime0 != routeTravelTime1) {
				throw new RuntimeException("The transit routes have different travel times. So far it is expected that a transit line consists of two identical" +
						" transit routes (one for each direction). Can't garantee correct calculation of departure times.  ");
			}
			
			double umlaufzeit_sec = Math.round((routeTravelTime0 + pausenzeit) * 2.0);
			int numberOfBuses = (int) Math.ceil(umlaufzeit_sec / headway_sec);
			
			log.info("RouteTravelTime: "+ Time.writeTime(routeTravelTime0, Time.TIMEFORMAT_HHMMSS));
			log.info("Umlaufzeit: "+ Time.writeTime(umlaufzeit_sec, Time.TIMEFORMAT_HHMMSS));
			log.info("Takt: "+ Time.writeTime(headway_sec, Time.TIMEFORMAT_HHMMSS));
			log.info("Required number of public vehicles: " + numberOfBuses);
			
			List<Id<Vehicle>> vehicleIDs = createVehicleIDs(numberOfBuses, transitLineId);
				
			int routeNr = 0;
			for (Id<TransitRoute> routeId : routeId2transitRoute.keySet()){
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
					Departure departure = this.sf.createDeparture(Id.create(depNr, Departure.class), departureTime);
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

	public List<Id<Vehicle>> createVehicleIDs(int numberOfBuses, Id<TransitLine> transitLineId){
		List<Id<Vehicle>> vehicleIDs = new ArrayList<>();
		for (int vehicleNr=1 ; vehicleNr <= numberOfBuses ; vehicleNr++){
			vehicleIDs.add(Id.create(transitLineId + "_bus_" + vehicleNr, Vehicle.class));
		}
		return vehicleIDs;
	}

}
