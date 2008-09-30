package playground.wrashid.deqsim;

import java.util.LinkedList;

import org.matsim.events.ActEndEvent;
import org.matsim.events.ActStartEvent;
import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.AgentStuckEvent;
import org.matsim.events.AgentWait2LinkEvent;
import org.matsim.events.BasicEvent;
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
import playground.wrashid.scenarios.EquilPopulationPlans1Modified1;
import playground.wrashid.util.TestHandlerDetailedEventChecker;
import playground.wrashid.util.TestHandlerEventCountChecker;

public class PDESStarter2Test extends MatsimTestCase {

	public void testScenarios() {
		t_equilPlans1();
		Gbl.reset();

		// t_equilEvent();
		// Gbl.reset();

		// t_Berlin();
		// Gbl.reset();
		// only comment this, when test stabelized again.
		// assertEquals(true, false);
	}

	private void t_equilEvent() {
		TestHandlerDetailedEventChecker orderChecker = new TestHandlerDetailedEventChecker();
		orderChecker.startTestPDES2("test/scenarios/equil/config.xml", false,
				null, null);
	}

	private void t_equilPlans1() {
		TestHandlerDetailedEventChecker orderChecker = new TestHandlerDetailedEventChecker();
		orderChecker.startTestPDES2("test/scenarios/equil/config.xml", true,
				"test/scenarios/equil/plans1.xml",
				new EquilPopulationPlans1Modified1());

		Gbl.reset();

		EquilPlans1EventCheckHandler eventChecker = new EquilPlans1EventCheckHandler();
		eventChecker.startTestPDES2("test/scenarios/equil/config.xml", true,
				"test/scenarios/equil/plans1.xml",
				new EquilPopulationPlans1Modified1());

	}

	private void t_Berlin() {
		TestHandlerDetailedEventChecker orderChecker = new TestHandlerDetailedEventChecker();
		orderChecker.startTestPDES2("test/scenarios/berlin/config.xml", false,
				null, null);
	}

	private class EquilPlans1EventCheckHandler extends
			TestHandlerDetailedEventChecker {
		public void checkAssertions() {
			LinkedList<BasicEvent> list = events.get("1");

			assertEquals(true, list.get(0) instanceof AgentDepartureEvent);

			assertEquals(true, list.get(1) instanceof LinkEnterEvent);

			assertEquals(true, list.get(2) instanceof LinkLeaveEvent);

			assertEquals(true, list.get(3) instanceof LinkEnterEvent);

			assertEquals(true, list.get(4) instanceof LinkLeaveEvent);

			assertEquals(true, list.get(5) instanceof LinkEnterEvent);

			assertEquals(true, list.get(6) instanceof LinkLeaveEvent);

			
			// TODO: continue here...

			assertEquals(true, list.get(21) instanceof AgentArrivalEvent);

			/*
			 * 
			 * <module name="deqsim" > 
			 * <param name="endTime" value="1000:00:00" />
			 * <param name="flowCapacityFactor" value="1.0" /> 
			 * <param name="squeezeTime" value="00:01:40" />
			 * <param name="carSize" value="7.5" />
			 * <param name="gapTravelSpeed" value="15.0" />
			 * <param name="startTime" value="00:00:00" />
			 * <param name="storageCapacityFactor" value="1.0" />
			 * </module>
			 */

			/*
			 * 
			 * 
			 * 
			 * <?xml version="1.0" ?> <!DOCTYPE plans SYSTEM
			 * "http://www.matsim.org/files/dtd/plans_v4.dtd"> <plans> <person
			 * id="1"> <plan> <act type="h" x="-25000" y="0" link="1"
			 * end_time="06:00" /> <leg mode="car" dep_time="00:06:00"> <route>2
			 * 7 12</route> </leg> <act type="w" x="10000" y="0" link="20"
			 * dur="00:10" /> <leg mode="car" dep_time="00:15:00"> <route>
			 * </route> </leg> <act type="w" x="10000" y="0" link="20"
			 * dur="03:30" /> <leg mode="car" dep_time="00:33:20"> <route>13 14
			 * 15 1</route> </leg> <act type="h" x="-25000" y="0" link="1" />
			 * </plan> </person> </plans>
			 * 
			 */

		}
	}

}
