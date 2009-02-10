package org.matsim.mobsim.deqsim;

import org.matsim.gbl.Gbl;
import org.matsim.mobsim.deqsim.scenarios.EquilPopulationPlans1Modified1;
import org.matsim.mobsim.deqsim.util.DEQSimEventFileComparator;
import org.matsim.mobsim.deqsim.util.DEQSimEventFileTravelTimeComparator;
import org.matsim.mobsim.deqsim.util.TestHandlerDetailedEventChecker;

import org.matsim.testcases.MatsimTestCase;


public class TestDESStarter_EquilPopulationPlans1Modified1 extends MatsimTestCase {

	// mit enable assertion flag funktionieren einige tests nicht mehr!!! =>
	// make test cases for these assertions.

	public void test_EquilPopulationPlans1Modified1_TestHandlerDetailedEventChecker() {
		Gbl.reset();

		TestHandlerDetailedEventChecker detailedChecker = new TestHandlerDetailedEventChecker();
		detailedChecker.startTestDES("test/scenarios/equil/config.xml", false,
				"test/scenarios/equil/plans1.xml", new EquilPopulationPlans1Modified1());
	}

	public void test_EquilPopulationPlans1Modified1_DEQSimEventFileComparator() {
		Gbl.reset();

		DEQSimEventFileComparator deqSimComparator = new DEQSimEventFileComparator(
				"test/input/org/matsim/mobsim/deqsim/deq_events.txt");
		deqSimComparator.startTestDES("test/scenarios/equil/config.xml", false,
				"test/scenarios/equil/plans1.xml", new EquilPopulationPlans1Modified1());
	}

	public void test_EquilPopulationPlans1Modified1_DEQSimEventFileTravelTimeComparator() {
		Gbl.reset();

		DEQSimEventFileTravelTimeComparator deqSimTravelTimeComparator = new DEQSimEventFileTravelTimeComparator(
				"test/input/org/matsim/mobsim/deqsim/deq_events.txt", 1);
		deqSimTravelTimeComparator.startTestDES("test/scenarios/equil/config.xml", false,
				"test/scenarios/equil/plans1.xml", new EquilPopulationPlans1Modified1());
	}

}
