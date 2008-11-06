package playground.wrashid.util;

import java.util.ArrayList;
import java.util.LinkedList;

import org.matsim.events.PersonEvent;

import playground.wrashid.DES.EventLog;
import playground.wrashid.tryouts.starting.CppEventFileParser;

public class DEQSimEventFileComparator extends
TestHandlerDetailedEventChecker {
	
	public String pathToDEQSimEventsFile=null;
	
	public DEQSimEventFileComparator(String path){
		pathToDEQSimEventsFile=path;
	}
	
	// compare events to event file
	public void checkAssertions() {
		ArrayList<EventLog> deqSimLog=CppEventFileParser.parseFile(pathToDEQSimEventsFile);
		for (int i=0;i<allEvents.size();i++){
			assertEquals(true,CppEventFileParser.equals(allEvents.get(i), deqSimLog.get(i)));
		}
	}

}
