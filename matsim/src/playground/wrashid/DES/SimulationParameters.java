package playground.wrashid.DES;

import java.util.ArrayList;
import java.util.HashMap;

import org.matsim.events.Events;
import org.matsim.events.handler.EventHandler;
import org.matsim.population.Plan;
import org.matsim.population.Population;

import playground.wrashid.DES.util.testable.PopulationModifier;
import playground.wrashid.DES.util.testable.TestHandler;

public class SimulationParameters {
	// EventHeap
	
	static long maxSimulationLength=800000; //in s 
	public static long linkCapacityPeriod=0; // 
	public static double gapTravelSpeed=15.0; // in m/s
	public static double flowCapacityFactor=1.0;
	public static double storageCapacityFactor=1.0;
	public static double carSize=7.5; // in meter
	public static double minimumInFlowCapacity=1800; // diese konstante ist hardcodiert bei der C++ simulation (in Frz/stunde)
													// diese wird auch skaliert mit dem flowCapacityFactor
													// Attention: This is 1800 per lane!!!!
	
	//public static ArrayList<EventLog> eventOutputLog=new ArrayList<EventLog>();
	
	public static final String START_LEG="start leg";
	public static final String END_LEG="end leg";
	public static final String ENTER_LINK="enter link";
	public static final String LEAVE_LINK="leave link";
	
	
	
	
	public static final boolean debugMode=false;
	public static final double maxAbsLinkAverage=0.01; // how far can the average usage of links differ for a test to pass


	public static Events events=null; // this is the simulation events object
	public static double stuckTime=Double.MAX_VALUE; // - this time is used for deadlock prevention.
														   // - when a car waits for more than 'stuckTime' for 
														   //   entering next road, it will enter the next 

	
	// a higher priority comes first in the message queue (when same time)
	// a person has a enter road message at the same time as leaving the previous road
	// for events with same time stamp: leave < arrival < departure < enter
	// especially for testing this is important
	// only important for messages of same person
	public static final int PRIORITY_LEAVE_ROAD_MESSAGE=200;
	public static final int PRIORITY_ARRIVAL_MESSAGE=150;
	public static final int PRIORITY_DEPARTUARE_MESSAGE=125;
	public static final int PRIORITY_ENTER_ROAD_MESSAGE=100;
	
	// simulation internal parameters
	
	private static boolean GC_MESSAGES = false;
	
	
	
	// test injection variables
	public static TestHandler testEventHandler=null;
	public static String testPlanPath=null;
	public static PopulationModifier testPopulationModifier=null;
	
	// this must be initialized before starting the simulation!
	public static HashMap<String, Road> allRoads=null;

	public static boolean isGC_MESSAGES() {
		return GC_MESSAGES;
	}

	public static void setGC_MESSAGES(boolean gc_messages) {
		GC_MESSAGES = gc_messages;
	}
	
}
