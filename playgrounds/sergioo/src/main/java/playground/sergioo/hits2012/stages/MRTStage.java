package playground.sergioo.hits2012.stages;

import java.util.HashSet;
import java.util.Set;

public class MRTStage extends WaitStage {

	public static final Set<String> STATIONS = new HashSet<String>();
	
	private final String startStation;
	private final String endStation;
	private final String firstTransfer;
	private final String secondTransfer;
	private final String thirdTransfer;

	public MRTStage(String id, String mode, double walkTime, double inVehicleTime,
			double lastWalkTime, double waitTime, String startStation, String endStation,
			String firstTransfer, String secondTransfer, String thirdTransfer) {
		super(id, mode, walkTime, inVehicleTime, lastWalkTime, waitTime, "MRT");
		this.startStation = startStation;
		this.endStation = endStation;
		this.firstTransfer = firstTransfer;
		this.secondTransfer = secondTransfer;
		this.thirdTransfer = thirdTransfer;
	}

}
