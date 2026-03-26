package org.matsim.core.scenario;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.consistency.ScenarioConsistencyChecker;
import org.matsim.core.scenario.consistency.VspScenarioConsistencyCheckerImpl;

import java.util.ArrayList;
import java.util.List;

class ScenarioConsistencyTest {

	@Test
	void startsWithoutScenarioConsistencyCheckerByDefault() {
		MutableScenario scenario = createScenario();

		Assertions.assertTrue(scenario.getScenarioConsistencyCheckers().isEmpty());
	}

	@Test
	void doesNotAddDuplicateCheckerTypesViaScenario() {
		MutableScenario scenario = createScenario();

		scenario.addScenarioConsistencyChecker(new ActivityCheckerSubclass());
		scenario.addScenarioConsistencyChecker(new ActivityCheckerSubclass());

		long checkerCount = scenario.getScenarioConsistencyCheckers().stream()
			.filter(checker -> checker.getClass().equals(ActivityCheckerSubclass.class))
			.count();

		Assertions.assertEquals(1, checkerCount);
	}

	@Test
	void removesScenarioCheckersByTypeViaScenario() {
		MutableScenario scenario = createScenario();
		scenario.addScenarioConsistencyChecker(new VspScenarioConsistencyCheckerImpl());

		scenario.removeScenarioConsistencyChecker(VspScenarioConsistencyCheckerImpl.class);

		Assertions.assertTrue(scenario.getScenarioConsistencyCheckers().stream()
			.noneMatch(checker -> checker.getClass().equals(VspScenarioConsistencyCheckerImpl.class)));
	}

	@Test
	void executesRegisteredCheckersViaScenario() {
		MutableScenario scenario = createScenario();
		List<String> executionOrder = new ArrayList<>();

		scenario.addScenarioConsistencyChecker(new FirstChecker(executionOrder));
		scenario.addScenarioConsistencyChecker(new SecondChecker(executionOrder));

		scenario.checkConsistencyBeforeRun();

		Assertions.assertEquals(List.of("first", "second"), executionOrder);
	}

	private static MutableScenario createScenario() {
		return ScenarioUtils.createMutableScenario(ConfigUtils.createConfig());
	}

	private static class ActivityCheckerSubclass implements ScenarioConsistencyChecker {
		@Override
		public void checkConsistencyBeforeRun(Scenario scenario) {
		}

		@Override
		public void checkConsistencyAfterRun(Scenario scenario) {

		}
	}

	private record FirstChecker(List<String> executionOrder) implements ScenarioConsistencyChecker {
		@Override
		public void checkConsistencyBeforeRun(Scenario scenario) {
			executionOrder.add("first");
		}

		@Override
		public void checkConsistencyAfterRun(Scenario scenario) {

		}
	}

	private record SecondChecker(List<String> executionOrder) implements ScenarioConsistencyChecker {

		@Override
		public void checkConsistencyBeforeRun(Scenario scenario) {
			executionOrder.add("second");
		}

		@Override
		public void checkConsistencyAfterRun(Scenario scenario) {

		}
	}
}
