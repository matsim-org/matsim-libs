/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.visum;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.*;
import org.matsim.visum.VisumNetwork;
import org.matsim.visum.VisumNetwork.VehicleCombination;
import org.matsim.visum.VisumNetwork.VehicleUnit;
import playground.johannes.gsv.analysis.TransitLineAttributes;

import java.util.*;

public class Visum2TransitSchedule {

	private static final Logger log = Logger.getLogger(Visum2TransitSchedule.class);

	private final VisumNetwork visum;
	private final TransitSchedule schedule;
	private final Vehicles vehicles;
	//	private final CoordinateTransformation coordinateTransformation = new Kilometer2MeterTransformation();
	private final CoordinateTransformation coordinateTransformation = new IdentityTransformation();
	private final Map<String, String> transportModes = new HashMap<String, String>();

	private Map<String, String> transportSystems;
	
	public Visum2TransitSchedule(final VisumNetwork visum, final TransitSchedule schedule, final Vehicles vehicles) {
		this.visum = visum;
		this.schedule = schedule;
		this.vehicles = vehicles;
	}

	public void registerTransportMode(final String visumTransportMode, final String transportMode) {
		this.transportModes.put(visumTransportMode, transportMode);
	}

	public void setTransportSystems(Map<String, String> map) {
		transportSystems = map;
	}
	
	public void convert() {
		TransitLineAttributes attrs = new TransitLineAttributes();
		long vehId = 0;

		TransitScheduleFactory builder = this.schedule.getFactory();

		// 1st step: convert vehicle types
		VehiclesFactory vb = this.vehicles.getFactory();
		for (VehicleCombination vehComb : this.visum.vehicleCombinations.values()) {
			VehicleType type = vb.createVehicleType(Id.create(vehComb.id, VehicleType.class));
			type.setDescription(vehComb.name);
			VehicleCapacity capacity = vb.createVehicleCapacity();
			VehicleUnit vu = this.visum.vehicleUnits.get(vehComb.vehUnitId);
			capacity.setSeats(Integer.valueOf(vehComb.numOfVehicles * vu.seats));
			capacity.setStandingRoom(Integer.valueOf(vehComb.numOfVehicles * (vu.passengerCapacity - vu.seats)));
			type.setCapacity(capacity);
			this.vehicles.addVehicleType(type);
		}
		// insert a dummy vehicle type if no type is specified
		Id<Vehicle> dummyVehId = Id.create(0, Vehicle.class);
		VehicleType type = vb.createVehicleType(Id.create(dummyVehId, VehicleType.class));
		type.setDescription("unknown");
		VehicleCapacity capacity = vb.createVehicleCapacity();
		//VehicleUnit vu = this.visum.vehicleUnits.get(vehComb.vehUnitId);
		capacity.setSeats(Integer.MAX_VALUE);
		capacity.setStandingRoom(Integer.MAX_VALUE);
		type.setCapacity(capacity);
		this.vehicles.addVehicleType(type);

		// 2nd step: convert stop points
		final Map<Id<TransitStopFacility>, TransitStopFacility> stopFacilities = new TreeMap<>();

		for (VisumNetwork.StopPoint stopPoint : this.visum.stopPoints.values()){
			Coord coord = this.coordinateTransformation.transform(this.visum.stops.get(this.visum.stopAreas.get(stopPoint.stopAreaId).StopId).coord);
			TransitStopFacility stop = builder.createTransitStopFacility(Id.create(stopPoint.id, TransitStopFacility.class), coord, false);
			stop.setName(stopPoint.name);
			stopFacilities.put(Id.create(stopPoint.id, TransitStopFacility.class), stop);
			this.schedule.addStopFacility(stop);
		}

		// 3rd step: convert lines
		int i = 0;
		for (VisumNetwork.TransitLine line : this.visum.lines.values()){
			i++;
			if(i % 10 == 0) {
				log.info(String.format("Converting %s of %s lines...", i, this.visum.lines.size()));
			}
			TransitLine tLine = builder.createTransitLine(Id.create(line.id, TransitLine.class));
			attrs.setTransportSystem(tLine.getId().toString(), transportSystems.get(line.tCode));

			for (VisumNetwork.TimeProfile timeProfile : this.visum.timeProfiles.values()){
				VisumNetwork.VehicleCombination defaultVehCombination = this.visum.vehicleCombinations.get(timeProfile.vehCombNr);
				if (defaultVehCombination == null) {
					defaultVehCombination = this.visum.vehicleCombinations.get(line.vehCombNo);
				}
//				VehicleType defaultVehType = null;
				VehicleType defaultVehType = this.vehicles.getVehicleTypes().get(dummyVehId);
				if (defaultVehCombination != null) {
					defaultVehType = this.vehicles.getVehicleTypes().get(Id.create(defaultVehCombination.id, VehicleType.class));
				}
				// convert line routes
				if (timeProfile.lineName.equals(line.id)) {
					List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
					//  convert route profile
					for (VisumNetwork.TimeProfileItem tpi : this.visum.timeProfileItems.values()){
						if (tpi.lineName.equals(line.id.toString()) && tpi.lineRouteName.equals(timeProfile.lineRouteName.toString()) && tpi.timeProfileName.equals(timeProfile.index.toString()) && tpi.dirCode.equals(timeProfile.dirCode)){
							TransitRouteStop s = builder.createTransitRouteStop(stopFacilities.get(this.visum.lineRouteItems.get(line.id.toString() +"/"+ timeProfile.lineRouteName.toString()+"/"+ tpi.lRIIndex.toString()+"/"+tpi.dirCode).stopPointNo),Time.parseTime(tpi.arr),Time.parseTime(tpi.dep));
							stops.add(s);
						}
					}
					String mode = this.transportModes.get(line.tCode);
					if (mode == null) {
						log.error("Could not find TransportMode for " + line.tCode + ", more info: " + line.id);
					}
					TransitRoute tRoute = builder.createTransitRoute(Id.create(timeProfile.lineName.toString()+"."+timeProfile.lineRouteName.toString()+"."+ timeProfile.index.toString()+"."+timeProfile.dirCode, TransitRoute.class),null,stops,mode);
					//  convert departures
					for (VisumNetwork.Departure d : this.visum.departures.values()){
						if (d.lineName.equals(line.id.toString()) && d.lineRouteName.equals(timeProfile.lineRouteName.toString()) && d.TRI.equals(timeProfile.index.toString()) && d.dirCode.equals(timeProfile.dirCode)) {
							Departure departure = builder.createDeparture(Id.create(d.index, Departure.class), Time.parseTime(d.dep));
							VehicleType vehType = defaultVehType;
							if (d.vehCombinationNo != null) {
								vehType = this.vehicles.getVehicleTypes().get(Id.create(d.vehCombinationNo, VehicleType.class));
								if (vehType == null) {
									vehType = defaultVehType;
								}
							}
							if (vehType == null) {
								log.error("Could not find any vehicle combination for deparutre " + d.index + " used in line " + line.id.toString() + ".");
							} else {
								Vehicle veh = vb.createVehicle(Id.create("tr_" + vehId++, Vehicle.class), vehType);
								this.vehicles.addVehicle( veh);
								departure.setVehicleId(veh.getId());
								tRoute.addDeparture(departure);
							}
						}
					}
					if (tRoute.getDepartures().size() > 0) {
						tLine.addRoute(tRoute);
					} else {
						log.warn("The route " + tRoute.getId() + " was not added to the line " + tLine.getId() + " because it does not contain any departures.");
					}
				}
			}
			if (tLine.getRoutes().size() > 0) {
				this.schedule.addTransitLine(tLine);
			} else {
				log.warn("The line " + tLine.getId() + " was not added to the transit schedule because it does not contain any routes.");
			}
		}
		
		TransitLineAttributes.writeToFile(attrs, "/home/johannes/gsv/matsim/studies/netz2030/data/transitLineAttributes.net");
	}
}


