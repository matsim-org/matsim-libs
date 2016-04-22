package org.matsim.contrib.transEnergySim.events;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.transEnergySim.chargingInfrastructure.stationary.ChargingPlug;
import org.matsim.contrib.transEnergySim.vehicles.api.Vehicle;

public abstract class EVSimEngineEventHandler extends EventManager<PluginEventHandler> {

	// 'input methods
	public abstract void handleVehicleDepartureEvent(double time, Id<Vehicle> vehicleId);

	public abstract void handleVehicleArrivalEvent(double time, Id<Vehicle> vehicleId);

	public abstract void handleTimeStep(double time);

	// 'output' methods
	public void processPlugVehicleEvent(double time, Id<Vehicle> vehicleId, Id<ChargingPlug> plugId) {
		for (PluginEventHandler handler : handlers) {
			handler.handlePluginEvent(time, vehicleId, plugId);
		}
	}

	public void processUnPlugVehicleEvent(double time, Id<Vehicle> vehicleId, Id<ChargingPlug> plugId){
		for (PluginEventHandler handler : handlers) {
			handler.handleUnplugEvent(time, vehicleId, plugId);
		}
	}
	
	public void processTimeStep(double time){
		for (PluginEventHandler handler : handlers) {
			handler.handleTimeStep(time);
		}
	}

}
