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
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.testcases.MatsimTestCase;

/**
 * Created by IntelliJ IDEA. User: zilske Date: 10/31/11 Time: 11:46 AM To
 * change this template use File | Settings | File Templates.
 */
public class CarrierPlanReaderV1Test extends MatsimTestCase {

	@Test
	public void testCarrierPlanReaderDoesSomething() {
		Carriers carriers = new Carriers();
		CarrierPlanReaderV1 carrierPlanReaderV1 = new CarrierPlanReaderV1(carriers);
		carrierPlanReaderV1.readFile(getClassInputDirectory() + "carrierPlansEquils.xml");
		assertEquals(1, carriers.getCarriers().size());
	}

	@Test
	public void testReaderReadsCorrectly() {
		Carriers carriers = new Carriers();
		CarrierPlanReaderV1 carrierPlanReaderV1 = new CarrierPlanReaderV1(carriers);
		carrierPlanReaderV1.readFile(getClassInputDirectory() + "carrierPlansEquils.xml");
		assertEquals(1, carriers.getCarriers().size());
		Carrier carrier = carriers.getCarriers().values().iterator().next();
		assertEquals(1, carrier.getSelectedPlan().getScheduledTours().size());
		Leg leg = (Leg) carrier.getSelectedPlan().getScheduledTours()
				.iterator().next().getTour().getTourElements().get(0);
		NetworkRoute route = (NetworkRoute) leg.getRoute();
		assertEquals(3, route.getLinkIds().size());
		assertEquals("23", route.getStartLinkId().toString());
		assertEquals("2", route.getLinkIds().get(0).toString());
		assertEquals("3", route.getLinkIds().get(1).toString());
		assertEquals("4", route.getLinkIds().get(2).toString());
		assertEquals("15", route.getEndLinkId().toString());
	}
	
	@Test
	public void testReaderReadsScoreAndSelectedPlanCorrectly() {
		Carriers carriers = new Carriers();
		CarrierPlanReaderV1 carrierPlanReaderV1 = new CarrierPlanReaderV1(carriers);
		carrierPlanReaderV1.readFile(getClassInputDirectory() + "carrierPlansEquils.xml");
		Carrier carrier = carriers.getCarriers().values().iterator().next();
		assertNotNull(carrier.getSelectedPlan());
		assertEquals(-100.0, carrier.getSelectedPlan().getScore());
		assertEquals(2,carrier.getPlans().size());
	}
	
	@Test
	public void testReaderReadsUnScoredAndUnselectedPlanCorrectly() {
		Carriers carriers = new Carriers();
		CarrierPlanReaderV1 carrierPlanReaderV1 = new CarrierPlanReaderV1(carriers);
		carrierPlanReaderV1.readFile(getClassInputDirectory() + "carrierPlansEquils_unscored_unselected.xml");
		Carrier carrier = carriers.getCarriers().values().iterator().next();
		assertNull(carrier.getSelectedPlan());
		assertEquals(2,carrier.getPlans().size());
	}

}