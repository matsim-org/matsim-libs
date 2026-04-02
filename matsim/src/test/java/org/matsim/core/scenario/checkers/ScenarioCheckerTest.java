package org.matsim.core.scenario.checkers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.ArrayList;
import java.util.List;

class ScenarioCheckerTest {

	@Test
	void startsWithoutScenarioConsistencyCheckerByDefault() {
		MutableScenario scenario = createScenario();

		Assertions.assertTrue(scenario.getScenarioCheckers().isEmpty());
	}

	@Test
	void doesNotAddDuplicateCheckerTypesViaScenario() {
		MutableScenario scenario = createScenario();

		scenario.addScenarioChecker(new ActivityCheckerSubclass());
		scenario.addScenarioChecker(new ActivityCheckerSubclass());

		long checkerCount = scenario.getScenarioCheckers().stream()
			.filter(checker -> checker.getClass().equals(ActivityCheckerSubclass.class))
			.count();

		Assertions.assertEquals(1, checkerCount);
	}

	@Test
	void removesScenarioCheckersByTypeViaScenario() {
		MutableScenario scenario = createScenario();
		VspScenarioCheckerImpl checker = new VspScenarioCheckerImpl();
		scenario.addScenarioChecker(checker);

		scenario.removeScenarioChecker(checker);

		Assertions.assertTrue(scenario.getScenarioCheckers().stream()
			.noneMatch(checker1 -> checker1.equals(checker)));
	}

	@Test
	void executesRegisteredCheckersViaScenario() {
		MutableScenario scenario = createScenario();
		List<String> executionOrder = new ArrayList<>();

		scenario.addScenarioChecker(new FirstChecker(executionOrder));
		scenario.addScenarioChecker(new SecondChecker(executionOrder));

		scenario.checkConsistencyBeforeRun();

		Assertions.assertEquals(List.of("first", "second"), executionOrder);
	}

	private static MutableScenario createScenario() {
		return ScenarioUtils.createMutableScenario(ConfigUtils.createConfig());
	}

	private static class ActivityCheckerSubclass implements ScenarioChecker {
		@Override
		public void checkConsistencyBeforeRun(Scenario scenario) {
		}

		@Override
		public void checkConsistencyAfterRun(Scenario scenario) {

		}
	}

	private record FirstChecker(List<String> executionOrder) implements ScenarioChecker {
		@Override
		public void checkConsistencyBeforeRun(Scenario scenario) {
			executionOrder.add("first");
		}

		@Override
		public void checkConsistencyAfterRun(Scenario scenario) {

		}
	}

	private record SecondChecker(List<String> executionOrder) implements ScenarioChecker {

		@Override
		public void checkConsistencyBeforeRun(Scenario scenario) {
			executionOrder.add("second");
		}

		@Override
		public void checkConsistencyAfterRun(Scenario scenario) {

		}
	}
}
