package playground.wrashid.PDES;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.matsim.events.BasicEvent;
import org.matsim.events.Events;

import playground.wrashid.PDES.util.ConcurrentListSPSC;

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
	
	public static ArrayList<EventLog> eventOutputLog=new ArrayList<EventLog>();
	
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

	
	
	// simulation internal parameters
	
	// specifies the minimal queue length for using in all message static constructers
    // this parameter needs to be set to avoid race conditions
	public static final int minQueueLength=Runtime.getRuntime().availableProcessors()*10;
	// if a lot of messages are not needed any more, then GC should be allowed
	public static final int maxQueueLength=10000;
	// optimal: numberOfMessageExecutorThreads=Runtime.getRuntime().availableProcessors()
	//public static final int numberOfMessageExecutorThreads=Runtime.getRuntime().availableProcessors();
	public static final int numberOfMessageExecutorThreads=1;
	
	synchronized public static void processEvent(BasicEvent event){
		SimulationParameters.events.processEvent(event);
	}
	
	public static void bufferEvent(BasicEvent event){
		eventBuffer.add(event);
	}
	
	public static void processEventBuffer(){
		BasicEvent be=eventBuffer.remove();
		while (be!=null){
			SimulationParameters.events.processEvent(be);
			be=eventBuffer.remove();
		}
	}
	
	
	
	public static double sumXCoordinate=0;
	public static double sumXCoordinateLeft=0;
	public static double sumXCoordinateRight=0;
	public static double noOfCarsLeft=0;
	public static double noOfCarsRight=0;
	public static double noOfCars=0;
	public static ConcurrentListSPSC<BasicEvent> eventBuffer=new ConcurrentListSPSC<BasicEvent>(); 
	
}
