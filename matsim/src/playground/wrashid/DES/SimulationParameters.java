package playground.wrashid.DES;

import java.util.ArrayList;

public class SimulationParameters {
	// EventHeap
	
	static long simulationLength=Long.MAX_VALUE; //in ms
	public static long linkCapacityPeriod=0; // wird initializiert im JavaDEQSim.java
	public static double gapTravelSpeed=15.0; // in m/s
	public static double flowCapacityFactor=0.33;
	public static double storageCapacityFactor=0.33;
	public static double carSize=7.5; // in meter
	public static double minimumInFlowCapacity=1800; // diese konstante ist hardcodiert bei der C++ simulation (in Frz/stunde)
													// diese wird auch skaliert mit dem flowCapacityFactor
	
	public static ArrayList<EventLog> eventOutputLog=new ArrayList<EventLog>();
	
	public static final String START_LEG="start leg";
	public static final String END_LEG="end leg";
	public static final String ENTER_LINK="enter link";
	public static final String LEAVE_LINK="leave link";
	
	
	
	
	public static final boolean debugMode=true;
}
