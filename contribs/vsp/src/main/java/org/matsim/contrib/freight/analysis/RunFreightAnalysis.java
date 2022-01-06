/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C) 2021 by the members listed in the COPYING,        *
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

package org.matsim.contrib.freight.analysis;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlReader;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.vehicles.MatsimVehicleReader;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import java.io.File;

/**
 * @author Jakob Harnisch (MATSim advanced class 2020/21)
 */

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

	   Carriers carriers = new Carriers();
	   new CarrierPlanXmlReader(carriers).readFile(carrierFile.getAbsolutePath());

	   Vehicles vehicles = VehicleUtils.createVehiclesContainer();
	   new  MatsimVehicleReader(vehicles).readFile(vehiclesFile.getAbsolutePath());

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
