/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.andreas.bln.ana.delayatstop;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import playground.andreas.utils.pt.DelayTracker;

public class DelayAnalyzer {
	
	private final static Logger log = Logger.getLogger(DelayAnalyzer.class);
	private int treshold = 0;
	private int hvzStartHour = 0;
	private int hvzEndHour = 0;
	private DelayHandler handler;
	private final TransitSchedule schedule;

	public DelayAnalyzer(final TransitSchedule schedule) {
		this.schedule = schedule;
	}
	
	public void readEvents(String filename){
		EventsManager events = EventsUtils.createEventsManager();
		this.handler = new DelayHandler(this.treshold, this.schedule);
		events.addHandler(this.handler);
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		reader.readFile(filename);
		
//		handler.printStats();
	}
	
	public static void main(String[] args) {
		Scenario s = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new TransitScheduleReader(s).readFile("TODO path missing");
		
		DelayAnalyzer delayEval = new DelayAnalyzer(s.getTransitSchedule());
		delayEval.setTreshold_s(60);
		delayEval.readEvents("E:/_out/nullfall_M44_344/out/ITERS/it.0/0.events.xml.gz");
	}
	
	public void setTreshold_s(int treshold) {
		this.treshold = treshold;
	}
	
	public void setHVZStartHour_h(int hvzStartHour) {
		this.hvzStartHour = hvzStartHour;
	}
	
	public void setHVZEndHour_h(int hvzEndHour) {
		this.hvzEndHour = hvzEndHour;
	}
	
	public double getAverageNegativeDelay() {		
		return this.getAverageDelayForMap(this.handler.negativeDelayMap);
	}
	public double getAveragePositiveDelay() {		
		return this.getAverageDelayForMap(this.handler.positiveDelayMap);
	}
	public double getAverageNegativeDelayTreshold() {		
		return this.getAverageDelayForMap(this.handler.negativeDelayMapTreshold);
	}
	public double getAveragePositiveDelayTreshold() {		
		return this.getAverageDelayForMap(this.handler.positiveDelayMapTreshold);
	}
	
	public double getAverageNegativeDelayNVZ() {		
		return this.getAverageDelayForMapNVZ(this.handler.negativeDelayMap);
	}
	
	public double getAverageNegativeDelayHVZ() {		
		return this.getAverageDelayForMapHVZ(this.handler.negativeDelayMap);
	}
	
	public double getAveragePositiveDelayNVZ() {		
		return this.getAverageDelayForMapNVZ(this.handler.positiveDelayMap);
	}
	
	public double getAveragePositiveDelayHVZ() {		
		return this.getAverageDelayForMapHVZ(this.handler.positiveDelayMap);
	}
	
	private double getAverageDelayForMap(HashMap<Integer, DelayCountBox> delayMap){
		double sum = 0;
		int entries = 0;
		for (int i = 0; i < delayMap.size(); i++){
			sum += delayMap.get(Integer.valueOf(i)).getAccumulatedDelay();
			entries += delayMap.get(Integer.valueOf(i)).getNumberOfEntries();
		}
		return sum / entries;
	}
	
	private double getAverageDelayForMapHVZ(HashMap<Integer, DelayCountBox> delayMap){
		double sum = 0;
		int entries = 0;
		for (int i = this.hvzStartHour; i < this.hvzEndHour; i++){
			sum += delayMap.get(Integer.valueOf(i)).getAccumulatedDelay();
			entries += delayMap.get(Integer.valueOf(i)).getNumberOfEntries();
		}
		return sum / entries;
	}
	
	private double getAverageDelayForMapNVZ(HashMap<Integer, DelayCountBox> delayMap){
		double sum = 0;
		int entries = 0;
		for (int i = 0; i < this.hvzStartHour; i++){
			sum += delayMap.get(Integer.valueOf(i)).getAccumulatedDelay();
			entries += delayMap.get(Integer.valueOf(i)).getNumberOfEntries();
		}
		
		for (int i = this.hvzEndHour; i < delayMap.size(); i++){
			sum += delayMap.get(Integer.valueOf(i)).getAccumulatedDelay();
			entries += delayMap.get(Integer.valueOf(i)).getNumberOfEntries();
		}
		return sum / entries;
	}

	static class DelayHandler implements TransitDriverStartsEventHandler, VehicleDepartsAtFacilityEventHandler, VehicleArrivesAtFacilityEventHandler{
		
		private final DelayTracker delayTracker;
		private int positiveDepartureCounter = 0;
		private int negativeArrivalCounter = 0;
		
		HashMap<Integer, DelayCountBox> positiveDelayMap = new HashMap<Integer, DelayCountBox>();
		HashMap<Integer, DelayCountBox> negativeDelayMap = new HashMap<Integer, DelayCountBox>();
		HashMap<Integer, DelayCountBox> positiveDelayMapTreshold =  new HashMap<Integer, DelayCountBox>();
		HashMap<Integer, DelayCountBox> negativeDelayMapTreshold  =  new HashMap<Integer, DelayCountBox>();
				
		private int treshold;		
		private final static Logger dhLog = Logger.getLogger(DelayHandler.class);
		
		public DelayHandler(int treshold, final TransitSchedule schedule) {
			this.delayTracker = new DelayTracker(schedule);
			for (int i = 0; i < 30; i++) {
				this.positiveDelayMap.put(new Integer(i), new DelayCountBox());
			}
			
			for (int i = 0; i < 30; i++) {
				this.negativeDelayMap.put(new Integer(i), new DelayCountBox());
			}
			
			for (int i = 0; i < 30; i++) {
				this.positiveDelayMapTreshold.put(new Integer(i), new DelayCountBox());
			}
			
			for (int i = 0; i < 30; i++) {
				this.negativeDelayMapTreshold.put(new Integer(i), new DelayCountBox());
			}
			
			this.treshold = treshold;
		}
		
		@Override
		public void reset(int iteration) {
			dhLog.warn("Should not happen, since scenario runs one iteration only.");			
		}
		
		@Override
		public void handleEvent(TransitDriverStartsEvent event) {
			this.delayTracker.addVehicleAssignment(event.getVehicleId(), event.getTransitLineId(), event.getTransitRouteId(), event.getDepartureId());
		}

		@Override
		public void handleEvent(VehicleArrivesAtFacilityEvent event) {			
			double delay = this.delayTracker.vehicleArrivesAtStop(event.getVehicleId(), event.getTime(), event.getFacilityId());

			DelayCountBox delBox = this.negativeDelayMap.get(Integer.valueOf((int) event.getTime()/3600));
			DelayCountBox delBoxTreshold = this.negativeDelayMapTreshold.get(Integer.valueOf((int) event.getTime()/3600));
			if(delay < -1 * this.treshold){
				delBoxTreshold.addEntry(delay);
			} else {
				delBoxTreshold.addEntry(0.0);
			}

			delBox.addEntry(Math.min(0.0, delay));

			this.negativeDelayMap.put(Integer.valueOf((int) event.getTime()/3600), delBox);
			this.negativeDelayMapTreshold.put(Integer.valueOf((int) event.getTime()/3600), delBoxTreshold); 
			this.negativeArrivalCounter++;
		}

		@Override
		public void handleEvent(VehicleDepartsAtFacilityEvent event) {
			double delay = this.delayTracker.vehicleDepartsAtStop(event.getVehicleId(), event.getTime(), event.getFacilityId());
		
			DelayCountBox delBox = this.positiveDelayMap.get(Integer.valueOf((int) event.getTime()/3600));
			DelayCountBox delBoxTreshold = this.positiveDelayMapTreshold.get(Integer.valueOf((int) event.getTime()/3600));
			if(delay > this.treshold){
				delBoxTreshold.addEntry(delay);
			} else {
				delBoxTreshold.addEntry(0.0);
			}
			
			delBox.addEntry(Math.max(0.0, delay));
			
			this.positiveDelayMap.put(Integer.valueOf((int) event.getTime()/3600), delBox);
			this.positiveDelayMapTreshold.put(Integer.valueOf((int) event.getTime()/3600), delBoxTreshold);
			this.positiveDepartureCounter++;

		}
		
		void printStats(){
			
			if(this.positiveDelayMap.size() == this.negativeDelayMap.size()){
				
				System.out.println("Stunde, Anzahl der Abfahrten, Verspätung je Abfahrt, Verspätung je Abfahrt > " + this.treshold + " s,  , Anzahl der Ankünfte, Verspätung je Ankunft, Verspätung je Ankunft > " + this.treshold + " s");
				for (int i = 0; i < this.positiveDelayMap.size(); i++) {
					StringBuffer string = new StringBuffer();
					string.append(i + "-" + (i+1) + ", ");
					string.append(this.positiveDelayMap.get(Integer.valueOf(i)).getNumberOfEntries() + ", ");
					string.append(this.positiveDelayMap.get(Integer.valueOf(i)).getAverageDelay() + ", ");
//					string.append(this.delayMapTreshold.get(Integer.valueOf(i)).getNumberOfEntries() + ", ");
					string.append(this.positiveDelayMapTreshold.get(Integer.valueOf(i)).getAverageDelay() + ", ");
					string.append(", ");
					string.append(this.negativeDelayMap.get(Integer.valueOf(i)).getNumberOfEntries() + ", ");
					string.append(this.negativeDelayMap.get(Integer.valueOf(i)).getAverageDelay() + ", ");
//					string.append(this.delayMapTermTreshold.get(Integer.valueOf(i)).getNumberOfEntries() + ", ");
					string.append(this.negativeDelayMapTreshold.get(Integer.valueOf(i)).getAverageDelay());
					System.out.println(string.toString());
				
				}
				
			}
		}		
	}


}
