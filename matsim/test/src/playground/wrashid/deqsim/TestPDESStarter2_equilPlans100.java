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

public class TestPDESStarter2_equilPlans100 extends MatsimTestCase {
	
	public void test_equilPlans100_TestHandlerDetailedEventChecker() {
		Gbl.reset();
		
		TestHandlerDetailedEventChecker detailedChecker = new TestHandlerDetailedEventChecker();
		detailedChecker.startTestPDES2("test/scenarios/equil/config.xml", true,
				"test/scenarios/equil/plans100.xml",
				null);
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
		Gbl.reset();

		DEQSimEventFileTravelTimeComparator deqSimTravelTimeComparator = new DEQSimEventFileTravelTimeComparator("test/src/playground/wrashid/input/deqsim/deq_events100.txt",5);
		deqSimTravelTimeComparator.startTestPDES2("test/scenarios/equil/config.xml", true,
				"test/scenarios/equil/plans100.xml",
				null);
	}

}
