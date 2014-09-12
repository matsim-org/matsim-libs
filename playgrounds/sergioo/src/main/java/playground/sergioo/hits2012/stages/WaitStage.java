package playground.sergioo.hits2012.stages;

import playground.sergioo.hits2012.Stage;

public class WaitStage extends Stage {

	private final double waitTime;
	private final String type;

	public WaitStage(String id, String mode, double walkTime, double inVehicleTime,
			double lastWalkTime, double waitTime, String type) {
		super(id, mode, walkTime, inVehicleTime, lastWalkTime);
		this.waitTime = waitTime;
		this.type = type;
	}

}
