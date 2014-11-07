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
	
	private final List<Id<Vehicle>> hdvVehicles = new ArrayList<Id<Vehicle>>();
	
	private Scenario scenario;
	
	private List<LinkEnterEvent> linkEnterEvents = new ArrayList<LinkEnterEvent>();
	private List<LinkEnterEvent> linkEnterEventsCar = new ArrayList<LinkEnterEvent>();
	private List<LinkEnterEvent> linkEnterEventsHdv = new ArrayList<LinkEnterEvent>();
	
	private Map<Id<Link>,List<LinkEnterEvent>> linkId2linkEnterEvents = new HashMap<Id<Link>, List<LinkEnterEvent>>();
	private Map<Id<Link>,List<LinkEnterEvent>> linkId2linkEnterEventsCar = new HashMap<Id<Link>, List<LinkEnterEvent>>();
	private Map<Id<Link>,List<LinkEnterEvent>> linkId2linkEnterEventsHdv = new HashMap<Id<Link>, List<LinkEnterEvent>>();
	private Map<Id<Link>, Map<Double,List<LinkEnterEvent>>> linkId2timeInterval2linkEnterEvents = new HashMap<Id<Link>, Map<Double,List<LinkEnterEvent>>>();
	private Map<Id<Link>, Map<Double,List<LinkEnterEvent>>> linkId2timeInterval2linkEnterEventsCar = new HashMap<Id<Link>, Map<Double,List<LinkEnterEvent>>>();
	private Map<Id<Link>, Map<Double,List<LinkEnterEvent>>> linkId2timeInterval2linkEnterEventsHdv = new HashMap<Id<Link>, Map<Double,List<LinkEnterEvent>>>();
	
	// output
	private Map<Id<Link>,Map<Double,Double>> linkId2timeInterval2noiseEmission = new HashMap<Id<Link>, Map<Double,Double>>();
	
	public NoiseEmissionHandler (Scenario scenario) {
		this.scenario = scenario;
	}

	@Override
	public void reset(int iteration) {
		linkEnterEvents.clear();
		linkEnterEventsCar.clear();
		linkEnterEventsHdv.clear();
		linkId2linkEnterEvents.clear();
		linkId2linkEnterEventsCar.clear();
		linkId2linkEnterEventsHdv.clear();
		linkId2timeInterval2linkEnterEvents.clear();
		linkId2timeInterval2linkEnterEventsCar.clear();
		linkId2timeInterval2linkEnterEventsHdv.clear();
		linkId2timeInterval2noiseEmission.clear();
	}
	
	public void setHdvVehicles(ArrayList<Id<Vehicle>> hdvVehicles) {
		if (hdvVehicles == null){
			log.warn("No HDV vehicle information provided. All vehicles are considered as cars and the HGV share is set to zero.");
		} else {
			this.hdvVehicles.addAll(hdvVehicles);
		}
	}
		
	@Override
	public void handleEvent(LinkEnterEvent event) {
		
		if (!(scenario.getPopulation().getPersons().containsKey(event.getVehicleId()))) {
			// probably public transit
			
		} else {
		
		if (hdvVehicles.contains(event.getVehicleId())) {
			
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
			linkEnterEventsHdv.add(event);
			
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
			linkEnterEventsCar.add(event);
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
		linkEnterEvents.add(event);
		}
	}
	
	public Map<Id<Link>, Map<Double, Double>> getLinkId2timeInterval2noiseEmission() {
		return linkId2timeInterval2noiseEmission;
	}
	
	public List<Id<Vehicle>> getHdvVehicles() {
		return hdvVehicles;
	}
	
	public Map<Id<Link>, Map<Double, List<LinkEnterEvent>>> getLinkId2timeInterval2linkEnterEvents() {
		return linkId2timeInterval2linkEnterEvents;
	}

	public Map<Id<Link>, Map<Double, List<LinkEnterEvent>>> getLinkId2timeInterval2linkEnterEventsCar() {
		return linkId2timeInterval2linkEnterEventsCar;
	}

	public Map<Id<Link>, Map<Double, List<LinkEnterEvent>>> getLinkId2timeInterval2linkEnterEventsHdv() {
		return linkId2timeInterval2linkEnterEventsHdv;
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
			for (double timeInterval = NoiseConfigParameters.getTimeBinSizeNoiseComputation() ; timeInterval <= 30 * 3600 ; timeInterval = timeInterval + NoiseConfigParameters.getTimeBinSizeNoiseComputation()){
				double noiseEmission = 0.;

				int n_car = linkId2timeInterval2linkEnterEventsCar.get(linkId).get(timeInterval).size();
				int n_hdv = linkId2timeInterval2linkEnterEventsHdv.get(linkId).get(timeInterval).size();
				int n = n_car + n_hdv;
				double p = 0.;
				
				if(!(n == 0)) {
					p = n_hdv / ((double) n);
				}
				
				if(!(n == 0)) {
//					// correction for a sample, multiplicate the scale factor
					n = (int) (n * (NoiseConfigParameters.getScaleFactor()));
					// correction for intervals unequal to 3600 seconds (= one hour)
					n = (int) (n * (3600./NoiseConfigParameters.getTimeBinSizeNoiseComputation()));
					
					noiseEmission = calculateEmissionspegel(n, p, vCar, vHdv);
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
			Map<Double,List<LinkEnterEvent>> timeInterval2linkEnterEvents = new HashMap<Double, List<LinkEnterEvent>>();
			Map<Double,List<LinkEnterEvent>> timeInterval2linkEnterEventsCar = new HashMap<Double, List<LinkEnterEvent>>();
			Map<Double,List<LinkEnterEvent>> timeInterval2linkEnterEventsHdv = new HashMap<Double, List<LinkEnterEvent>>();
			
			// time interval
			for (double timeInterval = NoiseConfigParameters.getTimeBinSizeNoiseComputation() ; timeInterval <= 30 * 3600 ; timeInterval = timeInterval + NoiseConfigParameters.getTimeBinSizeNoiseComputation()) {
				
				// initialize
				List<LinkEnterEvent> listLinkEnterEvents = new ArrayList<LinkEnterEvent>();
				List<LinkEnterEvent> listLinkEnterEventsCar = new ArrayList<LinkEnterEvent>();
				List<LinkEnterEvent> listLinkEnterEventsHdv = new ArrayList<LinkEnterEvent>();
				
				timeInterval2linkEnterEvents.put(timeInterval, listLinkEnterEvents);
				timeInterval2linkEnterEventsCar.put(timeInterval, listLinkEnterEventsCar);
				timeInterval2linkEnterEventsHdv.put(timeInterval, listLinkEnterEventsHdv);
			}
			
			// fill the empty lists / maps
			
			// all 
			if (linkId2linkEnterEvents.containsKey(linkId)) {
				
				for (LinkEnterEvent event : linkId2linkEnterEvents.get(linkId)) {
					double time = event.getTime();
					double timeInterval = 0.;
					
					if ( (time % NoiseConfigParameters.getTimeBinSizeNoiseComputation()) == 0) {
						timeInterval = time;
					} else {
						timeInterval = (( (int) ( time / NoiseConfigParameters.getTimeBinSizeNoiseComputation()) ) * NoiseConfigParameters.getTimeBinSizeNoiseComputation() ) + NoiseConfigParameters.getTimeBinSizeNoiseComputation();
					}
					
					List<LinkEnterEvent> linkEnterEvents = timeInterval2linkEnterEvents.get(timeInterval);
					linkEnterEvents.add(event);
					timeInterval2linkEnterEvents.put(timeInterval, linkEnterEvents);
				}
			}
			
			// car
			if (linkId2linkEnterEventsCar.containsKey(linkId)) {
				
				for (LinkEnterEvent event : linkId2linkEnterEventsCar.get(linkId)) {
					double time = event.getTime();
					double timeInterval = 0.;
					
					if ( (time % NoiseConfigParameters.getTimeBinSizeNoiseComputation() ) == 0) {
						timeInterval = time;
					} else {
						timeInterval = (( (int) ( time / NoiseConfigParameters.getTimeBinSizeNoiseComputation()) ) * NoiseConfigParameters.getTimeBinSizeNoiseComputation() ) + NoiseConfigParameters.getTimeBinSizeNoiseComputation();
					}
					
					List<LinkEnterEvent> linkEnterEventsCar = timeInterval2linkEnterEventsCar.get(timeInterval);
					linkEnterEventsCar.add(event);
					timeInterval2linkEnterEventsCar.put(timeInterval, linkEnterEventsCar);
				}
			}
			
			// hdv
			if (linkId2linkEnterEventsHdv.containsKey(linkId)) {
				
				for (LinkEnterEvent event : linkId2linkEnterEventsHdv.get(linkId)) {
					double time = event.getTime();
					double timeInterval = 0.;
					
					if ((time % NoiseConfigParameters.getTimeBinSizeNoiseComputation()) == 0) {
						timeInterval = time;
					} else {
						timeInterval = (( (int)(time/NoiseConfigParameters.getTimeBinSizeNoiseComputation()) ) * NoiseConfigParameters.getTimeBinSizeNoiseComputation() ) + NoiseConfigParameters.getTimeBinSizeNoiseComputation();
					}
					
					List<LinkEnterEvent> linkEnterEventsHdv = timeInterval2linkEnterEventsHdv.get(timeInterval);
					linkEnterEventsHdv.add(event);
					timeInterval2linkEnterEventsHdv.put(timeInterval, linkEnterEventsHdv);
				}
			}
			
			linkId2timeInterval2linkEnterEvents.put(linkId, timeInterval2linkEnterEvents);
			linkId2timeInterval2linkEnterEventsCar.put(linkId, timeInterval2linkEnterEventsCar);
			linkId2timeInterval2linkEnterEventsHdv.put(linkId, timeInterval2linkEnterEventsHdv);
		}
	}
	
	private double calculateEmissionspegel(int M , double p , double vCar , double vHdv) {		
		//	Der Beurteilungspegel L_r ist bei Straßenverkehrsgeraeuschen gleich dem Mittelungspegel L_m.
		//	L_r = L_m = 10 * lg(( 1 / T_r ) * (Integral)_T_r(10^(0,1*1(t))dt))
		//	L_m,e ist der Mittelungspegel im Abstand von 25m von der Achse der Schallausbreitung
		
		// 	M ... traffic volume
		// 	p ... share of hdv in %
		
		double pInPercentagePoints = p * 100.;	
		double emissionspegel = 0.0;
		double mittelungspegel = 37.3 + 10* Math.log10(M * (1 + (0.082 * pInPercentagePoints)));
		
		emissionspegel = mittelungspegel + calculateGeschwindigkeitskorrekturDv(vCar, vHdv, pInPercentagePoints);
		// other correction factors are considered when calculating the immission
		
		return emissionspegel;
	}
	
	private double calculateGeschwindigkeitskorrekturDv (double vCar , double vHdv , double pInPercentagePoints) {
		// 	basically the speed is 100 km/h
		// 	p ... share of hdv, in percentage points (see above)
		
		double geschwindigkeitskorrekturDv = 0.0;
		double lCar = 27.7 + (10.0 * Math.log10(1.0 + Math.pow(0.02 * vCar, 3.0)));
		double lHdv = 23.1 + (12.5 * Math.log10(vHdv));
		double d = lHdv - lCar; 
		
		geschwindigkeitskorrekturDv = lCar - 37.3 + 10* Math.log10((100.0 + (Math.pow(10.0, (0.1 * d)) - 1) * pInPercentagePoints ) / (100 + 8.23 * pInPercentagePoints));
		return geschwindigkeitskorrekturDv;
	}
	
	public void writeNoiseEmissionStats(String fileName) {
		File file = new File(fileName);
				
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("link;avg noiseEmission;avg noiseEmission (day);avg noiseEmission (night);avg noiseEmission (peak);avg noiseEmission (off-peak)");
			bw.newLine();
			
			List<Double> day = new ArrayList<Double>();
			for(double timeInterval = 6 * 3600 + NoiseConfigParameters.getTimeBinSizeNoiseComputation() ; timeInterval <= 22 * 3600 ; timeInterval = timeInterval + NoiseConfigParameters.getTimeBinSizeNoiseComputation()){
				day.add(timeInterval);
			}
			List<Double> night = new ArrayList<Double>();
			for(double timeInterval = NoiseConfigParameters.getTimeBinSizeNoiseComputation() ; timeInterval <= 24 * 3600 ; timeInterval = timeInterval + NoiseConfigParameters.getTimeBinSizeNoiseComputation()){
				if(!(day.contains(timeInterval))) {
					night.add(timeInterval);
				}
			}
			
			List<Double> peak = new ArrayList<Double>();
			for(double timeInterval = 7 * 3600 + NoiseConfigParameters.getTimeBinSizeNoiseComputation() ; timeInterval <= 9 * 3600 ; timeInterval = timeInterval + NoiseConfigParameters.getTimeBinSizeNoiseComputation()){
				peak.add(timeInterval);
			}
			for(double timeInterval = 15 * 3600 + NoiseConfigParameters.getTimeBinSizeNoiseComputation() ; timeInterval <= 18 * 3600 ; timeInterval = timeInterval + NoiseConfigParameters.getTimeBinSizeNoiseComputation()){
				peak.add(timeInterval);
			}
			List<Double> offPeak = new ArrayList<Double>();
			for(double timeInterval = NoiseConfigParameters.getTimeBinSizeNoiseComputation() ; timeInterval <= 24 * 3600 ; timeInterval = timeInterval + NoiseConfigParameters.getTimeBinSizeNoiseComputation()){
				if(!(peak.contains(timeInterval))) {
					offPeak.add(timeInterval);
				}
			}
			
			for (Id<Link> linkId : this.linkId2timeInterval2noiseEmission.keySet()){
				double avgNoise = 0.;
				double avgNoiseDay = 0.;
				double avgNoiseNight = 0.;
				double avgNoisePeak = 0.;
				double avgNoiseOffPeak = 0.;
				
				double sumAvgNoise = 0.;
				int counterAvgNoise = 0;
				double sumAvgNoiseDay = 0.;
				int counterAvgNoiseDay = 0;
				double sumAvgNoiseNight = 0.;
				int counterAvgNoiseNight = 0;
				double sumAvgNoisePeak = 0.;
				int counterAvgNoisePeak = 0;
				double sumAvgNoiseOffPeak = 0.;
				int counterAvgNoiseOffPeak = 0;
				
				for(double timeInterval : linkId2timeInterval2noiseEmission.get(linkId).keySet()) {
					double noiseValue = linkId2timeInterval2noiseEmission.get(linkId).get(timeInterval);
					double termToAdd = Math.pow(10., noiseValue/10.);
					
					if(timeInterval < 30 * 3600) {
						sumAvgNoise = sumAvgNoise + termToAdd;
						counterAvgNoise++;
					}
					
					if(day.contains(timeInterval)) {
						sumAvgNoiseDay = sumAvgNoiseDay + termToAdd;
						counterAvgNoiseDay++;
					}
					
					if(night.contains(timeInterval)) {
						sumAvgNoiseNight = sumAvgNoiseNight + termToAdd;
						counterAvgNoiseNight++;
					}
				
					if(peak.contains(timeInterval)) {
						sumAvgNoisePeak = sumAvgNoisePeak + termToAdd;
						counterAvgNoisePeak++;
					}
					
					if(offPeak.contains(timeInterval)) {
						sumAvgNoiseOffPeak = sumAvgNoiseOffPeak + termToAdd;
						counterAvgNoiseOffPeak++;
					}	
				}
				
				avgNoise = 10 * Math.log10(sumAvgNoise / (counterAvgNoise));
				avgNoiseDay = 10 * Math.log10(sumAvgNoiseDay / counterAvgNoiseDay);
				avgNoiseNight = 10 * Math.log10(sumAvgNoiseNight / counterAvgNoiseNight);
				avgNoisePeak = 10 * Math.log10(sumAvgNoisePeak / counterAvgNoisePeak);
				avgNoiseOffPeak = 10 * Math.log10(sumAvgNoiseOffPeak / counterAvgNoiseOffPeak);
								
				bw.write(linkId + ";" + avgNoise + ";" + avgNoiseDay+";"+avgNoiseNight+";"+avgNoisePeak+";"+avgNoiseOffPeak);
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		File file2 = new File(fileName + "t");
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file2));
			bw.write("\"String\",\"Real\",\"Real\",\"Real\",\"Real\",\"Real\"");
			
			bw.newLine();
			
			bw.close();
			log.info("Output written to " + fileName + "t");
			
		} catch (IOException e) {
			e.printStackTrace();
		}
				
	}
	
	public void writeNoiseEmissionStatsPerHour(String fileName) {
		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			
			// column headers
			bw.write("link");
			for(int i = 0; i < 30 ; i++) {
				String time = Time.writeTime( (i+1) * NoiseConfigParameters.getTimeBinSizeNoiseComputation(), Time.TIMEFORMAT_HHMMSS );
				bw.write(";demand " + time + ";noise emission " + time);
			}
			bw.newLine();
			
			for (Id<Link> linkId : this.linkId2timeInterval2noiseEmission.keySet()){
				bw.write(linkId.toString()); 
				for(int i = 0 ; i < 30 ; i++) {
					bw.write(";"+ ((linkId2timeInterval2linkEnterEvents.get(linkId).get((i+1)*NoiseConfigParameters.getTimeBinSizeNoiseComputation()).size()) * NoiseConfigParameters.getScaleFactor()) + ";"+linkId2timeInterval2noiseEmission.get(linkId).get((i+1)*NoiseConfigParameters.getTimeBinSizeNoiseComputation()));	
				}
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		File file2 = new File(fileName + "t");
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file2));
			bw.write("\"String\"");
			
			for(int i = 0; i < 30 ; i++) {
				bw.write(",\"Real\",\"Real\"");
			}
			
			bw.newLine();
			
			bw.close();
			log.info("Output written to " + fileName + "t");
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
}
