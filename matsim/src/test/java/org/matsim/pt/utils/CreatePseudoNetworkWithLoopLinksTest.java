package org.matsim.pt.utils;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import static org.assertj.core.api.Assertions.assertThat;

class CreatePseudoNetworkWithLoopLinksTest {

	@Test
	void createValidSchedule() {

		Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig("test/scenarios/pt-tutorial/0.config.xml"));

		CreatePseudoNetworkWithLoopLinks creator = new CreatePseudoNetworkWithLoopLinks(scenario.getTransitSchedule(), scenario.getNetwork(), "pt_");
		creator.createNetwork();


		TransitScheduleValidator.ValidationResult result = TransitScheduleValidator.validateAll(scenario.getTransitSchedule(), scenario.getNetwork());

		assertThat(result.getWarnings()).isEmpty();
		assertThat(result.getErrors()).isEmpty();
		assertThat(result.isValid()).isTrue();

	}
}
