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

import javax.inject.Inject;

import org.apache.commons.lang3.mutable.MutableInt;
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
	
	
	//Wir schreiben eine neue Methode, die die bisherige calcRelocations Methode ueberschreibt. Der Methodenaufruf muss dabei natuerlich kompatibel bleiben 
	/* (non-Javadoc)
	 * @see org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy#calcRelocations(java.lang.Iterable)
	 */
	@Override
	public List<Relocation> calcRelocations(Iterable<? extends Vehicle> rebalancableVehicles, double time) {
		
	//WO KOMMT rebalancableVehicles her?
	
		//Erstellt eine Liste bestehenden aus mehreren Relocation Eintraegen. Name der Liste ist relocations.
		List<Relocation> relocations = new ArrayList<>();
 		Map <Id<Vehicle>,Vehicle> idleVehiclesMap = new HashMap<>();
		
		//Wir pruefen welche der rebalancableVehicles fuer die naechsten 600 Sekunden noch im Einsatz sind. 
		for (Vehicle v : rebalancableVehicles){
			if (v.getServiceEndTime()>time+600){
				idleVehiclesMap.put(v.getId(),v);
			}
		}
		
		
		
		//FAHRZEUGBEDARF JE ZELLE
		//Wir berechnen die Fahrzeugnachfrage je Zelle. Als Input uebergeben wir eine Liste mit Idle Fahrzeugen
		Map<String, Integer> requiredAdditionalVehiclesPerZone = calculateZonalVehicleRequirements(idleVehiclesMap, time);
		//FAHRZEUGBEDARF JE ZELLE
		
		
		
		//Erstellen einer Zonen String Liste, ueber die iteriert wird. 
		List<String> zones = new ArrayList<>(requiredAdditionalVehiclesPerZone.keySet());
		
		
		
		//Nun versuchen wir den Fahrzeugbedarf je Zone aufzuloesen
		//Dabei iterieren wir ueber jeder einzelne Zone
		for (String zone : zones){
			
			//Fahrzeugbedarf in Zone wird aus requiredAdditionalVehiclesPerZone extrahiert
			int requiredVehicles = requiredAdditionalVehiclesPerZone.get(zone);
			
			//Es gibt in der Zone ein Fahrzeugbedarf
			if (requiredVehicles>0){
				
				
				//Fuer jedes angeforderte Fahrzeug wird das nachstgelegene Fahrzeug gesucht
				for (int i = 0; i< requiredVehicles; i++){
					Geometry z = zonalSystem.getZone(zone);
					if (z==null) {throw new RuntimeException();	};
					Coord zoneCentroid = MGC.point2Coord(z.getCentroid());
					Vehicle v = findClosestVehicle(idleVehiclesMap,zoneCentroid, time);
					
					
					//Wenn ein Fahrzeug gefunden wurde, muss dieses natuerlich von der Liste der idleVehiclesMap entfernt werden
					if (v!=null){
						idleVehiclesMap.remove(v.getId());
						//Hinzufuegen eines neuen Relocation Objekts, bestehend aus v und link
						relocations.add(new Relocation(v, NetworkUtils.getNearestLink(network, zonalSystem.getZoneCentroid(zone))));
					}
				}
			}
		}
//		relocations.forEach(l->Logger.getLogger(getClass()).info(l.vehicle.getId().toString()+"-->"+l.link.getId().toString()));
		
		return relocations;
	}

	/**
	 * @param idleVehicles2
	 * @return
	 */
	private Vehicle findClosestVehicle(Map<Id<Vehicle>, Vehicle> idles, Coord coord, double time) {
		double closestDistance = Double.MAX_VALUE;
		Vehicle closestVeh = null;
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
	//Berechnung des Fahrzeugbedarfs je Zone fuer die naechsten 60 min
	private Map<String, Integer> calculateZonalVehicleRequirements(Map <Id<Vehicle>,Vehicle> rebalancableVehicles, double time) {
		
		//Wir erhalten zunaechst die Nachfrage fuer die naechsten 60 min fuer jede Zone
		Map<String, MutableInt> expectedDemand = demandAggregator.getExpectedDemandForTimeBin(time+5);
		
		//Wenn wir in der ersten Iteration sind gibt es keine Nachfrageschaetzung. Daher wird eine leere Hashmap erzeugt. Es gibt dann auch kein Vehicle Relocationing
		if (expectedDemand==null){
			return new HashMap<>();
		}
		
		//Wir extrahieren die Gesamtnachfrage an Fahrzeugen fuer die naechsten Zeitscheibe
		//Die Gesamtnachfrage wird mit 0 initalisiert
		final MutableInt totalDemand = new MutableInt(0);
		//Wir loopen ueber alle Zonen und addieren jeweils die Zonennachfrage auf die totalDemand obendrauf. (Gesamtabfahrten)
		expectedDemand.values().forEach(demand->totalDemand.add(demand.intValue()));
//		Logger.getLogger(getClass()).info("Rebalancing at "+Time.writeTime(time)+" vehicles: " + rebalancableVehicles.size()+ " expected demand :"+totalDemand.toString());

		//Liefert als Ergebnis eine Hashmap aus Zone und Anzahl von Fahrzeugen
		Map<String,Integer> requiredAdditionalVehiclesPerZone = new HashMap<>();
		
		//Wir iterieren ueber die expectedDemand je Zone 
		for (Entry<String, MutableInt> entry : expectedDemand.entrySet()){
			
			//demand ist die Nachfrage (Abfahrten) in unserer Zelle.
			double demand = entry.getValue().doubleValue();
			
			//FAHRZEUGBEDARF?
			//ICH VERMUTE, DASS DIESE ZEILE DEN FAHRZEUGBEDARF PRO ZONE SCHAETZT! UND ZWAR IN PROPRTION ZW. ZONENNACHFRAGE/GESAMTNACHFRAGE
			//SIND KEINE FAHRZEUG REBALANCABLE (rebalancableVehicles==0), KOENNEN WIR AUCH KEINE FAHRZEUGE UMSCHICHTEN
			int vehPerZone = (int) Math.ceil((demand / totalDemand.doubleValue()) * rebalancableVehicles.size());
			int idleVehiclesInZone = 0;
			
			//Der Fahrzeugbedarf kann die Nachfrage grundsaetzlich nicht ueberschreiten
			if (vehPerZone>demand){
				vehPerZone=(int) demand;
			}
			
//			if (demand>0){
//			System.out.println("Zone "+entry.getKey() + " requires " +vehPerZone + " vehicles" + " | requests " +demand);
//			}
			
			//Wieviele Idle Fahrzeuge haben wir bereits in unserer Zone?
			LinkedList<Id<Vehicle>> idleVehicleIds = idleVehicles.getIdleVehiclesPerZone(entry.getKey());
			

			//Pruefen ob wir idle Fahrzeuge in unserer Zone haben?
			//Wir haben gar keine idle Fahrzeuge in unserer Zone. Wir koennen sofort welche Anfordern
			if (idleVehicleIds!=null & (!idleVehicleIds.isEmpty()))
			{
			
				idleVehiclesInZone = idleVehicleIds.size();
				
				//WAS PASSIERT HIER? WAS MACHT POLL
				for (int i = 0; i<vehPerZone;i++)
				{
					if (!idleVehicleIds.isEmpty()){
						Id<Vehicle> vid = idleVehicleIds.poll(); 
						if (rebalancableVehicles.remove(vid)==null) {
	//					Logger.getLogger(getClass()).error("Vehicle "+vid.toString()+" not idle for rebalancing.");	
						}
					}
				}
				
				
				//Ist der Fahrzeugbedarf in unserer Zone schon gedeckt?
				//Wir brauchen Fahrzeuge aus anderen Zonen (zoneSurplus)
				//Fahrzeugbedarf - Idle Fahrzeuge in Zone
				int zoneSurplus = (vehPerZone - idleVehiclesInZone);
				
				//Wenn Fahrzeugbedarf negativ, dann werden keine weiteren Fahrzeuge angefordert!
				if (zoneSurplus<0){
					
					//Wir haben genug Fahrzeuge in unserer Zone
					zoneSurplus = 0;
				}
				
				requiredAdditionalVehiclesPerZone.put(entry.getKey(), zoneSurplus);
			
			//Wir haben gar keine idle Fahrzeuge in unserer Zone. Wir koennen sofort welche Anfordern
			} else {
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
