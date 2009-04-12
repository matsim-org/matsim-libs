package playground.wrashid.util;

import java.util.List;

import org.matsim.api.basic.v01.population.BasicPlanElement;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.api.population.NetworkRoute;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.Population;
import org.matsim.core.basic.v01.BasicPlanImpl.LegIterator;
import org.matsim.core.config.Config;
import org.matsim.core.events.ActEndEvent;
import org.matsim.core.events.ActStartEvent;
import org.matsim.core.events.AgentArrivalEvent;
import org.matsim.core.events.AgentDepartureEvent;
import org.matsim.core.events.AgentStuckEvent;
import org.matsim.core.events.AgentWait2LinkEvent;
import org.matsim.core.events.Events;
import org.matsim.core.events.LinkEnterEvent;
import org.matsim.core.events.LinkLeaveEvent;
import org.matsim.core.events.handler.ActEndEventHandler;
import org.matsim.core.events.handler.ActStartEventHandler;
import org.matsim.core.events.handler.AgentArrivalEventHandler;
import org.matsim.core.events.handler.AgentDepartureEventHandler;
import org.matsim.core.events.handler.AgentStuckEventHandler;
import org.matsim.core.events.handler.AgentWait2LinkEventHandler;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.events.handler.LinkEnterEventHandler;
import org.matsim.core.events.handler.LinkLeaveEventHandler;
import org.matsim.core.events.parallelEventsHandler.ParallelEvents;
import org.matsim.core.mobsim.jdeqsim.JDEQSimulation;
import org.matsim.core.mobsim.jdeqsim.util.DummyPopulationModifier;
import org.matsim.core.mobsim.jdeqsim.util.testable.PopulationModifier;
import org.matsim.core.network.NetworkLayer;
import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.PDES2.TestHandler;
import playground.wrashid.deqsim.PDESStarter2;

public class TestHandlerEventCountChecker extends MatsimTestCase implements
TestHandler, ActEndEventHandler, ActStartEventHandler,
AgentDepartureEventHandler, AgentStuckEventHandler,
AgentWait2LinkEventHandler, AgentArrivalEventHandler, EventHandler,
LinkEnterEventHandler, LinkLeaveEventHandler  {

	private int expectedLinkEnterEvents;
	private int expectedLinkLeaveEvents;
	private int expectedDepartureEvents;
	private int expectedArrivalEvents;
	
	private int eventCounter = 0;
	private int linkEnterEventCounter = 0;
	private int linkLeaveEventCounter = 0;
	private int departureEventCounter = 0;
	private int arrivalEventCounter = 0;

	public boolean printEvent = true;

	
	public void checkAssertions() {
		assertEquals(expectedLinkEnterEvents,linkEnterEventCounter); 
		assertEquals(expectedLinkLeaveEvents,linkLeaveEventCounter); 
		assertEquals(expectedDepartureEvents,departureEventCounter);
		assertEquals(expectedArrivalEvents,arrivalEventCounter); 
	}
	
	public void estimateExpectedNumberOfEvents(Population population){
		// for each leg: we have one departure and one arrival event
		// the current model is, that we always enter the first link
		// and do not enter the last link (only wait until we can enter). 
		// This means, we have the same number of enters as leaves.
		// Special case: empty path => enter link and drive along the link and then leave it again
		// later: for each act: one actstart and actend
		
		// assumption, there is always one leg less than acts in a plan
		
		expectedLinkEnterEvents=0;
		expectedLinkLeaveEvents=0;
		expectedDepartureEvents=0;
		expectedArrivalEvents=0;
		
		for (Person p:population.getPersons().values()){
			Plan plan= p.getSelectedPlan();
			List<? extends BasicPlanElement> actsLegs =plan.getPlanElements();
			expectedDepartureEvents+=actsLegs.size() / 2 ;
			
			LegIterator iter=plan.getIteratorLeg();
			while (iter.hasNext()){
				Leg leg=(Leg)iter.next();
				expectedLinkEnterEvents+=((NetworkRoute) leg.getRoute()).getLinks().size()+1;
			}
		}
		
		expectedArrivalEvents=expectedDepartureEvents;
		expectedLinkLeaveEvents=expectedLinkEnterEvents;
	}
	
	//public TestHandlerEventCountChecker(int expectedLinkEnterEvents, int expectedLinkLeaveEvents, int expectedDepartureEvents, int expectedArrivalEvents){
	//	this.expectedLinkEnterEvents=expectedLinkEnterEvents;
	//	this.expectedLinkLeaveEvents=expectedLinkLeaveEvents;
	//	this.expectedDepartureEvents=expectedDepartureEvents;
	//	this.expectedArrivalEvents=expectedArrivalEvents;
	//}
	
	public TestHandlerEventCountChecker(){
	}

	public void reset(final int iteration) {
		this.eventCounter = 0;
	}

	public void handleEvent(final ActEndEvent event) {
		this.eventCounter++;
	}

	public void handleEvent(final AgentDepartureEvent event) {
		this.departureEventCounter++;
		if (printEvent) {
			System.out.println(event.toString());
		}
	}

	public void handleEvent(final AgentWait2LinkEvent event) {
		this.eventCounter++;
	}

	public void handleEvent(final LinkLeaveEvent event) {
		this.linkLeaveEventCounter++;
		if (printEvent) {
			System.out.println(event.toString());
		}
	}

	public void handleEvent(final LinkEnterEvent event) {
		this.linkEnterEventCounter++;
		if (printEvent) {
			System.out.println(event.toString());
		}
	}

	public void handleEvent(final AgentArrivalEvent event) {
		this.arrivalEventCounter++;
		if (printEvent) {
			System.out.println(event.toString());
		}
	}

	public void handleEvent(final ActStartEvent event) {
		this.eventCounter++;
	}

	public void handleEvent(final AgentStuckEvent event) {
		this.eventCounter++;
	}
	
	// if populationModifier == null, then the DummyPopulationModifier is used
	// if planFilePath == null, then the plan specified in the config file is used
	public void startTestDES(String configFilePath,boolean printEvent,String planFilePath,PopulationModifier populationModifier) {
		Config config = loadConfig(configFilePath);
		if (planFilePath != null) {
			config.plans().setInputFile(planFilePath);
		}
		this.printEvent=printEvent;

		ScenarioImpl data = new ScenarioImpl(config);
		NetworkLayer network = data.getNetwork();
		Population population = data.getPopulation();
		if (populationModifier != null) {
			population = populationModifier.modifyPopulation(population);
		}
		Events events = new ParallelEvents(1);
		events.addHandler(this);
		events.initProcessing();
		new JDEQSimulation(network, population, events).run();
		events.finishProcessing();
		
		this.estimateExpectedNumberOfEvents(population);
		this.checkAssertions();
	}
	
	// if populationModifier == null, then the DummyPopulationModifier is used
	// if planFilePath == null, then the plan specified in the config file is used
	public void startTestPDES2(String configFilePath,boolean printEvent,String planFilePath,PopulationModifier populationModifier) {
		String[] args = new String[1];
		args[0] = configFilePath;
		this.printEvent=printEvent;
		playground.wrashid.PDES2.SimulationParameters.testEventHandler =this;
		
		if (planFilePath!=null){
			playground.wrashid.PDES2.SimulationParameters.testPlanPath=planFilePath;
		} else {
			playground.wrashid.PDES2.SimulationParameters.testPlanPath=null;
		}
		
		if (populationModifier!=null){
			playground.wrashid.PDES2.SimulationParameters.testPopulationModifier=populationModifier;
		} else {
			playground.wrashid.PDES2.SimulationParameters.testPopulationModifier=new DummyPopulationModifier();
		}		
		
		PDESStarter2.main(args);
		this.estimateExpectedNumberOfEvents(playground.wrashid.PDES2.SimulationParameters.testPopulationModifier.getPopulation());
		playground.wrashid.PDES2.SimulationParameters.testEventHandler.checkAssertions();
	}
	

}
