package playground.wrashid.test.root.deqsim;

import java.util.ArrayList;

import org.matsim.basic.v01.Id;
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
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.PopulationReader;
import org.matsim.testcases.MatsimTestCase;

import org.matsim.gbl.Gbl;
import playground.wrashid.DES.SimulationParameters;
import playground.wrashid.deqsim.DEQSimStarter;
import playground.wrashid.test.root.util.DummyPopulationModifier;
import playground.wrashid.test.root.util.PopulationModifier;
import playground.wrashid.test.root.util.TestHandlerEventCountChecker;
import playground.wrashid.test.root.util.TestHandler;
import playground.wrashid.test.root.scenarios.EquilPopulationPlans1Modified1;

public class DEQSimStarterTest extends MatsimTestCase {

	public void testScenarios() {
		t_equilPlans1EventCounts();
		Gbl.reset();
		t_equilEventCounts();
	}
	
	private void t_equilEventCounts() {
		TestHandlerEventCountChecker countChecker= new TestHandlerEventCountChecker();
		countChecker.startTestDES("test/scenarios/equil/config.xml",false,null,null);
	}
	
	private void t_equilPlans1EventCounts() {
		TestHandlerEventCountChecker countChecker= new TestHandlerEventCountChecker();
		countChecker.startTestDES("test/scenarios/equil/config.xml",false,"test/scenarios/equil/plans1.xml",new EquilPopulationPlans1Modified1());
	}
	
	// noch testen, ob departure und arrival in richtiger reihenfolge passieren
	// vielleicht noch detailliertere kontrolle für einen bestimmten agent
	// eventuell eine single agent simulation machen: da kann man sagen, es soll einfach
	// mit dem plan exact vergleichen, weil ja dieser alle strassen einfach abfahren kann
}
