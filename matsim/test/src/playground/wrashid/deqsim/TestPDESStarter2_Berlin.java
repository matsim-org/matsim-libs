package playground.wrashid.deqsim;

import java.util.ArrayList;
import java.util.LinkedList;

import org.matsim.core.events.AgentArrivalEvent;
import org.matsim.core.events.AgentDepartureEvent;
import org.matsim.core.events.LinkEnterEvent;
import org.matsim.core.events.LinkLeaveEvent;
import org.matsim.core.events.PersonEvent;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.jdeqsim.EventLog;
import org.matsim.core.mobsim.jdeqsim.scenarios.EquilPopulationPlans1Modified1;
import org.matsim.core.mobsim.jdeqsim.util.CppEventFileParser;
import org.matsim.core.mobsim.jdeqsim.util.DEQSimEventFileComparator;
import org.matsim.core.mobsim.jdeqsim.util.DEQSimEventFileTravelTimeComparator;
import org.matsim.core.mobsim.jdeqsim.util.TestHandlerDetailedEventChecker;
import org.matsim.testcases.MatsimTestCase;


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
