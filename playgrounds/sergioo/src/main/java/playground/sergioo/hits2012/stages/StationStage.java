package playground.sergioo.hits2012.stages;

import java.util.HashSet;
import java.util.Set;

public class StationStage extends WaitStage {

	public static final Set<String> STATIONS = new HashSet<String>();
	
	private final String startStation;
	private final String endStation;

	public StationStage(String id, String mode, double walkTime, double inVehicleTime,
			double lastWalkTime, double waitTime,
			String startStation, String endStation) {
		super(id, mode, walkTime, inVehicleTime, lastWalkTime, waitTime, "LRT");
		this.startStation = startStation;
		this.endStation = endStation;
	}

	public String getStartStation() {
		return startStation;
	}

	public String getEndStation() {
		return endStation;
	}

}
