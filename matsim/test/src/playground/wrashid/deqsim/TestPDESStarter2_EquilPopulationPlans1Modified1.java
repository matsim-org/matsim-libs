package playground.wrashid.deqsim;

import java.util.ArrayList;
import java.util.LinkedList;

import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.LinkEnterEvent;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.events.PersonEvent;
import org.matsim.gbl.Gbl;
import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.DES.EventLog;
import playground.wrashid.scenarios.EquilPopulationPlans1Modified1;
import playground.wrashid.tryouts.starting.CppEventFileParser;
import playground.wrashid.util.DEQSimEventFileComparator;
import playground.wrashid.util.DEQSimEventFileTravelTimeComparator;
import playground.wrashid.util.TestHandlerDetailedEventChecker;

public class TestPDESStarter2_EquilPopulationPlans1Modified1 extends MatsimTestCase {

	// mit enable assertion flag funktionieren einige tests nicht mehr!!! => make test cases for these assertions.

	
	public void test_EquilPopulationPlans1Modified1_TestHandlerDetailedEventChecker() {
		Gbl.reset();
		
		TestHandlerDetailedEventChecker detailedChecker = new TestHandlerDetailedEventChecker();
		detailedChecker.startTestPDES2("test/scenarios/equil/config.xml", true,
				"test/scenarios/equil/plans1.xml",
				new EquilPopulationPlans1Modified1());
	}
	
	
	public void test_EquilPopulationPlans1Modified1_DEQSimEventFileComparator() {
		Gbl.reset();

		DEQSimEventFileComparator deqSimComparator = new DEQSimEventFileComparator("test/src/playground/wrashid/input/deqsim/deq_events.txt");
		deqSimComparator.startTestPDES2("test/scenarios/equil/config.xml", true,
				"test/scenarios/equil/plans1.xml",
				new EquilPopulationPlans1Modified1());
	}
	
	public void test_EquilPopulationPlans1Modified1_DEQSimEventFileTravelTimeComparator() {
		Gbl.reset();

		DEQSimEventFileTravelTimeComparator deqSimTravelTimeComparator = new DEQSimEventFileTravelTimeComparator("test/src/playground/wrashid/input/deqsim/deq_events.txt",1);
		deqSimTravelTimeComparator.startTestPDES2("test/scenarios/equil/config.xml", true,
				"test/scenarios/equil/plans1.xml",
				new EquilPopulationPlans1Modified1());
	}
	
	

	

}
