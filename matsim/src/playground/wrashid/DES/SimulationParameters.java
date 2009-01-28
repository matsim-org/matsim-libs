package playground.wrashid.DES;

import java.util.HashMap;
import org.matsim.events.Events;
import playground.wrashid.DES.util.testable.PopulationModifier;
import playground.wrashid.DES.util.testable.TestHandler;

public class SimulationParameters {

	// CONSTANTS
	public static final String START_LEG = "start leg";
	public static final String END_LEG = "end leg";
	public static final String ENTER_LINK = "enter link";
	public static final String LEAVE_LINK = "leave link";
	// the priorities of the messages.
	// a higher priority comes first in the message queue (when same time)
	// usage: for example a person has a enter road message at the same time as
	// leaving the previous road (need to keep the messages in right order)
	// for events with same time stamp: leave < arrival < departure < enter
	// especially for testing this is important
	public static final int PRIORITY_LEAVE_ROAD_MESSAGE = 200;
	public static final int PRIORITY_ARRIVAL_MESSAGE = 150;
	public static final int PRIORITY_DEPARTUARE_MESSAGE = 125;
	public static final int PRIORITY_ENTER_ROAD_MESSAGE = 100;

	// INPUT
	public static long maxSimulationLength = 800000; // in s
	public static long linkCapacityPeriod = 0; // 
	public static double gapTravelSpeed = 15.0; // in m/s
	public static double flowCapacityFactor = 1.0;
	public static double storageCapacityFactor = 1.0;
	public static double carSize = 7.5; // in meter
	// in [vehicles/hour] per lane, can be scaled with flow capacity factor
	public static double minimumInFlowCapacity = 1800;
	// stuckTime is used for deadlock prevention.
	// when a car waits for more than 'stuckTime' for entering next road, it
	// will enter the next
	public static double stuckTime = Double.MAX_VALUE;
	// this must be initialized before starting the simulation!
	// mapping: key=linkId
	// used to find a road corresponding to a link
	public static HashMap<String, Road> allRoads = null;

	// SETTINGS
	// should garbage collection of messages be activated
	private static boolean GC_MESSAGES = false;

	// OUTPUT
	// this is the simulation events object
	//public static Events processEventThread = null;
	// The thread for processing the events
	public static Events processEventThread = null;

	// TESTING
	// test injection variables
	public static TestHandler testEventHandler = null;
	public static String testPlanPath = null;
	public static PopulationModifier testPopulationModifier = null;
	// how far can the average usage of links differ for a unit test to pass
	// in percent
	public static final double maxAbsLinkAverage = 0.01;

	// METHODS
	public static boolean isGC_MESSAGES() {
		return GC_MESSAGES;
	}

	public static void setGC_MESSAGES(boolean gc_messages) {
		GC_MESSAGES = gc_messages;
	}
	



}
