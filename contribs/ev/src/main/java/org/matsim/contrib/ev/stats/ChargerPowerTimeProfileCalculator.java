package org.matsim.contrib.ev.stats;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.common.timeprofile.TimeDiscretizer;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.EvUnits;
import org.matsim.contrib.ev.charging.ChargingEndEvent;
import org.matsim.contrib.ev.charging.ChargingEndEventHandler;
import org.matsim.contrib.ev.charging.ChargingStartEvent;
import org.matsim.contrib.ev.charging.ChargingStartEventHandler;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;

public class ChargerPowerTimeProfileCalculator implements ChargingStartEventHandler, ChargingEndEventHandler {

	private final Map<Id<Charger>, double[]> chargerProfiles = new HashMap<>();
	private final Map<Id<Vehicle>, Double> chargingStartTime = new HashMap<>();
	private final Map<Id<Vehicle>, Double> chargingStartEnergy = new HashMap<>();

	private final TimeDiscretizer timeDiscretizer;
	private final double qsimEndTime;

	@Inject
	public ChargerPowerTimeProfileCalculator(Config config) {
		int chargeTimeStep = ConfigUtils.addOrGetModule(config, EvConfigGroup.class).chargeTimeStep;
		qsimEndTime = ConfigUtils.addOrGetModule(config, QSimConfigGroup.class).getEndTime().orElse(0.0);
		timeDiscretizer = new TimeDiscretizer((int)Math.ceil(qsimEndTime), chargeTimeStep);
	}


	public Map<Id<Charger>, double[]> getChargerProfiles() {
		return chargerProfiles;
	}

	@Override
	public void handleEvent(ChargingStartEvent event) {
		chargingStartTime.put(event.getVehicleId(), event.getTime());
		chargingStartEnergy.put(event.getVehicleId(), EvUnits.J_to_kWh(event.getCharge()));
	}

	@Override
	public void handleEvent(ChargingEndEvent event) {
		double chargingTimeIn_h = (event.getTime() - chargingStartTime.get(event.getVehicleId())) / 3600.0;
		double averagePowerIn_kW = (EvUnits.J_to_kWh(event.getCharge()) - chargingStartEnergy.get(event.getVehicleId())) / chargingTimeIn_h;
		increment(averagePowerIn_kW, event.getChargerId(), chargingStartTime.get(event.getVehicleId()), event.getTime());
	}
	private void increment(double averagePower, Id<Charger> chargerId, double beginTime, double endTime) {
		if (beginTime == endTime && beginTime >= qsimEndTime) {
			return;
		}
		endTime = Math.min(endTime, qsimEndTime);

		int fromIdx = timeDiscretizer.getIdx(beginTime);
		int toIdx = timeDiscretizer.getIdx(endTime);

		for (int i = fromIdx; i < toIdx; i++) {
			double[] chargingVector = chargerProfiles.computeIfAbsent(chargerId, c -> new double[timeDiscretizer.getIntervalCount()]);
			chargingVector[i] += averagePower;
		}
	}

	public TimeDiscretizer getTimeDiscretizer() {
		return timeDiscretizer;
	}
}
