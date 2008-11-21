package playground.wrashid.util;

import java.util.ArrayList;
import java.util.LinkedList;

import org.matsim.events.PersonEvent;

import playground.wrashid.DES.EventLog;
import playground.wrashid.tryouts.starting.CppEventFileParser;

public class DEQSimEventFileTravelTimeComparator extends
TestHandlerDetailedEventChecker {
	
	public String pathToDEQSimEventsFile=null;
	
	public DEQSimEventFileTravelTimeComparator(String path){
		pathToDEQSimEventsFile=path;
	}
	
	/*
	 * As DEQSimEventFileComparator does not function for most comparisons of the JavaDEQSim and C++DEQSim model,
	 * we need to compare the time each car was on the road and take its average. This figure should with in a small interval
	 * for both simulations.
	 * Attention: Still when vehicles are stuck, this comparison can be off by larger number, because unstucking the vehicles is
	 * done in different ways by the two simulations 
	 */
	public void checkAssertions() {
		// TODO: implement this.
	}

}
