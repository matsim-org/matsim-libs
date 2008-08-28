package playground.wrashid.PDES2;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.matsim.events.BasicEvent;
import org.matsim.events.Events;

import playground.wrashid.PDES.util.ConcurrentListMPDSC;
import playground.wrashid.PDES.util.ConcurrentListMPSC;
import playground.wrashid.PDES.util.PriorityConcurrentListMPDSC;

public class SimulationParameters {
	// EventHeap
	
	static double maxSimulationLength=Double.MAX_VALUE/2; //in s 
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
	public static BasicEvent temp_be=null;
	
	
	
	
	public static final boolean debugMode=false;
	public static final double maxAbsLinkAverage=0.01; // how far can the average usage of links differ for a test to pass


	public static Events events=null; // this is the simulation events object
	public static double stuckTime=Double.MAX_VALUE; // - this time is used for deadlock prevention.
														   // - when a car waits for more than 'stuckTime' for 
														   //   entering next road, it will enter the next 
	
	
	public static double delta=0.001; // this delta is used to correct any wrong messages (e.g. if a null message is sent, that there
	// cannot be any vehicle until time x. And exactly at time x there is a vehicle, then we could have a problem in the simulation.
	// For this reason, this delta is introduced: All null messages are corrected by this delta value;
	// especially at the beginning of the simulation this is possible, as all roads have a null message (and possibly some enterRequest messages).
	

	
	
	// simulation internal parameters
	
	// specifies the minimal queue length for using in all message static constructers
    // this parameter needs to be set to avoid race conditions
	public static final int minQueueLength=Runtime.getRuntime().availableProcessors()*10;
	// if a lot of messages are not needed any more, then GC should be allowed
	public static final int maxQueueLength=10000;
	// optimal: numberOfMessageExecutorThreads=Runtime.getRuntime().availableProcessors()
	//public static final int numberOfMessageExecutorThreads=Runtime.getRuntime().availableProcessors();
	public static final int numberOfMessageExecutorThreads=Runtime.getRuntime().availableProcessors();
	
	// the number of zones, in which the network is divided
	//public static final int numberOfZones=numberOfMessageExecutorThreads;
	// don't change this anymore
	// numberOfZones must be equal to numberOfMessageExecutorThreads
	public static final int numberOfZones=numberOfMessageExecutorThreads;
	
	public static double minXCoodrinate=Double.MAX_VALUE;
	public static double maxXCoodrinate=Double.MIN_VALUE;
	public static double xZoneDistance=0;
	public static double zoneBorderLines[]=new double[numberOfZones-1];
	// keep the number of zone buckets big, because else the zones have different
	// number of events in each zone not equal (ca. 5000)
	// need to be high, because else a problem with JavaPDEQSim2.maxEventsPerBucket may occur
	public static int numberOfZoneBuckets=5000;
	
	public static void processEvent(BasicEvent event){
		SimulationParameters.events.processEvent(event);
	}
	
	public static void bufferEvent(BasicEvent event){
		//if (temp_be==null){
		//	temp_be=event;
		//} else {
		//	if (test_timer % 100000==0){
		//		bufferEvent(temp_be,MessageExecutor.getThreadId());
		//		temp_be=null;
		//	}
			bufferEvent(event,MessageExecutor.getThreadId());
		//}	
		//test_timer++;
		
		
		
		//test_timer++;
		//if (test_timer % 100000==0){System.out.println(test_timer);}
	}
	
	private static void bufferEvent(BasicEvent event, int producerId){
		eventBuffer.add(event, producerId);
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
	public static PriorityConcurrentListMPDSC eventBuffer=new PriorityConcurrentListMPDSC(SimulationParameters.numberOfMessageExecutorThreads,1000,1000000); 
	
	
	
	public static double test_timer=0;
	
}
