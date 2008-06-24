package playground.wrashid.PDES;

public class SimulationParameters {
	// EventHeap
	
	static long simulationLength=Long.MAX_VALUE; //in ms
	public static long linkCapacityPeriod=0; // wird initializiert im JavaDEQSim.java
	public static double gapTravelSpeed=15.0; // in m/s
	public static double flowCapacityFactor=0.33;
	public static double storageCapacityFactor=0.33;
	public static double carSize=7.5; // in meter
	public static double minimumInFlowCapacity=1800; // diese konstante ist hardcodiert bei der C++ simulation (in Frz/stunde)

	
}
