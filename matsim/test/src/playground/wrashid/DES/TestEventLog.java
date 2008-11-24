package playground.wrashid.DES;

import java.util.ArrayList;

import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.tryouts.starting.CppEventFileParser;

public class TestEventLog extends MatsimTestCase {

	
	
	public void testGetTravelTime(){
		ArrayList<EventLog> deqSimLog=CppEventFileParser.parseFile("test/src/playground/wrashid/input/deqsim/deq_events.txt");
		assertEquals(3599.0, Math.floor(EventLog.getTravelTime(deqSimLog,1)));
	}
	
	
	public void testGetAverageTravelTime(){
		ArrayList<EventLog> deqSimLog=CppEventFileParser.parseFile("test/src/playground/wrashid/input/deqsim/deq_events.txt");
		assertEquals(EventLog.getTravelTime(deqSimLog,1), EventLog.getSumTravelTime(deqSimLog));
	}
}
