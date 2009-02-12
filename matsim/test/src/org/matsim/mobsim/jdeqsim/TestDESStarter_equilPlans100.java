package org.matsim.mobsim.jdeqsim;


import org.matsim.gbl.Gbl;
import org.matsim.mobsim.jdeqsim.util.DEQSimEventFileTravelTimeComparator;
import org.matsim.mobsim.jdeqsim.util.TestHandlerDetailedEventChecker;

import org.matsim.testcases.MatsimTestCase;



public class TestDESStarter_equilPlans100 extends MatsimTestCase {
	
	public void test_equilPlans100_TestHandlerDetailedEventChecker() {
		Gbl.reset();
		
		TestHandlerDetailedEventChecker detailedChecker = new TestHandlerDetailedEventChecker();
		detailedChecker.startTestDES("test/scenarios/equil/config.xml", false,
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
	
	/*
	 * This test was turn off, because java deqsim is based on time specified on the acts (as mobsim)
	 * and is not based on times specified on the leg as in c++ deqsim
	 */
	
	/*
	public void test_equilPlans100_DEQSimEventFileTravelTimeComparator() {
		Gbl.reset();

		DEQSimEventFileTravelTimeComparator deqSimTravelTimeComparator = new DEQSimEventFileTravelTimeComparator("test/input/org/matsim/mobsim/deqsim/deq_events_100.txt",1);
		deqSimTravelTimeComparator.startTestDES("test/scenarios/equil/config.xml", false,
				"test/scenarios/equil/plans100.xml",
				null);
	}
*/
}
