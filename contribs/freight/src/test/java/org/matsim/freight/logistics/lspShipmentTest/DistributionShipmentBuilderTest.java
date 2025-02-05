/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2022 by the members listed in the COPYING,        *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.freight.logistics.lspShipmentTest;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.freight.carriers.TimeWindow;
import org.matsim.freight.logistics.shipment.LspShipment;
import org.matsim.freight.logistics.shipment.LspShipmentUtils;

public class DistributionShipmentBuilderTest {

	private Network network;
	private ArrayList<LspShipment> shipments;


	@BeforeEach
	public void initialize() {
		Config config = new Config();
		config.addCoreModules();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(ExamplesUtils.getTestScenarioURL("logistics-2regions") + "2regions-network.xml");
		this.network = scenario.getNetwork();
		ArrayList<Link> linkList = new ArrayList<>(network.getLinks().values());
		Id<Link> distributionLinkId = Id.createLinkId("(14 2) (14 3)");
		Id<Link> fromLinkId = network.getLinks().get(distributionLinkId).getId();
		this.shipments = new ArrayList<>();

		for (int i = 1; i < 11; i++) {
			Id<LspShipment> id = Id.create(i, LspShipment.class);
			LspShipmentUtils.LspShipmentBuilder builder = LspShipmentUtils.LspShipmentBuilder.newInstance(id);
			int capacityDemand = MatsimRandom.getRandom().nextInt(10);
			builder.setCapacityDemand(capacityDemand);

			while (true) {
				Collections.shuffle(linkList);
				Link pendingToLink = linkList.getFirst();
				if ((pendingToLink.getFromNode().getCoord().getX() <= 18000 &&
						pendingToLink.getFromNode().getCoord().getY() <= 4000 &&
						pendingToLink.getFromNode().getCoord().getX() >= 14000 &&
						pendingToLink.getToNode().getCoord().getX() <= 18000 &&
						pendingToLink.getToNode().getCoord().getY() <= 4000 &&
						pendingToLink.getToNode().getCoord().getX() >= 14000)) {
					builder.setToLinkId(pendingToLink.getId());
					break;
				}

			}

			builder.setFromLinkId(fromLinkId);
			TimeWindow endTimeWindow = TimeWindow.newInstance(0, (24 * 3600));
			builder.setEndTimeWindow(endTimeWindow);
			TimeWindow startTimeWindow = TimeWindow.newInstance(0, (24 * 3600));
			builder.setStartTimeWindow(startTimeWindow);
			builder.setDeliveryServiceTime(capacityDemand * 60);
			shipments.add(builder.build());
		}
	}

	@Test
	public void testShipments() {
		assertEquals(10, shipments.size());
		for (LspShipment shipment : shipments) {
			assertNotNull(shipment.getId());
			assertNotNull(shipment.getSize());
			assertNotNull(shipment.getDeliveryTimeWindow());
			assertNotNull(shipment.getFrom());
			assertNotNull(shipment.getDeliveryServiceTime());
			assertNotNull(shipment.getTo());
			assertNotNull(shipment.getPickupTimeWindow());
//			assertNotNull(shipment.getShipmentPlan());
			assertNotNull(shipment.getShipmentLog());
			assertNotNull(shipment.getSimulationTrackers());

			assertTrue(shipment.getSimulationTrackers().isEmpty());
			assertEquals(shipment.getShipmentLog().getLspShipmentId(), shipment.getId());
			assertTrue(shipment.getShipmentLog().getPlanElements().isEmpty());

//			assertEquals(shipment.getShipmentPlan().getEmbeddingContainer(), shipment.getId());
//			assertTrue(shipment.getShipmentPlan().getPlanElements().isEmpty());
			Link link = network.getLinks().get(shipment.getTo());
			assertTrue(link.getFromNode().getCoord().getX() <= 18000);
			assertTrue(link.getFromNode().getCoord().getX() >= 14000);
			assertTrue(link.getToNode().getCoord().getX() <= 18000);
			assertTrue(link.getToNode().getCoord().getX() >= 14000);
		}
	}
}
