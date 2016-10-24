package org.matsim.pt.utils;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.Collections;

import static java.util.Collections.emptyList;
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


}
