package playground.mrieser.svi.data.vehtrajectories;

import java.util.ArrayList;
import java.util.List;

/**
 * Allows to process a {@link VehicleTrajectory} by more than one {@link VehicleTrajectoryHandler handler}.
 * 
 * @author mrieser
 */
public class MultipleVehicleTrajectoryHandler implements VehicleTrajectoryHandler {

	private final List<VehicleTrajectoryHandler> handlers = new ArrayList<VehicleTrajectoryHandler>();
	
	public void addTrajectoryHandler(final VehicleTrajectoryHandler handler) {
		this.handlers.add(handler);
	}
	
	@Override
	public void handleVehicleTrajectory(VehicleTrajectory trajectory) {
		for (VehicleTrajectoryHandler handler : this.handlers) {
			handler.handleVehicleTrajectory(trajectory);
		}
	}

}
