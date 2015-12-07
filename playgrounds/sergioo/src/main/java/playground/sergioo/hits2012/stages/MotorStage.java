package playground.sergioo.hits2012.stages;

import playground.sergioo.hits2012.Stage;

public class MotorStage extends Stage {

	private final int numPassegers;

	public MotorStage(String id, String mode, double walkTime, double inVehicleTime,
			double lastWalkTime, int numPassengers) {
		super(id, mode, walkTime, inVehicleTime, lastWalkTime);
		this.numPassegers = numPassengers;
	}

	public int getNumPassegers() {
		return numPassegers;
	}
	
}
