package playground.wrashid.DES.utils;

import java.util.ArrayList;
import java.util.LinkedList;

import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.PersonEvent;
import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.DES.EventLog;
import playground.wrashid.DES.util.EventLibrary;
import playground.wrashid.tryouts.starting.CppEventFileParser;

public class TestEventLibrary extends MatsimTestCase {
	
	public void testGetTravelTime(){
		LinkedList<PersonEvent> events=new LinkedList<PersonEvent>();
		events.add(new AgentDepartureEvent(20,"2","0",0));
		events.add(new AgentArrivalEvent(30,"2","0",0));
		events.add(new AgentDepartureEvent(90,"1","0",0));
		events.add(new AgentArrivalEvent(110,"1","0",0));
		
		assertEquals(20.0, EventLibrary.getTravelTime(events,1));
	}
	
	public void testGetAverageTravelTime(){
		LinkedList<PersonEvent> events=new LinkedList<PersonEvent>();
		events.add(new AgentDepartureEvent(20,"2","0",0));
		events.add(new AgentArrivalEvent(30,"2","0",0));
		events.add(new AgentDepartureEvent(90,"1","0",0));
		events.add(new AgentArrivalEvent(110,"1","0",0));
		
		assertEquals(30.0, EventLibrary.getSumTravelTime(events));
	}
	
}
