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
package playground.ikaddoura.noise2;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;

/**
 * 
 * Collects the relevant information to compute the noise emission for each link and time interval.
 * 
 * @author lkroeger, ikaddoura
 *
 */

public class NoiseEmissionHandler implements LinkEnterEventHandler {

	private static final Logger log = Logger.getLogger(NoiseEmissionHandler.class);
		
	private Scenario scenario;
	private NoiseParameters noiseParams;
		
	private Map<Id<Link>,List<LinkEnterEvent>> linkId2linkEnterEvents = new HashMap<Id<Link>, List<LinkEnterEvent>>();
	private Map<Id<Link>,List<LinkEnterEvent>> linkId2linkEnterEventsCar = new HashMap<Id<Link>, List<LinkEnterEvent>>();
	private Map<Id<Link>,List<LinkEnterEvent>> linkId2linkEnterEventsHdv = new HashMap<Id<Link>, List<LinkEnterEvent>>();
	
	private Map<Id<Link>, Map<Double,List<Id<Vehicle>>>> linkId2timeInterval2linkEnterVehicleIDs = new HashMap<Id<Link>, Map<Double,List<Id<Vehicle>>>>();
	private Map<Id<Link>, Map<Double, Integer>> linkId2timeInterval2numberOfLinkEnterCars = new HashMap<Id<Link>, Map<Double, Integer>>();
	private Map<Id<Link>, Map<Double, Integer>> linkId2timeInterval2numberOfLinkEnterHgv = new HashMap<Id<Link>, Map<Double, Integer>>();

	// output
	private Map<Id<Link>,Map<Double,Double>> linkId2timeInterval2noiseEmission = new HashMap<Id<Link>, Map<Double,Double>>();
	
	public NoiseEmissionHandler (Scenario scenario, NoiseParameters noiseParams) {
		this.scenario = scenario;
		this.noiseParams = noiseParams;
	}

	@Override
	public void reset(int iteration) {
		linkId2linkEnterEvents.clear();
		linkId2linkEnterEventsCar.clear();
		linkId2linkEnterEventsHdv.clear();
		linkId2timeInterval2linkEnterVehicleIDs.clear();
		linkId2timeInterval2numberOfLinkEnterCars.clear();
		linkId2timeInterval2numberOfLinkEnterHgv.clear();
		linkId2timeInterval2noiseEmission.clear();
	}
		
	@Override
	public void handleEvent(LinkEnterEvent event) {
		
		if (!(scenario.getPopulation().getPersons().containsKey(event.getVehicleId()))) {
			// probably public transit
			
		} else {
		
			if (event.getVehicleId().toString().startsWith(this.noiseParams.getHgvIdPrefix())) {
				// hdv
				
				if (linkId2linkEnterEventsHdv.containsKey(event.getLinkId())) {
					List<LinkEnterEvent> listTmp = linkId2linkEnterEventsHdv.get(event.getLinkId());
					listTmp.add(event);
					linkId2linkEnterEventsHdv.put(event.getLinkId(), listTmp);
				} else {
					List<LinkEnterEvent> listTmp = new ArrayList<LinkEnterEvent>();
					listTmp.add(event);
					linkId2linkEnterEventsHdv.put(event.getLinkId(), listTmp);
				}
				
			} else {
				// car
				
				if (linkId2linkEnterEventsCar.containsKey(event.getLinkId())) {
					List<LinkEnterEvent> listTmp = linkId2linkEnterEventsCar.get(event.getLinkId());
					listTmp.add(event);
					linkId2linkEnterEventsCar.put(event.getLinkId(), listTmp);
				} else {
					List<LinkEnterEvent> listTmp = new ArrayList<LinkEnterEvent>();
					listTmp.add(event);
					linkId2linkEnterEventsCar.put(event.getLinkId(), listTmp);
				}
			}
			
			// all vehicle types
			if (linkId2linkEnterEvents.containsKey(event.getLinkId())) {
				List<LinkEnterEvent> listTmp = linkId2linkEnterEvents.get(event.getLinkId());
				listTmp.add(event);
				linkId2linkEnterEvents.put(event.getLinkId(), listTmp);
			} else {
				List<LinkEnterEvent> listTmp = new ArrayList<LinkEnterEvent>();
				listTmp.add(event);
				linkId2linkEnterEvents.put(event.getLinkId(), listTmp);
			}
		}
	}

	public void calculateNoiseEmission() {
		log.info("Preprocessing data...");
		preProcessData();
		log.info("Preprocessing data... Done.");
		
		log.info("Calculating noise emission for each link and for each time interval...");
		// link
		for (Id<Link> linkId : scenario.getNetwork().getLinks().keySet()){
			Map<Double,Double> timeInterval2NoiseEmission = new HashMap<Double, Double>();
			
			double vCar = (scenario.getNetwork().getLinks().get(linkId).getFreespeed()) * 3.6;
			double vHdv = vCar;
			
			// time interval
			for (double timeInterval = noiseParams.getTimeBinSizeNoiseComputation() ; timeInterval <= 30 * 3600 ; timeInterval = timeInterval + noiseParams.getTimeBinSizeNoiseComputation()){
				double noiseEmission = 0.;

				int n_car = linkId2timeInterval2numberOfLinkEnterCars.get(linkId).get(timeInterval);
				int n_hdv = linkId2timeInterval2numberOfLinkEnterHgv.get(linkId).get(timeInterval);
				int n = n_car + n_hdv;
				double p = 0.;
				
				if(!(n == 0)) {
					p = n_hdv / ((double) n);
				}
				
				if(!(n == 0)) {
					
					// correction for a sample, multiplicate the scale factor
					n = (int) (n * (noiseParams.getScaleFactor()));
					
					// correction for intervals unequal to 3600 seconds (= one hour)
					n = (int) (n * (3600. / noiseParams.getTimeBinSizeNoiseComputation()));
					
					double mittelungspegel = NoiseEquations.calculateMittelungspegelLm(n, p);
					double Dv = NoiseEquations.calculateGeschwindigkeitskorrekturDv(vCar, vHdv, p);
					noiseEmission = mittelungspegel + Dv;					
				}	
				timeInterval2NoiseEmission.put(timeInterval, noiseEmission);
			}
			linkId2timeInterval2noiseEmission.put(linkId , timeInterval2NoiseEmission);
		}
		log.info("Calculating noise emission for each link and for each time interval... Done.");
	}

	private void preProcessData() {
		
		// link
		for (Id<Link> linkId : scenario.getNetwork().getLinks().keySet()) {
			// initialize
			Map<Double,List<Id<Vehicle>>> timeInterval2linkEnterVehicleIDs = new HashMap<Double, List<Id<Vehicle>>>();
			Map<Double, Integer> timeInterval2linkEnterVehicleIDsCar = new HashMap<Double, Integer>();
			Map<Double, Integer> timeInterval2linkEnterVehicleIDsHdv = new HashMap<Double, Integer>();
			
			// time interval
			for (double timeInterval = noiseParams.getTimeBinSizeNoiseComputation() ; timeInterval <= 30 * 3600 ; timeInterval = timeInterval + noiseParams.getTimeBinSizeNoiseComputation()) {
				
				// initialize			
				timeInterval2linkEnterVehicleIDs.put(timeInterval, new ArrayList<Id<Vehicle>>());
				timeInterval2linkEnterVehicleIDsCar.put(timeInterval, 0);
				timeInterval2linkEnterVehicleIDsHdv.put(timeInterval, 0);
			}
			
			// fill the empty lists / maps
			
			// all 
			if (linkId2linkEnterEvents.containsKey(linkId)) {
				
				for (LinkEnterEvent event : linkId2linkEnterEvents.get(linkId)) {
					double time = event.getTime();
					double timeInterval = 0.;
					
					if ( (time % noiseParams.getTimeBinSizeNoiseComputation()) == 0) {
						timeInterval = time;
					} else {
						timeInterval = (( (int) ( time / noiseParams.getTimeBinSizeNoiseComputation()) ) * noiseParams.getTimeBinSizeNoiseComputation() ) + noiseParams.getTimeBinSizeNoiseComputation();
					}
					
					List<Id<Vehicle>> linkEnterVehicleIDs = timeInterval2linkEnterVehicleIDs.get(timeInterval);
					linkEnterVehicleIDs.add(event.getVehicleId());
					timeInterval2linkEnterVehicleIDs.put(timeInterval, linkEnterVehicleIDs);
				}
			}
			linkId2timeInterval2linkEnterVehicleIDs.put(linkId, timeInterval2linkEnterVehicleIDs);
			
			// car
			if (linkId2linkEnterEventsCar.containsKey(linkId)) {
				
				for (LinkEnterEvent event : linkId2linkEnterEventsCar.get(linkId)) {
					double time = event.getTime();
					double timeInterval = 0.;
					
					if ( (time % noiseParams.getTimeBinSizeNoiseComputation() ) == 0) {
						timeInterval = time;
					} else {
						timeInterval = (( (int) ( time / noiseParams.getTimeBinSizeNoiseComputation()) ) * noiseParams.getTimeBinSizeNoiseComputation() ) + noiseParams.getTimeBinSizeNoiseComputation();
					}
					
					int newNumberOfLinkEnterCars = timeInterval2linkEnterVehicleIDsCar.get(timeInterval) + 1;
					timeInterval2linkEnterVehicleIDsCar.put(timeInterval, newNumberOfLinkEnterCars);
				}
			}
			linkId2timeInterval2numberOfLinkEnterCars.put(linkId, timeInterval2linkEnterVehicleIDsCar);

			// hdv
			if (linkId2linkEnterEventsHdv.containsKey(linkId)) {
				
				for (LinkEnterEvent event : linkId2linkEnterEventsHdv.get(linkId)) {
					double time = event.getTime();
					double timeInterval = 0.;
					
					if ((time % noiseParams.getTimeBinSizeNoiseComputation()) == 0) {
						timeInterval = time;
					} else {
						timeInterval = (( (int) (time / noiseParams.getTimeBinSizeNoiseComputation()) ) * noiseParams.getTimeBinSizeNoiseComputation() ) + noiseParams.getTimeBinSizeNoiseComputation();
					}
					int newNumberOfLinkEnterCars = timeInterval2linkEnterVehicleIDsHdv.get(timeInterval) + 1;
					timeInterval2linkEnterVehicleIDsHdv.put(timeInterval, newNumberOfLinkEnterCars);
				}
			}
			linkId2timeInterval2numberOfLinkEnterHgv.put(linkId, timeInterval2linkEnterVehicleIDsHdv);
		}
		
		// Deleting unnecessary information
		this.linkId2linkEnterEvents.clear();
		this.linkId2linkEnterEventsCar.clear();
		this.linkId2linkEnterEventsHdv.clear();		
	}
		
	public Map<Id<Link>, Map<Double, Double>> getLinkId2timeInterval2noiseEmission() {
		return linkId2timeInterval2noiseEmission;
	}
	
	public Map<Id<Link>, Map<Double, List<Id<Vehicle>>>> getLinkId2timeInterval2linkEnterVehicleIDs() {
		return linkId2timeInterval2linkEnterVehicleIDs;
	}

	public Map<Id<Link>, Map<Double, Integer>> getLinkId2timeInterval2numberOfLinkEnterCars() {
		return linkId2timeInterval2numberOfLinkEnterCars;
	}

	public Map<Id<Link>, Map<Double, Integer>> getLinkId2timeInterval2numberOfLinkEnterHgv() {
		return linkId2timeInterval2numberOfLinkEnterHgv;
	}
}
