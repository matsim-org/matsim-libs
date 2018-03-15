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

/**
 * 
 */
package vwExamples.peoplemoverVWExample.CustomRebalancing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.schedule.DrtTask;
import org.matsim.contrib.drt.schedule.DrtTask.DrtTaskType;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.util.distance.DistanceUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import com.google.inject.name.Named;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;



/**
 * @author  axer
 *
 */
/**
 *
 */
public class DemandBasedRebalancingStrategyMy implements RebalancingStrategy {

	private ZonalIdleVehicleCollectorMy idleVehicles;
	private ZonalDemandAggregatorMy demandAggregator;
	private DrtZonalSystem zonalSystem;
	private Network network;
	private ZonalRelocationAggregatorMy reloacatedVehicles;
	private RelocationWriter relocationWriter;
	


	@Inject
	public DemandBasedRebalancingStrategyMy(ZonalIdleVehicleCollectorMy idleVehicles, ZonalDemandAggregatorMy demandAggregator,
			DrtZonalSystem zonalSystem, @Named(DvrpModule.DVRP_ROUTING) Network network, ZonalRelocationAggregatorMy reloacatedVehicles,
			RelocationWriter relocationWriter) {
		this.reloacatedVehicles = reloacatedVehicles;
		this.idleVehicles = idleVehicles;
		this.demandAggregator = demandAggregator;
		this.zonalSystem = zonalSystem;
		this.network = network;
		this.relocationWriter = relocationWriter;
		
	}
	
	@Override
	public List<Relocation> calcRelocations(Stream<? extends Vehicle> rebalancableVehicles, double time) {
		

		System.out.println("Iteration: "+ demandAggregator.getnOfIterStarts()  + " || Measured open request in this iteration: " +demandAggregator.getOpenRequests());
		
		
		Map<String,LinkedList<Id<Vehicle>>> vehiclesPerZone = new HashMap<>();
		Map<Id<Vehicle>,String> zonePerVehicle = new HashMap<>();
		
		//Populate vehiclesPerZone with LinkedLists per Zone (z)
		for (String z : zonalSystem.getZones().keySet()){
			vehiclesPerZone.put(z,new LinkedList<Id<Vehicle>>());
		}
		
		int totalReloVehice = 0;
		int requiredVehiclesAllZones = 0;
		
		Map<Id<Vehicle>, Vehicle> idleVehiclesMap = rebalancableVehicles.filter(v -> v.getServiceEndTime() > time + 3600).collect(Collectors.toMap(v -> v.getId(), v -> v));

		//FILTER FOR ALREADY RELOCATED VEHICLES
		//Check whether vehicles has been already relocated. Relocated vehicles are stored in a blacklist
		//cleanVehicleBlackListMap drops vehicles from the idleVehiclesMap because they have already been relocated within the last 600 seconds.
		idleVehiclesMap = cleanVehicleBlackListMap(time, idleVehiclesMap);
		
		//FILTER TO TAKE ONLY VEHICLES THAT ARE ALREADY IDLE FOR A LONG TIME PERIOD (Percentile-based)
		idleVehiclesMap = getIdleTimeVehicleSubset(idleVehiclesMap, time, 30.0);
		
		
		//We create a new map of idle vehicles map based on rebalancableVehicles and we do not use idleVehicles of the demand aggregator
		for ( Vehicle idleVeh : idleVehiclesMap.values()) {
			Link link = getLastLink(idleVeh,time);
			Id<Vehicle> vid = Id.create(idleVeh.getId(), Vehicle.class);
			String zone = zonalSystem.getZoneForLinkId(Id.create(link.getId(), Link.class));
			vehiclesPerZone.get(zone).add(vid);
			zonePerVehicle.put(vid, zone);
		}
		
		int initalIdleMapSize = idleVehiclesMap.size();
	
		
		List<Relocation> relocations = new ArrayList<>();
		Map<String, Integer> requiredAdditionalVehiclesPerZone = calculateZonalVehicleRequirements(idleVehiclesMap,vehiclesPerZone,time);
		List<String> zones = new ArrayList<>(requiredAdditionalVehiclesPerZone.keySet());
		for (String zone : zones) {
			
			
			int requiredVehicles = requiredAdditionalVehiclesPerZone.get(zone);
			requiredVehiclesAllZones = requiredVehiclesAllZones+requiredVehicles;

			//Required Vehicles per zone
			if (requiredVehicles > 0) {
				
				
				for (int i = 0; i < requiredVehicles; i++) {
					Geometry z = zonalSystem.getZone(zone);
					if (z == null) {
						throw new RuntimeException();
					} ;
					Coord zoneCentroid = MGC.point2Coord(z.getCentroid());
					Vehicle v = findClosestVehicle(idleVehiclesMap, zoneCentroid, time);
					//Vehicle v = findLongIdleVehicle(idleVehiclesMap, time);

					if (v != null) {
						idleVehiclesMap.remove(v.getId());
						relocations.add(new Relocation(v,NetworkUtils.getNearestLink(network, zonalSystem.getZoneCentroid(zone))));
						totalReloVehice = totalReloVehice+1;
						
						//Add this vehicle into vehicleBlackListMap and store 
						demandAggregator.vehicleBlackListMap.put(v.getId(), (int) time);
					} 
					
				}
			}
			

			}
		System.out.println("Total required vehicles = "+ requiredVehiclesAllZones + " || Total relocated vehicles = "+totalReloVehice + " || " + "Available idle vehicles = " + initalIdleMapSize);
//		relocations.forEach(l->Logger.getLogger(getClass()).info(l.vehicle.getId().toString() + "||"  + getLastLink(l.vehicle,time)   +" --> "+l.link.getId().toString()));

		for (Relocation reloc : relocations)
		{
			System.out.println("Moving vehicle = "+ reloc.vehicle.getId().toString() + " from Link = " + getLastLink(reloc.vehicle,time).getId().toString() +" --> " + reloc.link.getId().toString());
			
			double x1=getLastLink(reloc.vehicle,time).getCoord().getX();
			double y1=getLastLink(reloc.vehicle,time).getCoord().getY();
			double x2=reloc.link.getCoord().getX();
			double y2=reloc.link.getCoord().getY();
			
			LineString lineSegment = new GeometryFactory().createLineString(new Coordinate[]{new Coordinate(x1,y1),new Coordinate(x2,y2)}) ;
			
			relocationWriter.setRelocation(reloc.vehicle.getId().toString()+";"+ time +";"+lineSegment.toString());
			//Store relocation in relocations
			Double bin = reloacatedVehicles.getBinForTime(time);
			String zoneId = zonalSystem.getZoneForLinkId(reloc.link.getId());
			reloacatedVehicles.relocations.get(bin).get(zoneId).increment();
		}
		
		return relocations;
}

	/**
	 * @param idleVehicles2
	 * @return
	 */
	private Vehicle findClosestVehicle(Map<Id<Vehicle>, Vehicle> idles, Coord coord, double time) {
		double closestDistance = Double.MAX_VALUE;
		Vehicle closestVeh = null;
		
		//Wir iterieren über alle idle Fahrzeuge
		for (Vehicle v : idles.values()){
			Link vl = getLastLink(v,time);
			if (vl!=null){
				double distance = DistanceUtils.calculateDistance(coord, vl.getCoord());
				if (distance<closestDistance){
					closestDistance = distance;
					closestVeh = v;
				}
			}
		}
		return closestVeh;
	}
	
	
	private Vehicle findLongIdleVehicle(Map<Id<Vehicle>, Vehicle> idles,double time) {
		//Assign actual simulation time
		//We are searching the most long idle vehicle --> min(idleBegin)
		double earliesIdleBegin = time;
		Vehicle longestIdleVeh = null;
				
		//Iterate over all idle vehicles
		for (Entry<Id<Vehicle>, Integer> v : idleVehicles.vehicleIdleMap.entrySet()){
				//Get the start of idle time 
				double idleBegin = v.getValue();
				if (idleBegin<earliesIdleBegin){
					earliesIdleBegin = idleBegin;
					longestIdleVeh = idles.get(v.getKey());
				}
			
		}
		if (longestIdleVeh!=null) {
			idleVehicles.vehicleIdleMap.remove(longestIdleVeh.getId());
			System.out.println("Took vehicle: " +longestIdleVeh.getId().toString() +" | IdleTime: "+ (time - earliesIdleBegin) );
		}
		return longestIdleVeh;
	}
	
	
	private Map<Id<Vehicle>, Vehicle> getIdleTimeVehicleSubset(Map<Id<Vehicle>, Vehicle> idles, double time, double p) {
		
		int initalIdles = idles.size();
		
		if (!idles.isEmpty()) {
			
		
		//Create list of Double Objects
		List<Double> idleTimes = new ArrayList<Double>();

		
		
		
		//Collect idle times from vehicles
		for (Vehicle v : idles.values()){
			
			
			//Add idleTimes into ArrayList
			//If we find the the vehicle in vehicleIdleMap it is already igle
			//Else we can not calculate an idleTime value
			if (idleVehicles.vehicleIdleMap.containsKey(v.getId()))
			{
			double idleTime = (time - (double) idleVehicles.vehicleIdleMap.get(v.getId()));
			idleTimes.add(idleTime);
			}
		}
		
		//Convert to double array from 
		double[] arr = idleTimes.stream().mapToDouble(Double::doubleValue).toArray(); 

		
		//A high idleTimePercentile 
		//double idleTimePercentile = percentile.evaluate(arr,p);
		double idleTimePercentile = new Percentile(p).evaluate(arr);
		
		
		
		
		//Drop vehicles from idles list that are not waiting a long enough time period.
		//The time period scales with the provided percentile value = p
		
		
		Iterator<Entry<Id<Vehicle>, Vehicle>> it = idles.entrySet().iterator();
		
		while(it.hasNext()){
			
			Entry<Id<Vehicle>, Vehicle> vehicleEntry = it.next();
			
			if (idleVehicles.vehicleIdleMap.containsKey(vehicleEntry.getKey()))
			{
			
				double idleTime = time - (double) idleVehicles.vehicleIdleMap.get(vehicleEntry.getKey());
				
				if (idleTime<idleTimePercentile)
				{
					
					//System.out.println("Dropped busy vehicle!");
					it.remove();
				}
			
			}
			
		}
		
		System.out.println("Idle time threshold = " + idleTimePercentile  + " All idle vehicles = " + initalIdles + " Remaining idle vehicles = " + idles.size());
		return idles;
		}
		
		else return idles;
	}

	/**
	 * @param rebalancableVehicles
	 * @return 
	 */
	//Predict vehicle demand based on expected trips at time + timeShift
	private Map<String, Integer> calculateZonalVehicleRequirements(Map <Id<Vehicle>,Vehicle> idleVehiclesMap,Map<String,LinkedList<Id<Vehicle>>>vehiclesPerZone, double time) {
		
		//timeShifts defines how far we are looking into the feature demand situation! 
		double timeShift = 300.0;
		
		//Get expected demand in network at time+timeShift
		Map<String, MutableInt> expectedDemand = demandAggregator.getExpectedRejectsForTimeBin(time+timeShift);
		
		//In the first iteration we have no expected demand, return empty map!
		if (expectedDemand==null){
			return new HashMap<>();
		}
		
		
		
		final MutableInt totalDemand = new MutableInt(0);
		
		
		
		expectedDemand.values().forEach(demand->totalDemand.add(demand.intValue()));
		
		System.out.println("Total Demand of open request: "+ totalDemand.toString());
		
//		Logger.getLogger(getClass()).info("Rebalancing at "+Time.writeTime(time)+" vehicles: " + rebalancableVehicles.size()+ " expected demand :"+totalDemand.toString());

		//Number of required vehicles per Zone
		Map<String,Integer> requiredAdditionalVehiclesPerZone = new HashMap<>();
		
		for (Entry<String, MutableInt> entry : expectedDemand.entrySet()){
			
			double demand = entry.getValue().doubleValue();
			
			int zoneSurplus =0;

			//Proportional split over zones
			int vehPerZone = (int) Math.ceil((demand / totalDemand.doubleValue()) * idleVehiclesMap.size());
//			int vehPerZone = (int) demand;
			
			
			
			//Do not send more vehicles than request in zone
			if (vehPerZone>demand){
				vehPerZone=(int) demand;
			}
			
			
			//Check the actual vehicle situation in this zone
			
			LinkedList<Id<Vehicle>> idleVehicleIds = vehiclesPerZone.get(entry.getKey());
			
			
			if (idleVehicleIds!=null & (!idleVehicleIds.isEmpty()))
			{
				
				//Assign idle vehicle of this zone
				int idleVehiclesInZone = idleVehicleIds.size();
				for (int i = 0; i<vehPerZone;i++)
				{
					if (!idleVehicleIds.isEmpty()){
						Id<Vehicle> vid = idleVehicleIds.poll(); 
						if (idleVehiclesMap.remove(vid)==null) {
	//					Logger.getLogger(getClass()).error("Vehicle "+vid.toString()+" not idle for rebalancing.");	
						}
					}
				}
				
				zoneSurplus = (vehPerZone - idleVehiclesInZone);
				
				//Request vehicles from other zones
				if (zoneSurplus<0){
					zoneSurplus = 0;
				}
				requiredAdditionalVehiclesPerZone.put(entry.getKey(), zoneSurplus);
			
			//Directly request vehicles from other zones
			} 
			else 
			{
			requiredAdditionalVehiclesPerZone.put(entry.getKey(),vehPerZone );
			}
				
		}
		return requiredAdditionalVehiclesPerZone;
		
	}
	
	private Link getLastLink(Vehicle vehicle, double time) {
		Schedule schedule = vehicle.getSchedule();
		if (time >= vehicle.getServiceEndTime() || schedule.getStatus() != ScheduleStatus.STARTED) {
			return null;
		}

		DrtTask currentTask = (DrtTask)schedule.getCurrentTask();
		if( currentTask.getTaskIdx() == schedule.getTaskCount() - 1 // last task (because no prebooking)
				&& currentTask.getDrtTaskType() == DrtTaskType.STAY ){
			DrtStayTask st = (DrtStayTask) currentTask;
			return st.getLink();
		}
		else return null;
	}
	
	private Map<Id<Vehicle>, Vehicle> cleanVehicleBlackListMap(double time, Map<Id<Vehicle>, Vehicle> idleVehiclesMap) {
	
		//Define an iterator in order to modify blacklist within a loop		
		Iterator<Entry<Id<Vehicle>, Integer>> it = demandAggregator.vehicleBlackListMap.entrySet().iterator();
		
		double relocationStopTime = 300.0;
		
		while(it.hasNext())
		{
			Entry<Id<Vehicle>, Integer> vehicleEntry = it.next();
			double timeSinceVehicleRelocation = time-vehicleEntry.getValue();
			
			if (timeSinceVehicleRelocation<relocationStopTime)
			{
			//Keep vehicle in vehicleBlackListMap and remove it from idleVehiclesMap
			//Drop Vehicle from idleVehiclesMap because it has already been relocated within the last relocationStopTime
			idleVehiclesMap.remove(vehicleEntry.getKey());
			System.out.println("Dropped vehicle from idleMap: "+vehicleEntry.getKey().toString() + " || time since relocation: "+ timeSinceVehicleRelocation);
			}
			else {
				//Remove vehicle from vehicleBlackListMap it is available in idleVehiclesMap and could be relocated again
				System.out.println("Dropped vehicle from vehicleBlackListMap: "+vehicleEntry.getKey().toString() + " || time since relocation: "+ timeSinceVehicleRelocation);	
				it.remove();
//				demandAggregator.vehicleBlackListMap.remove(vehicleEntry.getKey());
			}
			
		}
		
		return idleVehiclesMap;
		
	}
	

	
	

}
