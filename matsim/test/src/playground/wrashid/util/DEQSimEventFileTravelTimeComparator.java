package playground.wrashid.util;

import java.util.ArrayList;
import java.util.LinkedList;

import org.matsim.events.PersonEvent;

import playground.wrashid.DES.EventLog;
import playground.wrashid.DES.utils.EventLibrary;
import playground.wrashid.tryouts.starting.CppEventFileParser;

public class DEQSimEventFileTravelTimeComparator extends
TestHandlerDetailedEventChecker {
	
	public String pathToDEQSimEventsFile=null;
	private double tolerenzPercentValue=0.0;
	private ArrayList<EventLog> deqSimLog=null;
	
	public DEQSimEventFileTravelTimeComparator(String path, double tolerenzPercentValue){
		this.pathToDEQSimEventsFile=path;
		this.tolerenzPercentValue=tolerenzPercentValue;
	}
	
	/*
	 * As DEQSimEventFileComparator does not function for most comparisons of the JavaDEQSim and C++DEQSim model,
	 * we need to compare the time each car was on the road and take its average. This figure should with in a small interval
	 * for both simulations.
	 * Attention: Still when vehicles are stuck, this comparison can be off by larger number, because unstucking the vehicles is
	 * done in different ways by the two simulations 
	 */
	public void checkAssertions() {
		deqSimLog=CppEventFileParser.parseFile(pathToDEQSimEventsFile);
		assertEquals(true,checkDifferenceTravelTime());
	}
	
	
	
	/*
	 * - The difference in travel time should be smaller than the tolerenz percent value	 * 
	 */
	private boolean checkDifferenceTravelTime(){
		boolean result=false;
		double deqSimTravelSum=EventLog.getSumTravelTime(deqSimLog);
		double javaSimTravelSum=EventLibrary.getSumTravelTime(allEvents);
		
		if ((Math.abs(deqSimTravelSum - javaSimTravelSum)/deqSimTravelSum)<tolerenzPercentValue){
			result=true;
		}
		
		return result;
	}
	

}
