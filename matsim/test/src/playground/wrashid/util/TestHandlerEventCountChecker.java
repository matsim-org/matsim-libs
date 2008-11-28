package playground.wrashid.util;

import java.util.ArrayList;

import org.matsim.basic.v01.BasicPlanImpl.LegIterator;
import org.matsim.events.ActEndEvent;
import org.matsim.events.ActStartEvent;
import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.AgentStuckEvent;
import org.matsim.events.AgentWait2LinkEvent;
import org.matsim.events.LinkEnterEvent;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.events.handler.ActEndEventHandler;
import org.matsim.events.handler.ActStartEventHandler;
import org.matsim.events.handler.AgentArrivalEventHandler;
import org.matsim.events.handler.AgentDepartureEventHandler;
import org.matsim.events.handler.AgentStuckEventHandler;
import org.matsim.events.handler.AgentWait2LinkEventHandler;
import org.matsim.events.handler.EventHandler;
import org.matsim.events.handler.LinkEnterEventHandler;
import org.matsim.events.handler.LinkLeaveEventHandler;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.routes.CarRoute;
import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.DES.SimulationParameters;
import playground.wrashid.DES.util.testable.PopulationModifier;
import playground.wrashid.DES.util.testable.TestHandler;
import playground.wrashid.deqsim.DEQSimStarter;
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
			ArrayList<Object> actsLegs =plan.getActsLegs();
			expectedDepartureEvents+=actsLegs.size() / 2 ;
			
			LegIterator iter=plan.getIteratorLeg();
			while (iter.hasNext()){
				Leg leg=(Leg)iter.next();
				expectedLinkEnterEvents+=((CarRoute) leg.getRoute()).getLinks().length+1;
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
		String[] args = new String[1];
		args[0] = configFilePath;
		this.printEvent=printEvent;
		SimulationParameters.testEventHandler =this;
		
		if (planFilePath!=null){
			SimulationParameters.testPlanPath=planFilePath;
		} else {
			SimulationParameters.testPlanPath=null;
		}
		
		if (populationModifier!=null){
			SimulationParameters.testPopulationModifier=populationModifier;
		} else {
			SimulationParameters.testPopulationModifier=new DummyPopulationModifier();
		}		
		
		DEQSimStarter.main(args);
		this.estimateExpectedNumberOfEvents(SimulationParameters.testPopulationModifier.getPopulation());
		SimulationParameters.testEventHandler.checkAssertions();
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
