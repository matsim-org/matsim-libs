package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.vehicles.Vehicle;

/**
 * 
 * @author droeder@Senozon 
 *
 */
public interface QNetsimEngineI extends MobsimEngine, NetsimEngine {
	
	interface NetsimInternalInterface {
		QNetwork getNetsimNetwork();
		void arrangeNextAgentState(MobsimAgent pp);
		void letVehicleArrive(QVehicle veh);
	}


	void doSimStep(double time);

	int getNumberOfSimulatedLinks();

	int getNumberOfSimulatedNodes();

//	NetworkModeDepartureHandler getVehicularDepartureHandler();
	// get from injection

	Map<Id<Vehicle>, QVehicle> getVehicles();

	void printEngineRunTimes();
	
	NetsimInternalInterface getNetsimInternalInterface();

}
