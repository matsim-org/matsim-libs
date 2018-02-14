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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
//import org.matsim.contrib.drt.analysis.zonal.ZonalDemandAggregator;
import org.matsim.contrib.drt.analysis.zonal.ZonalIdleVehicleCollector;
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
import org.matsim.pt.transitSchedule.api.TransitLine;

import com.google.inject.name.Named;
import com.vividsolutions.jts.geom.Geometry;

/**
 * @author  axer
 *
 */
/**
 *
 */
public class DemandBasedRebalancingStrategyMy implements RebalancingStrategy {

	private ZonalIdleVehicleCollector idleVehicles;
	private ZonalDemandAggregatorMy demandAggregator;
	private DrtZonalSystem zonalSystem;
	private Network network;
	

	/**
	 * 
	 */
	@Inject
	public DemandBasedRebalancingStrategyMy(ZonalIdleVehicleCollector idleVehicles, ZonalDemandAggregatorMy demandAggregator, DrtZonalSystem zonalSystem, @Named(DvrpModule.DVRP_ROUTING) Network network) {
		this.idleVehicles = idleVehicles;
		this.demandAggregator = demandAggregator;
		this.zonalSystem = zonalSystem;
		this.network = network;
		
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
		
		int totalRequiredVehicles = 0;
		int totalReloVehice = 0;
		
		
		Map<Id<Vehicle>, Vehicle> idleVehiclesMap = rebalancableVehicles.filter(v -> v.getServiceEndTime() > time + 3600).collect(Collectors.toMap(v -> v.getId(), v -> v));

		
		//We create a new map of idle vehicles based on rebalancableVehicles and we do not use idleVehicles
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
			
			totalRequiredVehicles=totalRequiredVehicles+requiredVehicles;
			

			//Required Vehicles per zone
			if (requiredVehicles > 0) {
				
				
				for (int i = 0; i < requiredVehicles; i++) {
					Geometry z = zonalSystem.getZone(zone);
					if (z == null) {
						throw new RuntimeException();
					} ;
					Coord zoneCentroid = MGC.point2Coord(z.getCentroid());
					Vehicle v = findClosestVehicle(idleVehiclesMap, zoneCentroid, time);

					if (v != null) {
						idleVehiclesMap.remove(v.getId());
						relocations.add(new Relocation(v,NetworkUtils.getNearestLink(network, zonalSystem.getZoneCentroid(zone))));
						totalReloVehice = totalReloVehice+1;
					} 
					
				}
			}
			

			}
		System.out.println("Total required vehicles = "+ totalRequiredVehicles + " || Total relocated vehicles = "+totalReloVehice + " || " + "Available idle vehicles = " + initalIdleMapSize);
		relocations.forEach(l->Logger.getLogger(getClass()).info(l.vehicle.getId().toString() + "||"  + getLastLink(l.vehicle,time)   +" --> "+l.link.getId().toString()));

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

	/**
	 * @param rebalancableVehicles
	 * @return 
	 */
	//Predict vehicle demand based on expected trips at time + timeShift
	private Map<String, Integer> calculateZonalVehicleRequirements(Map <Id<Vehicle>,Vehicle> idleVehiclesMap,Map<String,LinkedList<Id<Vehicle>>>vehiclesPerZone, double time) {
		
		//timeShifts defines how far we are looking into the feature demand situation! 
		double timeShift = 300.0;
		
		//Get expected demand in network at time+timeShift
		Map<String, MutableInt> expectedDemand = demandAggregator.getExpectedDemandForTimeBin(time+timeShift);
		
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

}
