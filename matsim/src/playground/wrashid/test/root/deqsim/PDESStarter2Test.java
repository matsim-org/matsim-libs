package playground.wrashid.test.root.deqsim;

import org.matsim.events.ActEndEvent;
import org.matsim.events.ActStartEvent;
import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.AgentStuckEvent;
import org.matsim.events.AgentWait2LinkEvent;
import org.matsim.events.Events;
import org.matsim.events.LinkEnterEvent;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.events.handler.ActEndEventHandler;
import org.matsim.events.handler.ActStartEventHandler;
import org.matsim.events.handler.AgentArrivalEventHandler;
import org.matsim.events.handler.AgentDepartureEventHandler;
import org.matsim.events.handler.AgentStuckEventHandler;
import org.matsim.events.handler.AgentWait2LinkEventHandler;
import org.matsim.events.handler.LinkEnterEventHandler;
import org.matsim.events.handler.LinkLeaveEventHandler;
import org.matsim.testcases.MatsimTestCase;

import org.matsim.gbl.Gbl;
import playground.wrashid.PDES2.SimulationParameters;
import playground.wrashid.deqsim.DEQSimStarter;
import playground.wrashid.deqsim.PDESController2;
import playground.wrashid.deqsim.PDESStarter2;
import playground.wrashid.test.root.scenarios.EquilPopulationPlans1Modified1;
import playground.wrashid.test.root.util.TestHandlerEventCountChecker;
import playground.wrashid.test.root.util.TestHandlerDetailedEventChecker;

public class PDESStarter2Test extends MatsimTestCase {

	public void testScenarios() {
		//t_equilPlans1();
		Gbl.reset();
		
		//t_equilEvent();
		Gbl.reset();
		
		t_Berlin();
		Gbl.reset();
		// only comment this, when test stabelized again.
		assertEquals(true, false);
	}
	
	
	private void t_equilEvent() {
		TestHandlerDetailedEventChecker orderChecker = new TestHandlerDetailedEventChecker();
		orderChecker.startTestPDES2("test/scenarios/equil/config.xml",false,null,null);
	}
	
	private void t_equilPlans1() {
		TestHandlerDetailedEventChecker orderChecker= new TestHandlerDetailedEventChecker();
		orderChecker.startTestPDES2("test/scenarios/equil/config.xml",false,"test/scenarios/equil/plans1.xml",new EquilPopulationPlans1Modified1());
	}
	
	private void t_Berlin() {
		TestHandlerDetailedEventChecker orderChecker = new TestHandlerDetailedEventChecker();
		orderChecker.startTestPDES2("test/scenarios/berlin/config.xml",false,null,null);
	}

}
