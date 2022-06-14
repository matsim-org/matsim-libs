
/* *********************************************************************** *
 * project: org.matsim.*
 * TransitScheduleValidatorTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.pt.utils;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.Collections;
import java.util.List;

public class TransitScheduleValidatorTest {

	@Test
	public void testPtTutorial() {
		Scenario scenario = ScenarioUtils.loadScenario(
				ConfigUtils.loadConfig("test/scenarios/pt-tutorial/0.config.xml"));
		TransitScheduleValidator.ValidationResult validationResult = TransitScheduleValidator.validateAll(
				scenario.getTransitSchedule(), scenario.getNetwork());
		Assertions.assertThat(validationResult.getIssues()).isEmpty();
	}

	@Test
	public void testPtTutorialWithError() {
		Scenario scenario = ScenarioUtils.loadScenario(
				ConfigUtils.loadConfig("test/scenarios/pt-tutorial/0.config.xml"));
		TransitLine transitLine = scenario.getTransitSchedule()
				.getTransitLines()
				.get(Id.create("Blue Line", TransitLine.class));
		transitLine.getRoutes()
				.get(Id.create("3to1", TransitRoute.class))
				.getRoute()
				.setLinkIds(Id.createLinkId("33"), Collections.<Id<Link>>emptyList(), Id.createLinkId("11"));
		TransitScheduleValidator.ValidationResult validationResult = TransitScheduleValidator.validateAll(
				scenario.getTransitSchedule(), scenario.getNetwork());
		System.out.println(validationResult);

		Assertions.assertThat(validationResult.getIssues()).usingRecursiveFieldByFieldElementComparator()
				.containsExactly(new TransitScheduleValidator.ValidationResult.ValidationIssue<>(
						TransitScheduleValidator.ValidationResult.Severity.ERROR,
						"Transit line Blue Line, route 3to1: Stop 2b cannot be reached along network route.",
						TransitScheduleValidator.ValidationResult.Type.ROUTE_HAS_UNREACHABLE_STOP,
						List.of(Id.create("2b", TransitStopFacility.class))));
	}

	@Test
	public void testValidator_Transfers_implausibleTime() {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		TransitSchedule schedule = scenario.getTransitSchedule();
		TransitScheduleFactory factory = schedule.getFactory();

		Id<TransitStopFacility> id1 = Id.create(1, TransitStopFacility.class);
		Id<TransitStopFacility> id2 = Id.create(2, TransitStopFacility.class);
		Id<TransitStopFacility> id3 = Id.create(3, TransitStopFacility.class);
		Id<TransitStopFacility> id4 = Id.create(4, TransitStopFacility.class);

		schedule.addStopFacility(factory.createTransitStopFacility(id1, new Coord(10000, 10000), false));
		schedule.addStopFacility(factory.createTransitStopFacility(id2, new Coord(20000, 10000), false));

		schedule.getMinimalTransferTimes().set(id1, id2, 120);
		TransitScheduleValidator.ValidationResult result = TransitScheduleValidator.validateTransfers(schedule);
		Assert.assertTrue(result.getIssues().isEmpty());

		schedule.getMinimalTransferTimes().set(id1, id2, 0);
		result = TransitScheduleValidator.validateTransfers(schedule);
		Assert.assertEquals("Should warn against implausible transfer time.", 1, result.getIssues().size());
	}

	@Test
	public void testValidator_Transfers_missingStop() {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		TransitSchedule schedule = scenario.getTransitSchedule();
		TransitScheduleFactory factory = schedule.getFactory();

		Id<TransitStopFacility> id1 = Id.create("stop1", TransitStopFacility.class);
		Id<TransitStopFacility> id2 = Id.create("stop2", TransitStopFacility.class);
		Id<TransitStopFacility> id3 = Id.create("stop3", TransitStopFacility.class);
		Id<TransitStopFacility> id4 = Id.create("stop4", TransitStopFacility.class);

		schedule.addStopFacility(factory.createTransitStopFacility(id1, new Coord(10000, 10000), false));
		schedule.addStopFacility(factory.createTransitStopFacility(id2, new Coord(20000, 10000), false));

		schedule.getMinimalTransferTimes().set(id1, id3, 120);
		TransitScheduleValidator.ValidationResult result = TransitScheduleValidator.validateTransfers(schedule);
		Assert.assertEquals("Should warn against missing stop3.", 1, result.getIssues().size());
		Assert.assertTrue("Message should contain hint about stop3 being missing. " + result.getIssues().get(0).getMessage(), result.getIssues().get(0).getMessage().contains("stop3"));
		schedule.getMinimalTransferTimes().remove(id1, id3);

		schedule.getMinimalTransferTimes().set(id4, id2, 120);
		result = TransitScheduleValidator.validateTransfers(schedule);
		Assert.assertEquals("Should warn against missing stop4.", 1, result.getIssues().size());
		Assert.assertTrue("Message should contain hint about stop4 being missing. " + result.getIssues().get(0).getMessage(), result.getIssues().get(0).getMessage().contains("stop4"));
	}

}
