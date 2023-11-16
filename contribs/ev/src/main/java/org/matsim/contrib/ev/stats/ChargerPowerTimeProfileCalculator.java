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

public final class ChargerPowerTimeProfileCalculator implements ChargingStartEventHandler, ChargingEndEventHandler {

	private final Map<Id<Charger>, double[]> chargerProfiles = new HashMap<>();
	private final Map<Id<Vehicle>, Double> chargingStartTimeMap = new HashMap<>();
	private final Map<Id<Vehicle>, Double> chargingStartEnergyMap = new HashMap<>();

	private final TimeDiscretizer timeDiscretizer;
	private final double qsimEndTime;

	/**
	 * Calculation of average power output for each charging station for each charging event. Charging stations without any charging events will not
	 * be present in the output file. Implementation does only work when the Qsim end time is defined in the config, i.e., will not work for
	 * extensions drt and taxi or others depending on the ev contrib without a defined Qsim end time.
	 * @author mattiasingelstrom
	 */
	@Inject
	ChargerPowerTimeProfileCalculator(Config config) {
		int chargeTimeStep = ConfigUtils.addOrGetModule(config, EvConfigGroup.class).chargeTimeStep;
		qsimEndTime = ConfigUtils.addOrGetModule(config, QSimConfigGroup.class).getEndTime().orElse(0.0);
		timeDiscretizer = new TimeDiscretizer((int)Math.ceil(qsimEndTime), chargeTimeStep);
	}


	public Map<Id<Charger>, double[]> getChargerProfiles() {
		return chargerProfiles;
	}

	@Override
	public void handleEvent(ChargingStartEvent event) {
		chargingStartTimeMap.put(event.getVehicleId(), event.getTime());
		chargingStartEnergyMap.put(event.getVehicleId(), EvUnits.J_to_kWh(event.getCharge()));
	}

	@Override

	public void handleEvent(ChargingEndEvent event) {
		double chargingTimeIn_h = (event.getTime() - chargingStartTimeMap.get(event.getVehicleId())) / 3600.0;
		double averagePowerIn_kW = (EvUnits.J_to_kWh(event.getCharge()) - chargingStartEnergyMap.get(event.getVehicleId())) / chargingTimeIn_h;
		increment(averagePowerIn_kW, event.getChargerId(), chargingStartTimeMap.get(event.getVehicleId()), event.getTime());
	}
	private void increment(double averagePower, Id<Charger> chargerId, double chargingStartTime, double chargingEndTime) {

		 //If Qsim end time is undefined in config, qsimEndTime will be 0.0 and will therefore not proceed in calculating the power curves.
		if (chargingStartTime == chargingEndTime || chargingStartTime >= qsimEndTime || qsimEndTime == 0.0) {
			return;
		}
		chargingEndTime = Math.min(chargingEndTime, qsimEndTime);

		int fromIdx = timeDiscretizer.getIdx(chargingStartTime);
		int toIdx = timeDiscretizer.getIdx(chargingEndTime);

		for (int i = fromIdx; i < toIdx; i++) {
			double[] chargingVector = chargerProfiles.computeIfAbsent(chargerId, c -> new double[timeDiscretizer.getIntervalCount()]);
			chargingVector[i] += averagePower;
		}
	}

	public TimeDiscretizer getTimeDiscretizer() {
		return timeDiscretizer;
	}
}
