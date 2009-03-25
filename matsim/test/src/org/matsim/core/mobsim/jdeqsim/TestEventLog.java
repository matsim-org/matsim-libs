package org.matsim.core.mobsim.jdeqsim;

import java.util.ArrayList;

import org.matsim.core.mobsim.jdeqsim.EventLog;
import org.matsim.core.mobsim.jdeqsim.util.CppEventFileParser;
import org.matsim.testcases.MatsimTestCase;


public class TestEventLog extends MatsimTestCase {

	
	
	public void testGetTravelTime(){
		ArrayList<EventLog> deqSimLog=CppEventFileParser.parseFile("test/input/org/matsim/mobsim/jdeqsim/deq_events.txt");
		assertEquals(3599.0, Math.floor(EventLog.getTravelTime(deqSimLog,1)));
	}
	
	
	public void testGetAverageTravelTime(){
		ArrayList<EventLog> deqSimLog=CppEventFileParser.parseFile("test/input/org/matsim/mobsim/jdeqsim/deq_events.txt");
		assertEquals(EventLog.getTravelTime(deqSimLog,1), EventLog.getSumTravelTime(deqSimLog));
	}
}
