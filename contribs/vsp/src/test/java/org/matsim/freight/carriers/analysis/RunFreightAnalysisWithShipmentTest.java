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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.freight.carriers.CarrierPlanXmlReader;
import org.matsim.freight.carriers.CarrierShipment;
import org.matsim.freight.carriers.CarrierVehicleTypes;
import org.matsim.freight.carriers.Carriers;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.*;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedHashMap;

public class RunFreightAnalysisWithShipmentTest {

    @RegisterExtension
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	@Test
	void runShipmentTrackerTest(){
        final String inputPath = testUtils.getClassInputDirectory();
        File networkFile = new File(inputPath + "/shipment/output_network.xml.gz");
        File carrierFile = new File(inputPath + "/shipment/output_carriers.xml");
        File vehiclesFile = new File(inputPath + "/shipment/output_allVehicles.xml.gz");
        File eventsFile = new File(inputPath + "/shipment/output_events.xml.gz");

        Network network = NetworkUtils.readNetwork(networkFile.getAbsolutePath());

        Vehicles vehicles = VehicleUtils.createVehiclesContainer();
        new MatsimVehicleReader(vehicles).readFile(vehiclesFile.getAbsolutePath());

        CarrierVehicleTypes carrierVehicleTypes = new CarrierVehicleTypes();
        for( VehicleType vehicleType : vehicles.getVehicleTypes().values() ){
            carrierVehicleTypes.getVehicleTypes().put( vehicleType.getId(), vehicleType );
        }
        // yyyy the above is somewhat awkward.  ???

        Carriers carriers = new Carriers();
        new CarrierPlanXmlReader(carriers, carrierVehicleTypes).readFile(carrierFile.getAbsolutePath());

        EventsManager eventsManager = EventsUtils.createEventsManager();
        MyShipmentTrackerEventHandler eventHandler = new MyShipmentTrackerEventHandler(vehicles, network, carriers);
        eventsManager.addHandler(eventHandler);
        eventsManager.initProcessing();
        MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);

        eventsReader.readFile(eventsFile.getAbsolutePath());
        eventsManager.finishProcessing();

        LinkedHashMap<Id<CarrierShipment>, ShipmentTracker> shipments = eventHandler.getShipmentTracker().getShipments();

        Iterator<Id<CarrierShipment>> shipmentKeys = shipments.keySet().iterator();
        LinkedHashMap<Id<CarrierShipment>, Double> beelineDistance = new LinkedHashMap<>();

        while(shipmentKeys.hasNext()){
            Id<CarrierShipment> shipmentId = shipmentKeys.next();
            Id<Link> fromLinkId = shipments.get(shipmentId).from;
            Id<Link> toLinkId = shipments.get(shipmentId).to;

            double dist = NetworkUtils.getEuclideanDistance(network.getLinks().get(fromLinkId).getCoord(), network.getLinks().get(toLinkId).getCoord());
            beelineDistance.put(shipmentId, dist);
            System.out.println("from "+fromLinkId+" to "+toLinkId+" distance is "+dist);
        }

        Assertions.assertEquals(Double.valueOf(10816.653826391968),beelineDistance.get(Id.create( "1", CarrierShipment.class )),"Beeline distance is not as expected for shipment 1");
        Assertions.assertEquals(Double.valueOf(6000.0),beelineDistance.get(Id.create( "11", CarrierShipment.class )),"Beeline distance is not as expected for shipment 11");
        Assertions.assertEquals(Double.valueOf(7106.335201775948),beelineDistance.get(Id.create( "12", CarrierShipment.class )),"Beeline distance is not as expected for shipment 12");
        Assertions.assertEquals(Double.valueOf(4123.105625617661),beelineDistance.get(Id.create( "13", CarrierShipment.class )),"Beeline distance is not as expected for shipment 13");
        Assertions.assertEquals(Double.valueOf(9055.385138137417),beelineDistance.get(Id.create( "14", CarrierShipment.class )),"Beeline distance is not as expected for shipment 14");
        Assertions.assertEquals(Double.valueOf(6964.19413859206),beelineDistance.get(Id.create( "15", CarrierShipment.class )),"Beeline distance is not as expected for shipment 15");
        Assertions.assertEquals(Double.valueOf(9192.388155425118),beelineDistance.get(Id.create( "16", CarrierShipment.class )),"Beeline distance is not as expected for shipment 16");
    }
}
