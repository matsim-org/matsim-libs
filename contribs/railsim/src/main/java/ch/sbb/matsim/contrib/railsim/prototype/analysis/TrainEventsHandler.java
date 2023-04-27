package ch.sbb.matsim.contrib.railsim.prototype.analysis;

import ch.sbb.matsim.contrib.railsim.prototype.TrainEntersLink;
import ch.sbb.matsim.contrib.railsim.prototype.TrainEntersLinkEventHandler;
import ch.sbb.matsim.contrib.railsim.prototype.TrainLeavesLink;
import ch.sbb.matsim.contrib.railsim.prototype.TrainLeavesLinkEventHandler;
import ch.sbb.matsim.contrib.railsim.prototype.TrainPathEntersLink;
import ch.sbb.matsim.contrib.railsim.prototype.TrainPathEntersLinkEventHandler;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Ihab Kaddoura
 */
public class TrainEventsHandler implements TrainEntersLinkEventHandler, TrainLeavesLinkEventHandler, TrainPathEntersLinkEventHandler {

	private EventsManager events;

	Map<Id<Vehicle>, Double> vehId2lastTimeStep = new HashMap<>();

	public TrainEventsHandler(EventsManager events) {
		this.events = events;
	}

	@Override
	public void handleEvent(TrainPathEntersLink event) {
		Id<Vehicle> pathId = Id.createVehicleId(event.getVehicleId() + "_viz_path");

		if (isNewTimeStep(event.getTime(), pathId)) {
			this.events.processEvent(new LinkEnterEvent(event.getTime(), pathId, event.getLinkId()));
			this.vehId2lastTimeStep.put(pathId, event.getTime());
		}

	}

	@Override
	public void handleEvent(TrainLeavesLink event) {

		Id<Vehicle> trainId = Id.createVehicleId(event.getVehicleId() + "_viz_train");
		Id<Vehicle> pathId = Id.createVehicleId(event.getVehicleId() + "_viz_path");

		if (isNewTimeStep(event.getTime(), trainId)) {
			this.events.processEvent(new LinkLeaveEvent(event.getTime(), trainId, event.getLinkId()));
			this.vehId2lastTimeStep.put(trainId, event.getTime());
		}

		if (isNewTimeStep(event.getTime(), pathId)) {
			this.events.processEvent(new LinkLeaveEvent(event.getTime(), pathId, event.getLinkId()));
			this.vehId2lastTimeStep.put(pathId, event.getTime());

		}

	}

	private boolean isNewTimeStep(double time, Id<Vehicle> vehId) {
		if (this.vehId2lastTimeStep.get(vehId) == null) {
			return true;
		} else if (this.vehId2lastTimeStep.get(vehId) == time) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public void handleEvent(TrainEntersLink event) {
		Id<Vehicle> trainId = Id.createVehicleId(event.getVehicleId() + "_viz_train");

		if (isNewTimeStep(event.getTime(), trainId)) {
			this.events.processEvent(new LinkEnterEvent(event.getTime(), trainId, event.getLinkId()));
			this.vehId2lastTimeStep.put(trainId, event.getTime());
		}
	}

}
