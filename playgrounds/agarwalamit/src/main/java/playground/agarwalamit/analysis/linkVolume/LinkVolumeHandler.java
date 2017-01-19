/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.analysis.linkVolume;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import playground.agarwalamit.mixedTraffic.MixedTrafficVehiclesUtils;

/**
 * @author amit
 */
public class LinkVolumeHandler implements LinkLeaveEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {

	private static final Logger LOG = Logger.getLogger(LinkVolumeHandler.class);
	private final Map<Id<Link>, Map<Integer,Double>> linkId2Time2Count = new HashMap<>();
	private final Map<Id<Link>, Map<Integer,Double>> linkId2Time2PCUVol = new HashMap<>();
	private final Map<Id<Link>, Map<Integer,List<Id<Vehicle>>>> linkId2Time2Persons = new HashMap<>();

	private final Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler();
	private final Map<Id<Person>, String> person2mode = new HashMap<>();
	private final Map<String,Double> mode2pcu = new HashMap<>();
	
	public LinkVolumeHandler () {
		this(null);
	}
	
	public LinkVolumeHandler (final String VehiclesFile) {
		LOG.info("Starting volume count on links.");
		reset(0);
		if (VehiclesFile!=null) {
			Vehicles vehs = VehicleUtils.createVehiclesContainer();
			VehicleReaderV1 vr = new VehicleReaderV1(vehs);
			vr.readFile(VehiclesFile);

			vehs.getVehicleTypes().values().forEach(
					vehicleType -> mode2pcu.put(vehicleType.getId().toString(), vehicleType.getPcuEquivalents())
			);
		}
	}
	
	@Override
	public void reset(int iteration) {
		this.linkId2Time2Count.clear();
		this.linkId2Time2Persons.clear();
		this.linkId2Time2PCUVol.clear();
		this.delegate.reset(iteration);
	}

	private int getSlot(final double time){
		return (int)time/3600;
	}
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		String mode = person2mode.get( this.delegate.getDriverOfVehicle(event.getVehicleId()) );
		if(mode==null) {
			throw new RuntimeException("No mode found for person "+ this.delegate.getDriverOfVehicle(event.getVehicleId()) +" while leaving link. Event : "+event.toString()+". Should not happen.");
		}
		double pcu = mode2pcu.isEmpty() ? MixedTrafficVehiclesUtils.getPCU(mode) : mode2pcu.get(mode);
		
		int slotInt = getSlot(event.getTime());
		Map<Integer, Double> volsTime = new HashMap<>();
		Map<Integer, Double> pcuVolsTime = new HashMap<>();
		Map<Integer, List<Id<Vehicle>>> time2persons = new HashMap<>();

		Id<Link> linkId = event.getLinkId();
		if(this.linkId2Time2Count.containsKey(linkId)){
			volsTime =	this.linkId2Time2Count.get(linkId);
			pcuVolsTime = this.linkId2Time2PCUVol.get(linkId);
			
			time2persons = this.linkId2Time2Persons.get(linkId);
			List<Id<Vehicle>> vehicles = new ArrayList<>();
			
			if(volsTime.containsKey(slotInt)) {
				vehicles = time2persons.get(slotInt);
				vehicles.add(event.getVehicleId());
				
				volsTime.put(slotInt, volsTime.get(slotInt)+1);
				pcuVolsTime.put(slotInt, pcuVolsTime.get(slotInt)+pcu);
			}else {
				vehicles.add(event.getVehicleId());
				time2persons.put(slotInt, vehicles);
				this.linkId2Time2Persons.put(linkId, time2persons);
				
				volsTime.put(slotInt, 1.0);
				pcuVolsTime.put(slotInt, pcu);
			} 
		}else {
			List<Id<Vehicle>> vehicles = new ArrayList<>();
			vehicles.add(event.getVehicleId());
			time2persons.put(slotInt,vehicles);
			this.linkId2Time2Persons.put(linkId, time2persons);
			
			volsTime.put(slotInt, 1.0);
			pcuVolsTime.put(slotInt, pcu);
		}
		
		this.linkId2Time2Count.put(linkId, volsTime);
		this.linkId2Time2PCUVol.put(linkId, pcuVolsTime);
	}

	public Map<Id<Link>, Map<Integer, Double>> getLinkId2TimeSlot2LinkVolumePCU(){
		return this.linkId2Time2PCUVol;
	}
	
	public Map<Id<Link>, Map<Integer, Double>> getLinkId2TimeSlot2LinkCount(){
		return this.linkId2Time2Count;
	}
	
	public Map<Id<Link>, Map<Integer, List<Id<Vehicle>>>> getLinkId2TimeSlot2VehicleIds(){
		return this.linkId2Time2Persons;
	}
	
	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		this.delegate.handleEvent(event);
		this.person2mode.remove(event.getPersonId());
	}
	
	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		this.delegate.handleEvent(event);
		this.person2mode.put(event.getPersonId(), event.getNetworkMode());
	}
}