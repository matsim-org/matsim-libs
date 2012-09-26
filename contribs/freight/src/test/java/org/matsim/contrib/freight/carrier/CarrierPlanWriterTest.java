/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * ${file_name}
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) ${year} by the members listed in the COPYING,        *
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
 *
 * ${filecomment}
 * ${package_declaration}
 *
 * ${typecomment}
 * ${type_declaration}
 */

package org.matsim.contrib.freight.carrier;

import org.junit.Test;
import org.matsim.contrib.freight.carrier.Tour.Leg;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.testcases.MatsimTestCase;

/**
 */
public class CarrierPlanWriterTest extends MatsimTestCase {

	@Test
	public void testCarrierPlanWriterWrites() {
		Carriers carriers = new Carriers();
		CarrierPlanReader carrierPlanReader = new CarrierPlanReader(carriers);
		carrierPlanReader.read(getInputDirectory() + "carrierPlansEquils.xml");
		CarrierPlanWriter planWriter = new CarrierPlanWriter(carriers
				.getCarriers().values());
		try {
			planWriter.write(getInputDirectory()
					+ "carrierPlansEquilsWritten.xml");
			assertTrue(true);
		} catch (Exception e) {
			assertFalse(true);
		}
	}
	
	@Test
	public void testCarrierPlanWriterWritesAndReadsTheWrittenPlanCorrectly() {
		Carriers carriers = new Carriers();
		{
			CarrierPlanReader carrierPlanReader = new CarrierPlanReader(carriers);
			carrierPlanReader.read(getInputDirectory() + "carrierPlansEquils.xml");
			CarrierPlanWriter planWriter = new CarrierPlanWriter(carriers
					.getCarriers().values());
			try {
				planWriter.write(getInputDirectory()
						+ "carrierPlansEquilsWritten.xml");
				assertTrue(true);
			} catch (Exception e) {
				assertFalse(true);
			}
		}
		carriers = new Carriers();
		CarrierPlanReader carrierPlanReader = new CarrierPlanReader(carriers);
		carrierPlanReader.read(getInputDirectory() + "carrierPlansEquilsWritten.xml");
		assertEquals(1, carriers.getCarriers().size());
		Carrier carrier = carriers.getCarriers().values().iterator().next();
		assertEquals(1, carrier.getSelectedPlan().getScheduledTours().size());
		Leg leg = (Leg) carrier.getSelectedPlan().getScheduledTours()
				.iterator().next().getTour().getTourElements().get(0);
		LinkNetworkRouteImpl route = (LinkNetworkRouteImpl) leg.getRoute();
		assertEquals(3, route.getLinkIds().size());
		assertEquals("23", route.getStartLinkId().toString());
		assertEquals("2", route.getLinkIds().get(0).toString());
		assertEquals("3", route.getLinkIds().get(1).toString());
		assertEquals("4", route.getLinkIds().get(2).toString());
		assertEquals("15", route.getEndLinkId().toString());
		
		assertNotNull(carrier.getSelectedPlan());
		assertEquals(-100.0, carrier.getSelectedPlan().getScore());
		assertEquals(2,carrier.getPlans().size());
	}
}
