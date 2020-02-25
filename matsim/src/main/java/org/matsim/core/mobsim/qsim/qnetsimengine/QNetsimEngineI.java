package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.vehicles.Vehicle;

public interface QNetsimEngineI extends MobsimEngine, NetsimEngine {
	
	interface NetsimInternalInterface {
		QNetwork getNetsimNetwork();
		void arrangeNextAgentState(MobsimAgent pp);
		void letVehicleArrive(QVehicle veh);
	}


	/**
	 * Implements one simulation step, called from simulation framework
	 * @param time The current time in the simulation.
	 */
	void doSimStep(double time);

	int getNumberOfSimulatedLinks();

	int getNumberOfSimulatedNodes();

	VehicularDepartureHandler getDepartureHandler();

	Map<Id<Vehicle>, QVehicle> getVehicles();

	void printEngineRunTimes();
	
	NetsimInternalInterface getNetsimInternalInterface();

}