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

public class TestPDESStarter2_Berlin extends MatsimTestCase {

	
	
	
	
	public void test_Berlin_TestHandlerDetailedEventChecker() {
		// TODO: Test "frozen", the parallel version development has been stopped. 
		/*
		Gbl.reset();
		
		TestHandlerDetailedEventChecker detailedChecker = new TestHandlerDetailedEventChecker();
		detailedChecker.startTestPDES2("test/scenarios/berlin/config.xml", false,
				null, null);
				*/
	}
	
	

	

}
