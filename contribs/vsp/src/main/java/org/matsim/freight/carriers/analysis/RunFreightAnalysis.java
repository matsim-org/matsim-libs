/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.freight.carriers.analysis;

import org.matsim.api.core.v01.network.Network;
import org.matsim.freight.carriers.CarrierPlanXmlReader;
import org.matsim.freight.carriers.CarrierVehicleTypes;
import org.matsim.freight.carriers.Carriers;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.vehicles.MatsimVehicleReader;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import java.io.File;

/**
 *   @deprecated We have new event types now, allowing us to use a more straight forward analysis without guessing.
 *  I will let this here for some time so we can have a look, what else should be moved over, but in the end, We will remove this here.
 *  (kmt apr'23)
 *
 * @author Jakob Harnisch (MATSim advanced class 2020/21)
 */

@Deprecated(since = "apr '23", forRemoval = true)
public class RunFreightAnalysis {

	private final String inputPath;
	private final String outputPath;

	public RunFreightAnalysis(String inputPath, String outputPath) {
		this.inputPath = inputPath;
		this.outputPath = outputPath;
	}

	public void runAnalysis(){
	   File networkFile = new File(inputPath + "/output_network.xml.gz");
	   File carrierFile = new File(inputPath + "/output_carriers.xml");
	   File vehiclesFile = new File(inputPath + "/output_allVehicles.xml.gz");
	   File eventsFile = new File(inputPath + "/output_events.xml.gz");

	   Network network = NetworkUtils.readNetwork(networkFile.getAbsolutePath());

		Vehicles vehicles = VehicleUtils.createVehiclesContainer();
		new  MatsimVehicleReader(vehicles).readFile(vehiclesFile.getAbsolutePath());

		CarrierVehicleTypes carrierVehicleTypes = new CarrierVehicleTypes();
		for( VehicleType vehicleType : vehicles.getVehicleTypes().values() ){
			carrierVehicleTypes.getVehicleTypes().put( vehicleType.getId(), vehicleType );
		}
		// yyyy the above is somewhat awkward.  ???

		Carriers carriers = new Carriers();
		new CarrierPlanXmlReader(carriers, carrierVehicleTypes ).readFile(carrierFile.getAbsolutePath() );

	   EventsManager eventsManager = EventsUtils.createEventsManager();
	   FreightAnalysisEventHandler freightEventHandler = new FreightAnalysisEventHandler(network, vehicles,  carriers);
	   eventsManager.addHandler(freightEventHandler);

	   eventsManager.initProcessing();
	   MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);

	   eventsReader.readFile(eventsFile.getAbsolutePath());
	   eventsManager.finishProcessing();

	   freightEventHandler.exportVehicleInfo(outputPath, true);
	   freightEventHandler.exportVehicleTripInfo(outputPath, true);
	   freightEventHandler.exportVehicleTypeStats(outputPath, true);
	   freightEventHandler.exportServiceInfo(outputPath, true);
	   freightEventHandler.exportShipmentInfo(outputPath, true);
	}
}
