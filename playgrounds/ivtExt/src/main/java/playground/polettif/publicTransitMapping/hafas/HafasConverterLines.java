/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
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
 * *********************************************************************** *
 */

package playground.polettif.publicTransitMapping.hafas;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.misc.Counter;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;
import playground.polettif.publicTransitMapping.hafas.lib.BitfeldAnalyzer;
import playground.polettif.publicTransitMapping.hafas.lib.HAFASUtils;
import playground.polettif.publicTransitMapping.hafas.lib.OperatorReader;
import playground.polettif.publicTransitMapping.hafas.lib.StopReader;
import playground.polettif.publicTransitMapping.hafas.lib2.FPLANReader2;
import playground.polettif.publicTransitMapping.hafas.lib2.FPLANRoute;
import playground.polettif.publicTransitMapping.tools.ScheduleCleaner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of Hafas2MATSimTransitSchedule.
 *
 * @author boescpa
 */
public class HafasConverterLines extends Hafas2TransitSchedule {

	private TransitScheduleFactory scheduleFactory;
	private VehiclesFactory vehicleFactory;

	public HafasConverterLines(TransitSchedule schedule, Vehicles vehicles, CoordinateTransformation transformation) {
		super(schedule, vehicles, transformation);
		this.scheduleFactory = schedule.getFactory();
		this.vehicleFactory = vehicles.getFactory();
	}

	@Override
	public void createSchedule(String pathToInputFiles) {
		log.info("Creating the schedule based on HAFAS...");

		// 1. Read and create stop facilities
		log.info("  Read transit stops...");
		StopReader.run(schedule, transformation, pathToInputFiles + "BFKOORD_GEO");
		log.info("  Read transit stops... done.");

		// 2. Read all operators from BETRIEB_DE
		log.info("  Read operators...");
		Map<String, String> operators = OperatorReader.readOperators(pathToInputFiles + "BETRIEB_DE");
		log.info("  Read operators... done.");

		// 3. Read all ids for work-day-routes from HAFAS-BITFELD
		log.info("  Read bitfeld numbers...");
		Set<Integer> bitfeldNummern = BitfeldAnalyzer.findBitfeldnumbersOfBusiestDay(pathToInputFiles + "FPLAN", pathToInputFiles + "BITFELD");
		log.info("  Read bitfeld numbers... done.");

		// 4. Create all lines from HAFAS-Schedule
		log.info("  Read transit lines...");
		List<FPLANRoute> routes = FPLANReader2.parseFPLAN(bitfeldNummern, operators, pathToInputFiles + "FPLAN");
		log.info("  Read transit lines... done.");

		log.info("  Creating Transit Routes...");
		createTransitRoutesFromFPLAN(routes);
		log.info("  Creating Transit Routes... done.");

		// 5. Clean schedule
		HAFASUtils.removeNonUsedStopFacilities(schedule);
		ScheduleCleaner.combineIdenticalTransitRoutes(schedule);
		ScheduleCleaner.cleanDepartures(schedule);
		HAFASUtils.cleanVehicles(schedule, vehicles);

		log.info("Creating the schedule based on HAFAS... done.");
	}

	private void createTransitRoutesFromFPLAN(List<FPLANRoute> routes) {
		Map<Id<TransitLine>, Integer> routeNrs = new HashMap<>();

		Counter lineCounter = new Counter(" TransitLine # ");

		// set schedule so fplanRoutes have stopfacilities available
		FPLANRoute.setSchedule(schedule);

		for(FPLANRoute fplanRoute : routes) {
			Id<TransitLine> lineId = createLineId(fplanRoute);

			// create or get TransitLine
			TransitLine transitLine;
			if(!schedule.getTransitLines().containsKey(lineId)) {
				transitLine = scheduleFactory.createTransitLine(lineId);
				schedule.addTransitLine(transitLine);
				lineCounter.incCounter();
			} else {
				transitLine = schedule.getTransitLines().get(lineId);
			}

			// create vehicle type if needed
			Id<VehicleType> vehicleTypeId = fplanRoute.getVehicleTypeId();
			VehicleType vehicleType = vehicles.getVehicleTypes().get(vehicleTypeId);
			if(vehicleType == null) {
				String typeIdstr = vehicleTypeId.toString();

				vehicleType = vehicleFactory.createVehicleType(Id.create(vehicleTypeId.toString(), VehicleType.class));

				// using default values for vehicle type
				vehicleType.setLength(HafasDefinitions.Vehicles.valueOf(typeIdstr).length);
				vehicleType.setWidth(HafasDefinitions.Vehicles.valueOf(typeIdstr).width);
				vehicleType.setAccessTime(HafasDefinitions.Vehicles.valueOf(typeIdstr).accessTime);
				vehicleType.setEgressTime(HafasDefinitions.Vehicles.valueOf(typeIdstr).egressTime);
				vehicleType.setDoorOperationMode(HafasDefinitions.Vehicles.valueOf(typeIdstr).doorOperation);
				vehicleType.setPcuEquivalents(HafasDefinitions.Vehicles.valueOf(typeIdstr).pcuEquivalents);

				VehicleCapacity vehicleCapacity = vehicleFactory.createVehicleCapacity();
				vehicleCapacity.setSeats(HafasDefinitions.Vehicles.valueOf(typeIdstr).capacitySeats);
				vehicleCapacity.setStandingRoom(HafasDefinitions.Vehicles.valueOf(typeIdstr).capacityStanding);
				vehicleType.setCapacity(vehicleCapacity);

				vehicles.addVehicleType(vehicleType);
			}

			// create actual TransitRoute
			int routeNr = MapUtils.getInteger(lineId, routeNrs, 0);
			Id<TransitRoute> routeId = createRouteId(fplanRoute, ++routeNr);
			routeNrs.put(lineId, routeNr);

			TransitRoute transitRoute = scheduleFactory.createTransitRoute(routeId, null, fplanRoute.getTransitRouteStops(), fplanRoute.getMode());
				for(Departure departure : fplanRoute.getDepartures()) {
					transitRoute.addDeparture(departure);
					try {
						vehicles.addVehicle(vehicleFactory.createVehicle(departure.getVehicleId(), vehicleType));
					} catch (Exception e) {
						e.printStackTrace();
						fplanRoute.getDepartures();
					}
				}
			transitLine.addRoute(transitRoute);
		}
	}

	private Id<TransitLine> createLineId(FPLANRoute route) {
		if(route.getRouteDescription() == null) {
			return Id.create(route.getOperator(), TransitLine.class);
		} else {
			return Id.create(route.getOperator() + "_" + route.getRouteDescription(), TransitLine.class);
		}
	}

	private Id<TransitRoute> createRouteId(FPLANRoute route, int routeNr) {
		return Id.create(route.getFahrtNummer() + "_" + String.format("%03d", routeNr), TransitRoute.class);
//		return Id.create(route.getFahrtNummer(), TransitRoute.class);
	}
}
