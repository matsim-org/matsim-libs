package org.matsim.contrib.taxi.optimizer.rules;

import org.apache.log4j.Logger;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.taxi.passenger.TaxiRequest;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.core.mobsim.framework.MobsimTimer;

import java.util.ArrayList;
import java.util.List;

public class DriverConfirmationRegistry {
	private static final Logger log = Logger.getLogger(DriverConfirmationRegistry.class);
	private final TaxiConfigGroup taxiCfg;
	private final MobsimTimer timer;

	// TODO(CTudorache): performance
	private final List<DriverConfirmation> confirmations = new ArrayList<>();

	public DriverConfirmationRegistry(TaxiConfigGroup taxiCfg, MobsimTimer timer) {
		this.taxiCfg = taxiCfg;
		this.timer = timer;
	}

	public void addDriverConfirmation(TaxiRequest request, DvrpVehicle vehicle, VrpPathWithTravelData pathToPickup) {
		DriverConfirmation dc = new DriverConfirmation(request, vehicle, pathToPickup, timer.getTimeOfDay() + taxiCfg.getRequestAcceptanceDelay());
		log.warn("CTudorache addDriverConfirmation: " + dc);
		confirmations.add(dc);
	}

	public void removeDriverConfirmation(TaxiRequest req) {
		DriverConfirmation dc = getDriverConfirmation(req);
		if (dc != null) {
			removeDriverConfirmation(dc);
		}
	}

	public void removeDriverConfirmation(DriverConfirmation dc) {
		log.warn("CTudorache removeDriverConfirmation: " + dc);
		confirmations.remove(dc);
	}

	public DriverConfirmation getDriverConfirmation(TaxiRequest req) {
		for (var dc : confirmations) {
			if (dc.request == req) {
				return dc;
			}
		}
		return null;
	}
	public DriverConfirmation getDriverConfirmation(DvrpVehicle v) {
		for (var dc : confirmations) {
			if (dc.vehicle == v) {
				return dc;
			}
		}
		return null;
	}

	public boolean isWaitingDriverConfirmation(DvrpVehicle v) {
		return getDriverConfirmation(v) != null;
	}

	// set decision for those that are due
	public void updateForCurrentTime() {
		double now = timer.getTimeOfDay();
		for (DriverConfirmation dc : confirmations) {
			if (!dc.isComplete() && dc.endTime <= now) {
				dc.setComplete(true); // auto-accept
				log.warn("CTudorache DriverConfirmation complete: " + dc);
			}
		}
	}
}
