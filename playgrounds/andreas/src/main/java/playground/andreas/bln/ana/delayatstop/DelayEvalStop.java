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
import java.util.LinkedList;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
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
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.andreas.utils.pt.DelayTracker;

public class DelayEvalStop {
	
	private int treshold = 0;
	private final TransitSchedule schedule;

	public DelayEvalStop(final TransitSchedule schedule) {
		this.schedule = schedule;
	}

	public void readEvents(String filename){
		EventsManager events = EventsUtils.createEventsManager();
		DelayHandler handler = new DelayHandler(this.treshold, this.schedule);
		handler.addTermStop("781015.1"); //344 nord
		//handler.addTermStop("792200.2"); //344 sued aussetzer
		handler.addTermStop("792200.3"); // m44 nord
		
		handler.addTermStop("792040.2"); //344 sued
		
		handler.addTermStop("812013.2"); // m44 einsetzer gross
		//handler.addTermStop("801030.2"); // m44 einsetzer klein
		handler.addTermStop("812020.2"); //m44 sued
		events.addHandler(handler);
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		reader.parse(filename);
		
		handler.printStats();
	}
	
	public static void main(String[] args) {
		Scenario s = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new TransitScheduleReader(s).readFile("TODO path missing");
		
		DelayEvalStop delayEval = new DelayEvalStop(s.getTransitSchedule());
		delayEval.setTreshold_s(60);
		delayEval.readEvents("E:/_out/m3_traffic_jam/ITERS/it.0/0.events.xml.gz");

	}
	
	private void setTreshold_s(int treshold) {
		this.treshold = treshold;
	}

	static class DelayHandler implements TransitDriverStartsEventHandler, VehicleDepartsAtFacilityEventHandler, VehicleArrivesAtFacilityEventHandler{
		
		private int stopCounter = 0;
		private int termCounter = 0;
		
		HashMap<Integer, DelayCountBox> delayMap = new HashMap<Integer, DelayCountBox>();
		HashMap<Integer, DelayCountBox> delayMapTerm = new HashMap<Integer, DelayCountBox>();
		HashMap<Integer, DelayCountBox> delayMapTreshold =  new HashMap<Integer, DelayCountBox>();
		HashMap<Integer, DelayCountBox> delayMapTermTreshold  =  new HashMap<Integer, DelayCountBox>();
		LinkedList<Id<TransitStopFacility>> termList = new LinkedList<>();
		
		private int treshold;
		private DelayTracker delayTracker;
		
		private final static Logger dhLog = Logger.getLogger(DelayHandler.class);
		
		public DelayHandler(int treshold, final TransitSchedule schedule){
			this.delayTracker = new DelayTracker(schedule);
			for (int i = 0; i < 30; i++) {
				this.delayMap.put(new Integer(i), new DelayCountBox());
			}
			
			for (int i = 0; i < 30; i++) {
				this.delayMapTerm.put(new Integer(i), new DelayCountBox());
			}
			
			for (int i = 0; i < 30; i++) {
				this.delayMapTreshold.put(new Integer(i), new DelayCountBox());
			}
			
			for (int i = 0; i < 30; i++) {
				this.delayMapTermTreshold.put(new Integer(i), new DelayCountBox());
			}
			
			this.treshold = treshold;
		}
		
		public void addTermStop(String stop){
			this.termList.add(Id.create(stop, TransitStopFacility.class));
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
			
			if(this.termList.contains(event.getFacilityId())){
			
				DelayCountBox delBox = this.delayMapTerm.get(Integer.valueOf((int) event.getTime()/3600));
				DelayCountBox delBoxTreshold = this.delayMapTermTreshold.get(Integer.valueOf((int) event.getTime()/3600));
				if(delay < -1 * this.treshold){
					delBoxTreshold.addEntry(delay);
				} else {
					delBoxTreshold.addEntry(0.0);
				}
				
				delBox.addEntry(Math.min(0.0, delay));
			
				this.delayMapTerm.put(Integer.valueOf((int) event.getTime()/3600), delBox);
				this.delayMapTermTreshold.put(Integer.valueOf((int) event.getTime()/3600), delBoxTreshold); 
				this.termCounter++;
			
			}			

		}

		@Override
		public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		
			DelayCountBox delBox = this.delayMap.get(Integer.valueOf((int) event.getTime()/3600));
			DelayCountBox delBoxTreshold = this.delayMapTreshold.get(Integer.valueOf((int) event.getTime()/3600));
			if(event.getDelay() > this.treshold){
				delBoxTreshold.addEntry(event.getDelay());
			} else {
				delBoxTreshold.addEntry(0.0);
			}
			
			delBox.addEntry(Math.max(0.0, event.getDelay()));
			
			this.delayMap.put(Integer.valueOf((int) event.getTime()/3600), delBox);
			this.delayMapTreshold.put(Integer.valueOf((int) event.getTime()/3600), delBoxTreshold);
			this.stopCounter++;

		}
		
		void printStats(){
			
			if(this.delayMap.size() == this.delayMapTerm.size()){
				
				System.out.println("Stunde, Anzahl der Abfahrten, Verspätung je Abfahrt, Verspätung je Abfahrt > " + this.treshold + " s,  , Anzahl der Ankünfte, Verfrühung je Ankunft, Verfrühung je Ankunft < " + this.treshold + " s");
				for (int i = 0; i < this.delayMap.size(); i++) {
					StringBuffer string = new StringBuffer();
					string.append(i + "-" + (i+1) + ", ");
					string.append(this.delayMap.get(Integer.valueOf(i)).getNumberOfEntries() + ", ");
					string.append(this.delayMap.get(Integer.valueOf(i)).getAverageDelay() + ", ");
//					string.append(this.delayMapTreshold.get(Integer.valueOf(i)).getNumberOfEntries() + ", ");
					string.append(this.delayMapTreshold.get(Integer.valueOf(i)).getAverageDelay() + ", ");
					string.append(", ");
					string.append(this.delayMapTerm.get(Integer.valueOf(i)).getNumberOfEntries() + ", ");
					string.append(this.delayMapTerm.get(Integer.valueOf(i)).getAverageDelay() + ", ");
//					string.append(this.delayMapTermTreshold.get(Integer.valueOf(i)).getNumberOfEntries() + ", ");
					string.append(this.delayMapTermTreshold.get(Integer.valueOf(i)).getAverageDelay());
					System.out.println(string.toString());
				
				}
				
			}
		}		
	}


}
