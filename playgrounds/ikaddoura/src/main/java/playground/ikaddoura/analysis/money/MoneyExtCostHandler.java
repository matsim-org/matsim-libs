/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.ikaddoura.analysis.money;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.noise.personLinkMoneyEvents.PersonLinkMoneyEvent;
import org.matsim.contrib.noise.personLinkMoneyEvents.PersonLinkMoneyEventHandler;
import org.matsim.vehicles.Vehicle;

/**
 * @author ikaddoura
 *
 */
public class MoneyExtCostHandler implements  PersonLinkMoneyEventHandler, PersonDepartureEventHandler, PersonArrivalEventHandler, LinkLeaveEventHandler, PersonEntersVehicleEventHandler {
	private static final Logger log = Logger.getLogger(MoneyExtCostHandler.class);

	private final double timeBinSize = 3600.;
	
	private final Map<Integer, Double> timeBin2congestionTollPayments = new HashMap<>();
	private final Map<Integer, Double> timeBin2noiseTollPayments = new HashMap<>();
	private final Map<Integer, Double> timeBin2airPollutionTollPayments = new HashMap<>();
	private final Map<Integer, Integer> timeBin2numberOfCarDepartures = new HashMap<>();
	private final Map<Integer, Integer> timeBin2numberOfCarArrivals = new HashMap<>();
	private final Map<Integer, Double> timeBin2carTravelDistance = new HashMap<>();
	private final Map<Integer, Double> timeBin2carTravelTime = new HashMap<>();
	
	private final Map<Id<Person>, Double> personId2distance = new HashMap<>();
	private final Map<Id<Person>, Double> personId2departureTime = new HashMap<>();
	private final Map<Id<Vehicle>, Id<Person>> vehicleId2personId = new HashMap<>();
	
	private final Network network;
	
	public MoneyExtCostHandler(Network network) {
		this.network = network;
	}

	@Override
	public void reset(int iteration) {
		this.timeBin2airPollutionTollPayments.clear();
		this.timeBin2congestionTollPayments.clear();
		this.timeBin2noiseTollPayments.clear();
		this.timeBin2numberOfCarDepartures.clear();
		this.timeBin2carTravelDistance.clear();
		this.timeBin2numberOfCarArrivals.clear();
		this.personId2distance.clear();
		this.vehicleId2personId.clear();
		this.timeBin2carTravelTime.clear();
		this.personId2departureTime.clear();
	}
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		
		if (this.personId2distance.get(this.vehicleId2personId.get(event.getVehicleId())) == null) {
			this.personId2distance.put(this.vehicleId2personId.get(event.getVehicleId()), network.getLinks().get(event.getLinkId()).getLength());
		} else {
			this.personId2distance.put(this.vehicleId2personId.get(event.getVehicleId()), this.personId2distance.get(this.vehicleId2personId.get(event.getVehicleId())) + network.getLinks().get(event.getLinkId()).getLength());
		}
	}
	
	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (event.getLegMode().equals(TransportMode.car)) {
			
			if (this.timeBin2carTravelDistance.get(getIntervalNr(event.getTime())) == null) {
				this.timeBin2carTravelDistance.put(getIntervalNr(event.getTime()), this.personId2distance.get(event.getPersonId()));
			} else {
				this.timeBin2carTravelDistance.put(getIntervalNr(event.getTime()), this.timeBin2carTravelDistance.get(getIntervalNr(event.getTime())) + this.personId2distance.get(event.getPersonId()));
			}
			
			if (this.timeBin2carTravelTime.get(getIntervalNr(event.getTime())) == null) {
				this.timeBin2carTravelTime.put(getIntervalNr(event.getTime()), event.getTime() - this.personId2departureTime.get(event.getPersonId()));
			} else {
				this.timeBin2carTravelTime.put(getIntervalNr(event.getTime()), this.timeBin2carTravelTime.get(getIntervalNr(event.getTime())) + (event.getTime() - this.personId2departureTime.get(event.getPersonId())));
			}
			
			if (this.timeBin2numberOfCarArrivals.get(getIntervalNr(event.getTime())) == null) {
				this.timeBin2numberOfCarArrivals.put(getIntervalNr(event.getTime()), 1);
			} else {
				this.timeBin2numberOfCarArrivals.put(getIntervalNr(event.getTime()), this.timeBin2numberOfCarArrivals.get(getIntervalNr(event.getTime())) + 1);
			}
		}
	}

	@Override
	public void handleEvent(PersonLinkMoneyEvent event) {
		
		if (event.getDescription().equalsIgnoreCase("congestion")) {
			
			if (this.timeBin2congestionTollPayments.get(getIntervalNr(event.getRelevantTime())) == null) {
				this.timeBin2congestionTollPayments.put(getIntervalNr(event.getRelevantTime()), event.getAmount());
			} else {
				this.timeBin2congestionTollPayments.put(getIntervalNr(event.getRelevantTime()), this.timeBin2congestionTollPayments.get(getIntervalNr(event.getRelevantTime())) + event.getAmount());
			}
			
		} else if (event.getDescription().equalsIgnoreCase("noise")) {
			
			if (this.timeBin2noiseTollPayments.get(getIntervalNr(event.getRelevantTime())) == null) {
				this.timeBin2noiseTollPayments.put(getIntervalNr(event.getRelevantTime()), event.getAmount());
			} else {
				this.timeBin2noiseTollPayments.put(getIntervalNr(event.getRelevantTime()), this.timeBin2noiseTollPayments.get(getIntervalNr(event.getRelevantTime())) + event.getAmount());
			}
			
		} else if (event.getDescription().equalsIgnoreCase("airPollution")) {
			
			if (this.timeBin2airPollutionTollPayments.get(getIntervalNr(event.getRelevantTime())) == null) {
				this.timeBin2airPollutionTollPayments.put(getIntervalNr(event.getRelevantTime()), event.getAmount());
			} else {
				this.timeBin2airPollutionTollPayments.put(getIntervalNr(event.getRelevantTime()), this.timeBin2airPollutionTollPayments.get(getIntervalNr(event.getRelevantTime())) + event.getAmount());
			}
			
		} else {
			throw new RuntimeException("Unknown money event description. Aborting...");
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().equals(TransportMode.car)) {
			
			this.personId2distance.put(event.getPersonId(), 0.);
			this.personId2departureTime.put(event.getPersonId(), event.getTime());
			
			if (this.timeBin2numberOfCarDepartures.get(getIntervalNr(event.getTime())) == null) {
				this.timeBin2numberOfCarDepartures.put(getIntervalNr(event.getTime()), 1);
			} else {
				this.timeBin2numberOfCarDepartures.put(getIntervalNr(event.getTime()), this.timeBin2numberOfCarDepartures.get(getIntervalNr(event.getTime())) + 1);
			}
		}
	}
	
	private int getIntervalNr(double time) {
		return (int) (time / this.timeBinSize);
	}
	
	public void writeInfo(String outputDirectory) {
		
		String fileName = outputDirectory + "moneyExtCostPerTimeBin.csv";
		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			
			bw.write("TimeBin ; TimeBinEndTime ; NumberOfDepartures ; NumberOfArrivals ; TotalTravelTime ; TotalTravelDistance ; CongestionTolls ; NoiseTolls ; AirPollutionTolls");
			bw.newLine();
			
			for (int n = 0; n <= 30 ; n++) {
				
				int departures = 0;
				int arrivals = 0;
				double travelTime = 0.;
				double distance = 0.;
				double congestionTolls = 0.;
				double noiseTolls = 0.;
				double airPollutionTolls = 0.;
				
				if (timeBin2numberOfCarDepartures.get(n) != null) {
					departures = timeBin2numberOfCarDepartures.get(n);
				}
				
				if (timeBin2numberOfCarArrivals.get(n) != null) {
					arrivals = timeBin2numberOfCarArrivals.get(n);
				}
				
				if (timeBin2carTravelTime.get(n) != null) {
					travelTime = timeBin2carTravelTime.get(n);
				}
				
				if (timeBin2carTravelDistance.get(n) != null) {
					distance = timeBin2carTravelDistance.get(n);
				}
				
				if (timeBin2congestionTollPayments.get(n) != null) {
					congestionTolls = -1 * timeBin2congestionTollPayments.get(n);
				}
				
				if (timeBin2noiseTollPayments.get(n) != null) {
					noiseTolls = -1 * timeBin2noiseTollPayments.get(n);
				}
				
				if (timeBin2airPollutionTollPayments.get(n) != null) {
					airPollutionTolls = -1 * timeBin2airPollutionTollPayments.get(n);
				}
				
				bw.write(n + " ; " + (n + 1) * this.timeBinSize + " ; " + departures + " ; " + arrivals + " ; " + travelTime + " ; " + distance + " ; " + congestionTolls + " ; " + noiseTolls + " ; " + airPollutionTolls);
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		this.vehicleId2personId.put(event.getVehicleId(), event.getPersonId());
	}
}
