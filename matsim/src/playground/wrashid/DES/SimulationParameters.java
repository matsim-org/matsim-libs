package playground.wrashid.DES;

import java.util.ArrayList;

import org.matsim.events.Events;

public class SimulationParameters {
	// EventHeap
	
	static long simulationLength=Long.MAX_VALUE; //in ms
	public static long linkCapacityPeriod=0; // 
	public static double gapTravelSpeed=15.0; // in m/s
	public static double flowCapacityFactor=1.0;
	public static double storageCapacityFactor=1.0;
	public static double carSize=7.5; // in meter
	public static double minimumInFlowCapacity=1800; // diese konstante ist hardcodiert bei der C++ simulation (in Frz/stunde)
													// diese wird auch skaliert mit dem flowCapacityFactor
	
	public static ArrayList<EventLog> eventOutputLog=new ArrayList<EventLog>();
	
	public static final String START_LEG="start leg";
	public static final String END_LEG="end leg";
	public static final String ENTER_LINK="enter link";
	public static final String LEAVE_LINK="leave link";
	
	
	
	
	public static final boolean debugMode=true;
	public static final double maxAbsLinkAverage=0.01; // how far can the average usage of links differ for a test to pass


	public static Events events=null; // this is the simulation events object
	public static double stuckTime=Double.MAX_VALUE; // - this time is used for deadlock prevention.
														   // - when a car waits for more than 'stuckTime' for 
														   //   entering next road, it will enter the next 

}
