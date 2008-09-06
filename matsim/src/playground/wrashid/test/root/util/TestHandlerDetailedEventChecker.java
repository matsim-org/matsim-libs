package playground.wrashid.test.root.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import org.apache.commons.collections.map.HashedMap;
import org.matsim.basic.v01.BasicPlanImpl.LegIterator;
import org.matsim.events.ActEndEvent;
import org.matsim.events.ActStartEvent;
import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.AgentStuckEvent;
import org.matsim.events.AgentWait2LinkEvent;
import org.matsim.events.BasicEvent;
import org.matsim.events.LinkEnterEvent;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;

import playground.wrashid.DES.SimulationParameters;
import playground.wrashid.deqsim.DEQSimStarter;
import playground.wrashid.deqsim.PDESStarter2;

public class TestHandlerDetailedEventChecker extends TestHandler {

	private HashMap<String,LinkedList<BasicEvent>> events=new HashMap<String,LinkedList<BasicEvent>>();
	private HashMap<String,ExpectedNumberOfEvents> expectedNumberOfMessages=new HashMap<String,ExpectedNumberOfEvents>();
	public boolean printEvent = true;
	private Population population;
	
	public TestHandlerDetailedEventChecker(){
		
	}
	
	@Override
	public void checkAssertions() {
		// all events of one agent must have ascending time stamps
		double lastTimeStamp;
		for (LinkedList<BasicEvent> list:events.values()){
			lastTimeStamp=Double.NEGATIVE_INFINITY;
			for (int i=0;i<list.size();i++){
				if (lastTimeStamp>list.get(i).time){
						for (int j=0;j<list.size();j++){
							System.out.println(list.get(j).toString());
						}
						System.out.println(lastTimeStamp);
						System.out.println(list.get(i).time);
				}	
				assertEquals(true, lastTimeStamp<=list.get(i).time);
				lastTimeStamp=list.get(i).time;
			}
		}
		
		// compare with expected number of events per agent, per event type
		for (LinkedList<BasicEvent> list:events.values()){
			int linkEnterEventCounter = 0;
			int linkLeaveEventCounter = 0;
			int departureEventCounter = 0;
			int arrivalEventCounter = 0;
			for (int i=0;i<list.size();i++){
				if (list.get(i) instanceof LinkEnterEvent){
					linkEnterEventCounter++;
				}
				if (list.get(i) instanceof LinkLeaveEvent){
					linkLeaveEventCounter++;
				}
				if (list.get(i) instanceof AgentDepartureEvent){
					departureEventCounter++;
				}
				if (list.get(i) instanceof AgentArrivalEvent){
					arrivalEventCounter++;
				}

			}
			ExpectedNumberOfEvents expected=expectedNumberOfMessages.get(list.get(0).agentId);
			//if (estimate.expectedLinkEnterEvents!=linkEnterEventCounter){
			//	for (int j=0;j<list.size();j++){
			//		System.out.println(list.get(j).toString());
			//	}
			//} else {
			//	System.out.println("ok");
			//}
			
			assertEquals(expected.expectedLinkEnterEvents, linkEnterEventCounter);
			assertEquals(expected.expectedLinkLeaveEvents, linkLeaveEventCounter);
			assertEquals(expected.expectedDepartureEvents, departureEventCounter);
			assertEquals(expected.expectedArrivalEvents, arrivalEventCounter);
		}
		
		
		// check, that each enter event is followed by a leave event 
		// check, that the same road is left, which is entered
		for (LinkedList<BasicEvent> list:events.values()){
			for (int i=0;i<list.size();i++){
				if (list.get(i) instanceof LinkEnterEvent){
					assertEquals(true, list.get(i+1) instanceof LinkLeaveEvent);
					
					LinkEnterEvent enterEvent=(LinkEnterEvent)list.get(i);
					LinkLeaveEvent leaveEvent=(LinkLeaveEvent)list.get(i+1);
					//System.out.println(enterEvent);
					//if (enterEvent.link!=leaveEvent.link){
					//	System.out.println(leaveEvent.link.getId());
					//	System.out.println(leaveEvent.agentId);
					//	System.out.println(enterEvent.linkId);
					//}
					assertEquals(true, enterEvent.linkId.equalsIgnoreCase(leaveEvent.linkId));
				}				
			}
		}
		

		
		
	}

	@Override
	public void handleEvent(ActEndEvent event) {
		if (!events.containsKey(event.agentId)){
			events.put(event.agentId, new LinkedList<BasicEvent>());
		}
		events.get(event.agentId).add(event);
		if (printEvent) {
			System.out.println(event.toString());
		}
	}

	@Override
	public void reset(int iteration) {
		
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		if (!events.containsKey(event.agentId)){
			events.put(event.agentId, new LinkedList<BasicEvent>());
		}
		events.get(event.agentId).add(event);
		if (printEvent) {
			System.out.println(event.toString());
		}
	}

	@Override
	public void handleEvent(AgentWait2LinkEvent event) {
		if (!events.containsKey(event.agentId)){
			events.put(event.agentId, new LinkedList<BasicEvent>());
		}
		events.get(event.agentId).add(event);
		if (printEvent) {
			System.out.println(event.toString());
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (!events.containsKey(event.agentId)){
			events.put(event.agentId, new LinkedList<BasicEvent>());
		}
		events.get(event.agentId).add(event);
		if (printEvent) {
			System.out.println(event.toString());
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (!events.containsKey(event.agentId)){
			events.put(event.agentId, new LinkedList<BasicEvent>());
		}
		events.get(event.agentId).add(event);
		if (printEvent) {
			System.out.println(event.toString());
		}
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		if (!events.containsKey(event.agentId)){
			events.put(event.agentId, new LinkedList<BasicEvent>());
		}
		events.get(event.agentId).add(event);
		if (printEvent) {
			System.out.println(event.toString());
		}
	}

	@Override
	public void handleEvent(ActStartEvent event) {
		if (!events.containsKey(event.agentId)){
			events.put(event.agentId, new LinkedList<BasicEvent>());
		}
		events.get(event.agentId).add(event);
		if (printEvent) {
			System.out.println(event.toString());
		}
	}

	@Override
	public void handleEvent(AgentStuckEvent event) {
		if (!events.containsKey(event.agentId)){
			events.put(event.agentId, new LinkedList<BasicEvent>());
		}
		events.get(event.agentId).add(event);
		if (printEvent) {
			System.out.println(event.toString());
		}
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
		this.calculateExpectedNumberOfEvents(SimulationParameters.testPopulationModifier.getPopulation());
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
		this.calculateExpectedNumberOfEvents(playground.wrashid.PDES2.SimulationParameters.testPopulationModifier.getPopulation());
		playground.wrashid.PDES2.SimulationParameters.testEventHandler.checkAssertions();
	}
	
	public void calculateExpectedNumberOfEvents(Population population){
		this.population = population;
		
		for (Person p:population.getPersons().values()){
			Plan plan= p.getSelectedPlan();
			ExpectedNumberOfEvents expected=new ExpectedNumberOfEvents();
			ArrayList<Object> actsLegs =plan.getActsLegs();
			expected.expectedDepartureEvents+=actsLegs.size() / 2 ;
			
			LegIterator iter=plan.getIteratorLeg();
			while (iter.hasNext()){
				Leg leg=(Leg)iter.next();
				// at the moment only cars are simulated on the road
				if (leg.getMode().equalsIgnoreCase("car")){
					expected.expectedLinkEnterEvents+=leg.getRoute().getLinkRoute().length+1;
				}
			}
			
			expected.expectedArrivalEvents=expected.expectedDepartureEvents;
			expected.expectedLinkLeaveEvents=expected.expectedLinkEnterEvents;
			
			expectedNumberOfMessages.put(p.getId().toString(), expected);
		}
	}
	
	private class ExpectedNumberOfEvents{
		public int expectedLinkEnterEvents;
		public int expectedLinkLeaveEvents;
		public int expectedDepartureEvents;
		public int expectedArrivalEvents; 
	}

}


