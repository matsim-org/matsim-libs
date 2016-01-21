package playground.sergioo.hits2012.stages;

import java.util.HashSet;
import java.util.Set;

public class PublicBusStage extends WaitStage {

	public static final Set<String> LINES = new HashSet<String>();
	
	private final String line;

	public PublicBusStage(String id, String mode, double walkTime, double inVehicleTime,
			double lastWalkTime, double waitTime,
			String line) {
		super(id, mode, walkTime, inVehicleTime, lastWalkTime, waitTime, "PUBLIC_BUS");
		this.line = line;
	}

	public String getLine() {
		return line;
	}

}
