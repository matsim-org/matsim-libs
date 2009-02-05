package playground.wrashid.deqsim;

import java.util.ArrayList;
import java.util.LinkedList;

import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.LinkEnterEvent;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.events.PersonEvent;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.deqsim.EventLog;
import org.matsim.mobsim.deqsim.util.CppEventFileParser;
import org.matsim.mobsim.deqsim.util.DEQSimEventFileTravelTimeComparator;
import org.matsim.mobsim.deqsim.util.TestHandlerDetailedEventChecker;
import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.scenarios.EquilPopulationPlans1Modified1;
import playground.wrashid.util.DEQSimEventFileComparator;

public class TestPDESStarter2_EquilPopulationPlans1Modified1 extends MatsimTestCase {

	// mit enable assertion flag funktionieren einige tests nicht mehr!!! => make test cases for these assertions.

	
	public void test_EquilPopulationPlans1Modified1_TestHandlerDetailedEventChecker() {
		// TODO: Test "frozen", the parallel version development has been stopped. 
		/*
		Gbl.reset();
		
		TestHandlerDetailedEventChecker detailedChecker = new TestHandlerDetailedEventChecker();
		detailedChecker.startTestPDES2("test/scenarios/equil/config.xml", false,
				"test/scenarios/equil/plans1.xml",
				new EquilPopulationPlans1Modified1());
				*/
	}
	
	
	public void test_EquilPopulationPlans1Modified1_DEQSimEventFileComparator() {
		// TODO: Test "frozen", the parallel version development has been stopped. 
		/*
		Gbl.reset();

		DEQSimEventFileComparator deqSimComparator = new DEQSimEventFileComparator("test/src/playground/wrashid/input/deqsim/deq_events.txt");
		deqSimComparator.startTestPDES2("test/scenarios/equil/config.xml", false,
				"test/scenarios/equil/plans1.xml",
				new EquilPopulationPlans1Modified1());
				*/
	}
	
	public void test_EquilPopulationPlans1Modified1_DEQSimEventFileTravelTimeComparator() {
		// TODO: Test "frozen", the parallel version development has been stopped. 
		/*
		Gbl.reset();

		DEQSimEventFileTravelTimeComparator deqSimTravelTimeComparator = new DEQSimEventFileTravelTimeComparator("test/src/playground/wrashid/input/deqsim/deq_events.txt",1);
		deqSimTravelTimeComparator.startTestPDES2("test/scenarios/equil/config.xml", false,
				"test/scenarios/equil/plans1.xml",
				new EquilPopulationPlans1Modified1());
				*/
	}
	
	

	

}
