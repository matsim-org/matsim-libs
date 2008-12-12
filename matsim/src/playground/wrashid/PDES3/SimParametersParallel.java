package playground.wrashid.PDES3;

public class SimParametersParallel {

	public static final int numberOfMessageExecutorThreads = Runtime
			.getRuntime().availableProcessors();

	// TODO:
	// consider later also numberOfZones != numberOfMessageExecutorThreads, especially
	// useful, when parts of the day, there is lots of traffic in one area and in other parts
	// in the rest of the day.
	public static final int numberOfZones = numberOfMessageExecutorThreads;
	
	// TODO: also optimize for different times of the day
	
	// keep the number of zone buckets big, because else the zones have
	// different
	// number of events in each zone not equal (ca. 5000)
	// need to be high, because else a problem with
	// JavaPDEQSim2.maxEventsPerBucket may occur
	public static int numberOfZoneBuckets = 5000;

}
