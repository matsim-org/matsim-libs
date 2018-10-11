package org.matsim.pt.utils;

import org.hamcrest.Matchers;
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

import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;

public class TransitScheduleValidatorTest {

	@Test
	public void testPtTutorial() {
		Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig("test/scenarios/pt-tutorial/0.config.xml"));
		TransitScheduleValidator.ValidationResult validationResult = TransitScheduleValidator.validateAll(scenario.getTransitSchedule(), scenario.getNetwork());
		Assert.assertThat(validationResult.getIssues(), empty());
	}

	@Test
	public void testPtTutorialWithError() {
		Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig("test/scenarios/pt-tutorial/0.config.xml"));
		TransitLine transitLine = scenario.getTransitSchedule().getTransitLines().get(Id.create("Blue Line", TransitLine.class));
		transitLine.getRoutes().get(Id.create("3to1", TransitRoute.class)).getRoute().setLinkIds(Id.createLinkId("33"), Collections.<Id<Link>>emptyList(), Id.createLinkId("11"));
		TransitScheduleValidator.ValidationResult validationResult = TransitScheduleValidator.validateAll(scenario.getTransitSchedule(), scenario.getNetwork());

		Assert.assertThat(validationResult.getIssues(), containsInAnyOrder(Matchers.allOf(
				hasProperty("severity", Matchers.equalTo(TransitScheduleValidator.ValidationResult.Severity.ERROR)),
				hasProperty("entities", containsInAnyOrder(Matchers.equalTo(Id.create("2b", TransitStopFacility.class)))))));
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
