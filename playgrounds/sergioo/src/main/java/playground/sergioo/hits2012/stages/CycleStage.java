package playground.sergioo.hits2012.stages;

import playground.sergioo.hits2012.Stage;

public class CycleStage extends Stage {

	private final String type;

	public CycleStage(String id, String mode, double walkTime, double inVehicleTime,
			double lastWalkTime, String type) {
		super(id, mode, walkTime, inVehicleTime, lastWalkTime);
		this.type = type;
	}

	public String getType() {
		return type;
	}
	
}
