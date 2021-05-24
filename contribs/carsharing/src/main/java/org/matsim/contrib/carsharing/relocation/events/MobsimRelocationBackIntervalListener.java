package org.matsim.contrib.carsharing.relocation.events;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyInterface;
import org.matsim.contrib.carsharing.manager.supply.FreeFloatingVehiclesContainer;
import org.matsim.contrib.carsharing.relocation.demand.CarsharingVehicleRelocationContainer;
import org.matsim.contrib.carsharing.relocation.events.handlers.DemandDistributionHandler;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.qsim.QSim;

import com.google.inject.Inject;

public class MobsimRelocationBackIntervalListener implements MobsimBeforeSimStepListener, MobsimAfterSimStepListener {
	public static final Logger log = Logger.getLogger("dummy");

	@Inject private CarsharingSupplyInterface carsharingSupply;

	@Inject private DemandDistributionHandler demandDistributionHandler;

	@Inject private CarsharingVehicleRelocationContainer carsharingVehicleRelocation;

	@Inject EventsManager eventsManager;

	@SuppressWarnings("rawtypes")
	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent event) {
		QSim qSim = (QSim) event.getQueueSimulation();

		Double start = new Double(Math.floor(qSim.getSimTimer().getTimeOfDay()));
		Double end;
		Double previous;

		// relocation times will only be called if there are activities (usually starting with 32400.0), which makes sense
		for (Entry<String, List<Double>> entry : this.carsharingVehicleRelocation.getRelocationTimes().entrySet()) {
			String companyId = entry.getKey();
			List<Double> relocationTimes = entry.getValue();

			if (relocationTimes.contains(start)) {
				int index = relocationTimes.indexOf(start);

				try {
					previous = relocationTimes.get(index - 1);
				} catch (IndexOutOfBoundsException e) {
					previous = null;
				}

				if (null != previous) {
					this.carsharingVehicleRelocation.storeStatus(companyId, previous);
				}

				try {
					end = relocationTimes.get(index + 1);
				} catch (IndexOutOfBoundsException e) {
					end = 86400.0;
				}

				log.info("time to relocate " + companyId + " vehicles: " + (Math.floor(qSim.getSimTimer().getTimeOfDay()) / 3600));

			//	this.demandDistributionHandler.createODMatrices(companyId, start);

			//	this.carsharingVehicleRelocation.resetRelocationZones(companyId);

				// count number of vehicles in car sharing relocation zones
				FreeFloatingVehiclesContainer vehiclesContainer = (FreeFloatingVehiclesContainer) this.carsharingSupply.getCompany(companyId).getVehicleContainer("freefloating");
				for (Entry<CSVehicle, Link> vehicleEntry : vehiclesContainer.getFfvehiclesMap().entrySet()) {
					ArrayList<String> IDs = new ArrayList<String>();
					IDs.add(vehicleEntry.getKey().getVehicleId());
					this.carsharingVehicleRelocation.addVehicles(companyId, vehicleEntry.getValue(), IDs);
				}

				eventsManager.processEvent(new DispatchRelocationsEvent(start, end, companyId));
			}
			else if (relocationTimes.contains(event.getSimulationTime() + 3599.0)) {

				this.demandDistributionHandler.createODMatrices(companyId, event.getSimulationTime() + 3599.0);

				this.carsharingVehicleRelocation.resetRelocationZones(companyId);
			}
		}
	}

	@Override
	public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {
					
	}
}
