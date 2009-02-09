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
import org.matsim.mobsim.deqsim.util.DEQSimEventFileComparator;
import org.matsim.mobsim.deqsim.util.DEQSimEventFileTravelTimeComparator;
import org.matsim.mobsim.deqsim.util.TestHandlerDetailedEventChecker;
import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.scenarios.EquilPopulationPlans1Modified1;

public class TestPDESStarter2_equilPlans100 extends MatsimTestCase {
	
	public void test_equilPlans100_TestHandlerDetailedEventChecker() {
		// TODO: Test "frozen", the parallel version development has been stopped. 
		/*
		Gbl.reset();
		
		TestHandlerDetailedEventChecker detailedChecker = new TestHandlerDetailedEventChecker();
		detailedChecker.startTestPDES2("test/scenarios/equil/config.xml", false,
				"test/scenarios/equil/plans100.xml",
				null);
				*/
	}
	
	/* 
	 * This test is turned off, because it cannot pass.
	 * Reason: Different priorities possible at junctions, which (almost) always result in different event order.
	 */ 
	
	/*
	public void test_equilPlans100_DEQSimEventFileComparator() {
		Gbl.reset();

		DEQSimEventFileComparator deqSimComparator = new DEQSimEventFileComparator("test/src/playground/wrashid/input/deqsim/deq_events100.txt");
		deqSimComparator.startTestPDES2("test/scenarios/equil/config.xml", true,
				"test/scenarios/equil/plans100.xml",
				null);
	}
	*/
	
	public void test_equilPlans100_DEQSimEventFileTravelTimeComparator() {
		// TODO: Test "frozen", the parallel version development has been stopped. 
		/*
		Gbl.reset();

		DEQSimEventFileTravelTimeComparator deqSimTravelTimeComparator = new DEQSimEventFileTravelTimeComparator("test/src/playground/wrashid/input/deqsim/deq_events100.txt",1);
		deqSimTravelTimeComparator.startTestPDES2("test/scenarios/equil/config.xml", false,
				"test/scenarios/equil/plans100.xml",
				null);
				*/
	}

}
