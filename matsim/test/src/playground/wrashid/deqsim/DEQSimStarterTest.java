package playground.wrashid.deqsim;

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
import playground.wrashid.scenarios.EquilPopulationPlans1Modified1;
import playground.wrashid.util.DummyPopulationModifier;
import playground.wrashid.util.PopulationModifier;
import playground.wrashid.util.TestHandler;
import playground.wrashid.util.TestHandlerDetailedEventChecker;
import playground.wrashid.util.TestHandlerEventCountChecker;

public class DEQSimStarterTest extends MatsimTestCase {

	public void testScenarios() {
		t_equilPlans1();
		Gbl.reset();
		
		t_equilEvent();
		Gbl.reset();
		
		t_Berlin();
		Gbl.reset();
	}
	
	private void t_equilEvent() {
		TestHandlerDetailedEventChecker orderChecker = new TestHandlerDetailedEventChecker();
		orderChecker.startTestDES("test/scenarios/equil/config.xml",false,null,null);
	}
	
	private void t_equilPlans1() {
		TestHandlerDetailedEventChecker orderChecker= new TestHandlerDetailedEventChecker();
		orderChecker.startTestDES("test/scenarios/equil/config.xml",false,"test/scenarios/equil/plans1.xml",new EquilPopulationPlans1Modified1());
	}
	
	private void t_Berlin() {
		TestHandlerDetailedEventChecker orderChecker = new TestHandlerDetailedEventChecker();
		orderChecker.startTestDES("test/scenarios/berlin/config.xml",false,null,null);
	}
	

}
