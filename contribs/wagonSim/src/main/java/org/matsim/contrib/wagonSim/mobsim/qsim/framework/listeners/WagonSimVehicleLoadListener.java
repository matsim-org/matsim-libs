/* *********************************************************************** *
 * project: org.matsim.*                                                   *
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

package org.matsim.contrib.wagonSim.mobsim.qsim.framework.listeners;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.contrib.wagonSim.WagonSimConstants;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.utils.objectattributes.ObjectAttributes;

/**
 * Plugs a handler to the {@link Controler} which observes the locomotives (pt-vehicles), more precisely their
 * load. Furthermore this class provides access to the current-vehicleload (and those of the last iteration).
 * 
 * @author droeder
 *
 */
public final class WagonSimVehicleLoadListener implements StartupListener, IterationStartsListener, IterationEndsListener {

	private static final Logger log = Logger
			.getLogger(WagonSimVehicleLoadListener.class);
	private VehicleLoadHandler handler;
	private ObjectAttributes wagonAttribs;
	private VehicleLoad lastLoad;

	public WagonSimVehicleLoadListener(ObjectAttributes wagonAttribs) {
		this.wagonAttribs = wagonAttribs;
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		this.handler = new VehicleLoadHandler(wagonAttribs);
		handler.vehicleLoad = new VehicleLoad(event.getServices().getScenario().getTransitSchedule());
		handler.reset(-1);
		lastLoad = handler.vehicleLoad;
		event.getServices().getEvents().addHandler(handler);
	}


	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		handler.vehicleLoad = new VehicleLoad(event.getServices().getScenario().getTransitSchedule());
		handler.reset(event.getIteration());
	}
	
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		this.lastLoad = handler.vehicleLoad;
		BufferedWriter w = IOUtils.getBufferedWriter(
				event.getServices().getControlerIO().getIterationFilename(event.getIteration(), "locomotiveLoadPerDepartureAndStop.txt"));
		try {
			w.write(this.lastLoad.toString());
			w.flush();
			w.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @return
	 */
	public final VehicleLoad getLoadOfLastIter() {
		return lastLoad;
	}
	
	/**
	 * @return
	 */
	public VehicleLoad getCurrentLoad() {
		return this.handler.vehicleLoad;
	}
	
	
	public static final String getIdentifier(String vehId, String stopId, int stopIndex){
		String delimeter = "---";
		return new String(vehId + delimeter + 
				stopId + delimeter +
				String.valueOf(stopIndex));
	}
	
	public static final boolean freeCapacityInVehicle(
			String vehId,
			String stopId,
			int stopIndex,
			String wagonId,
			VehicleLoad vehLoad, 
			ObjectAttributes locomotiveAttribs,
			ObjectAttributes wagonAttribs) throws NullPointerException{
		String vehDepId = WagonSimVehicleLoadListener.getIdentifier(vehId, stopId, stopIndex);
		double expectedLengthOfTheTrain = vehLoad.getAdditionalLengthAtStopForVehicle(vehDepId);
		double expectedWeightOfTheTrain = vehLoad.getAdditionalWeightAtStopForVehicle(vehDepId);
		Double maxLoadOfTrain = (Double) locomotiveAttribs.getAttribute(vehId, WagonSimConstants.TRAIN_MAX_WEIGHT);
		Double maxLengthOfTrain = (Double) locomotiveAttribs.getAttribute(vehId, WagonSimConstants.TRAIN_MAX_LENGTH);
		Double lengthOfWagon = (Double) wagonAttribs.getAttribute(wagonId, WagonSimConstants.WAGON_LENGTH);
		Double weightOfWagon = (Double) wagonAttribs.getAttribute(wagonId, WagonSimConstants.WAGON_GROSS_WEIGHT);
// check if the data is available (it should). otherwise, throw error 
		if((lengthOfWagon == null) || (weightOfWagon == null) || (maxLengthOfTrain == null) || (maxLoadOfTrain == null) ){
			throw new NullPointerException("Either the locomotive or the wagon or the necessary attributes are unknown.");
		}
		if((maxLengthOfTrain < (expectedLengthOfTheTrain + lengthOfWagon)) || 
				(maxLoadOfTrain < (expectedWeightOfTheTrain + weightOfWagon))){
			return false;
		}
		return true;
	}
	
	
	private class VehicleLoadHandler implements TransitDriverStartsEventHandler,
														PersonEntersVehicleEventHandler,
														PersonLeavesVehicleEventHandler,
														VehicleArrivesAtFacilityEventHandler{
		
		private VehicleLoad vehicleLoad;
		private List<Id> ptVehId;
		private Map<Id, Integer> veh2StopIndex;
		private Map<Id, Id> vehAtFacility;
		private ObjectAttributes oa;
		private List<Id> ptDrivers;

		VehicleLoadHandler(ObjectAttributes oa){
			this.oa = oa;
		}
		
		@Override
		public void reset(int iteration) {
			vehicleLoad.reset();
			this.ptVehId = new ArrayList<Id>();
			this.veh2StopIndex = new HashMap<Id, Integer>();
			this.vehAtFacility = new HashMap<Id, Id>();
			this.ptDrivers = new ArrayList<Id>();
		}

		@Override
		public void handleEvent(VehicleArrivesAtFacilityEvent event) {
			// handle only ``pt''-vehicles
			if(!ptVehId.contains(event.getVehicleId())) return;
			Id veh = event.getVehicleId();
			LoadPerDeparturePerStop oldLoad = null;
			// get the previous location/load, but not for the first stop
			if(vehAtFacility.containsKey(veh)){
				String id = getIdentifier(veh.toString(), vehAtFacility.get(veh).toString(), veh2StopIndex.get(veh));
				oldLoad = vehicleLoad.load.get(id);
			}
			// personsEnter/leave-events do not know about position. Thus, store this information.
			incStopIndex(veh);
			this.vehAtFacility.put(veh, event.getFacilityId());
			// copy the load to the next stop-departure-combination, for all except the first stop
			if(!(oldLoad == null)){
				String id = getIdentifier(veh.toString(), vehAtFacility.get(veh).toString(), veh2StopIndex.get(veh));
				vehicleLoad.entering(id, oldLoad.weight, oldLoad.length);
			}
		}

		@Override
		public void handleEvent(PersonEntersVehicleEvent event) {
			// handle only ``pt''-vehicles, but not their drivers
			if(!ptVehId.contains(event.getVehicleId())) return;
			if(ptDrivers.contains(event.getPersonId())) return;
			Id veh = event.getVehicleId();
			String id = getIdentifier(veh.toString(), vehAtFacility.get(veh).toString(), veh2StopIndex.get(veh));
			Double weight = (Double) this.oa.getAttribute(event.getPersonId().toString(), WagonSimConstants.WAGON_GROSS_WEIGHT);
			Double length = (Double) this.oa.getAttribute(event.getPersonId().toString(), WagonSimConstants.WAGON_LENGTH);
			this.vehicleLoad.entering(id, weight, length);
		}
		
		@Override
		public void handleEvent(PersonLeavesVehicleEvent event) {
			// handle only ``pt''-vehicles, but not their drivers
			if(!ptVehId.contains(event.getVehicleId())) return;
			if(ptDrivers.contains(event.getPersonId())) return;
			Id veh = event.getVehicleId();
			String id = getIdentifier(veh.toString(), vehAtFacility.get(veh).toString(), veh2StopIndex.get(veh));
			Double weight = (Double) this.oa.getAttribute(event.getPersonId().toString(), WagonSimConstants.WAGON_GROSS_WEIGHT);
			Double length = (Double) this.oa.getAttribute(event.getPersonId().toString(), WagonSimConstants.WAGON_LENGTH);
			this.vehicleLoad.leaving(id, weight, length);
		}

		@Override
		public void handleEvent(TransitDriverStartsEvent event) {
			// intialize information about the vehicles
			this.ptVehId.add(event.getVehicleId());
			this.veh2StopIndex.put(event.getVehicleId(), -1);
			this.ptDrivers.add(event.getDriverId());
		}
		
		private void incStopIndex(Id vehId){
			Integer index = this.veh2StopIndex.get(vehId);
			if(index == null) throw new RuntimeException("you tried to increase the stopIndex for a vehicle which does not exist...");
			this.veh2StopIndex.put(vehId, index + 1);
		}


		
	}

	
	public static final class VehicleLoad {
		
		private Map<String, LoadPerDeparturePerStop> load;

		public VehicleLoad(TransitSchedule schedule) {
			init(schedule);
		}

		/**
		 * @param schedule
		 */
		private void init(TransitSchedule schedule) {
			this.load = new HashMap<String, LoadPerDeparturePerStop>();
			for(TransitLine l: schedule.getTransitLines().values()){
				for(TransitRoute r: l.getRoutes().values()){
					for(Departure d: r.getDepartures().values()){
						for(int i = 0; i < r.getStops().size(); i++){
							TransitRouteStop s = r.getStops().get(i);
							String id = getIdentifier(d.getVehicleId().toString(), s.getStopFacility().getId().toString(), i);
							LoadPerDeparturePerStop lo = this.load.put(id, new LoadPerDeparturePerStop(id));
							if(!(lo == null)){
								throw new RuntimeException("the identifier of this departure-stop-combination is not unique! " + id);
							}
						}
					}
				}
			}
			reset();
		}
		
		/**
		 * 
		 */
		void reset() {
			for(String s: load.keySet()){
				load.get(s).reset();
			}
		}
		
		public void entering(String id, double weight, double length){
			LoadPerDeparturePerStop l = this.load.get(id);
			l.sumUp(weight, length);
		}
		
		public void leaving(String id, double weight, double length){
			LoadPerDeparturePerStop l = this.load.get(id);
			l.subtract(weight, length);
		}
		
		public final double getAdditionalWeightAtStopForVehicle(String id){
			return load.get(id).weight;
		}
		
		public final double getAdditionalLengthAtStopForVehicle(String id){
			return load.get(id).length;
		}
		
		@Override
		public String toString(){
			StringBuffer b = new StringBuffer();
			b.append("id\tlength\tweight\n");
			for(LoadPerDeparturePerStop l: this.load.values()){
				b.append(l.toString() + "\n");
			}
			return b.toString();
		}
		
	}
	
	/**
	 * small inner helper-class to store weight/length for a current departure at a current stop (of a TransitVehicle)
	 * @author droeder
	 *
	 */
	private static class LoadPerDeparturePerStop{
		private Double weight = null;
		private Double length = null;
		private String id;
		
		public LoadPerDeparturePerStop(String id){
			this.id = id;
		}
		
	

		void reset() {
			weight = 0.;
			length = 0.;
		}
		
		void sumUp(Double weight, Double length) throws NullPointerException{
			if(weight == null || length == null){
				throw new NullPointerException("weight: " + weight + "\t length" + length );
			}
			this.weight += weight;
			this.length += length;
		}
		 
		void subtract(Double weight, Double length) throws NullPointerException{
			if(weight == null || length == null){
				throw new NullPointerException("weight: " + weight + "\t length" + length );
			}
			this.weight -= weight;
			this.length -= length;
//			if(this.weight < 0 || this.length < 0){
//				// sometimes this number is below zero (e.g. 10E-14), due roinding error
//				log.warn("weight and length must not below zero. id: " + id + "\t weight:" + this.weight + "\t length:" + this.length);
//			}
		}
		
		@Override
		public String toString(){
			return new String(id + "\t" + length + "\t" + weight);
		}
		
	}

	
}

