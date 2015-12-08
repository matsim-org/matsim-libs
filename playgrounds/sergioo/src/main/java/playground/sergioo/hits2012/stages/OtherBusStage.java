package playground.sergioo.hits2012.stages;

public class OtherBusStage extends WaitStage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public OtherBusStage(String id, String mode, double walkTime, double inVehicleTime,
			double lastWalkTime, double waitTime, String type) {
		super(id, mode, walkTime, inVehicleTime, lastWalkTime, waitTime, type);
	}

}
