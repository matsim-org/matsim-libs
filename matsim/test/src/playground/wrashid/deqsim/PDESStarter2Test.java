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
import playground.wrashid.util.TestHandlerDetailedEventChecker;

public class PDESStarter2Test extends MatsimTestCase {

	public void testScenarios() {
		//t_equilPlans1(); // das sollte laufen...
		//Gbl.reset();
		
		 t_equilEvent(); // mit enable assertion flag funktioniert es nicht mehr!!!
		 Gbl.reset();

		// t_Berlin();
		// Gbl.reset();
		// only comment this, when test stabelized again.
		// assertEquals(true, false);
	}

	
	private void t_equilPlans1() {
		TestHandlerDetailedEventChecker detailedChecker = new TestHandlerDetailedEventChecker();
		detailedChecker.startTestPDES2("test/scenarios/equil/config.xml", true,
				"test/scenarios/equil/plans1.xml",
				new EquilPopulationPlans1Modified1());

		Gbl.reset();

		DEQSimEventFileComparator deqSimComparator = new DEQSimEventFileComparator("test/src/playground/wrashid/input/deqsim/deq_events.txt");
		deqSimComparator.startTestPDES2("test/scenarios/equil/config.xml", true,
				"test/scenarios/equil/plans1.xml",
				new EquilPopulationPlans1Modified1());

	}
	
	
	private void t_equilEvent() {
		TestHandlerDetailedEventChecker detailedChecker = new TestHandlerDetailedEventChecker();
		detailedChecker.startTestPDES2("test/scenarios/equil/config.xml", false,
				null, null);
		
		Gbl.reset();
		
		// This test cannot function, because DEQSim and JDEQSim can have different orders of cars at junction
		//DEQSimEventFileComparator deqSimComparator = new DEQSimEventFileComparator("test/src/playground/wrashid/input/deqsim/deq_events100.txt");
		//deqSimComparator.startTestPDES2("test/scenarios/equil/config.xml", false,
		//		null, null);
	}



	private void t_Berlin() {
		TestHandlerDetailedEventChecker orderChecker = new TestHandlerDetailedEventChecker();
		orderChecker.startTestPDES2("test/scenarios/berlin/config.xml", false,
				null, null);
	}

	

}
