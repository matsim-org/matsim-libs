package org.matsim.contrib.ev.stats;


import com.google.inject.Inject;
import com.google.inject.Provider;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.common.timeprofile.TimeDiscretizer;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.EvUnits;
import org.matsim.contrib.ev.charging.ChargingEndEvent;
import org.matsim.contrib.ev.charging.ChargingEndEventHandler;
import org.matsim.contrib.ev.charging.ChargingStartEvent;
import org.matsim.contrib.ev.charging.ChargingStartEventHandler;
import org.matsim.contrib.ev.fleet.ElectricFleet;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.events.MobsimScopeEventHandler;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.vehicles.Vehicle;

import java.util.*;

import static com.google.common.collect.ImmutableMap.toImmutableMap;


public class ChargerPowerTimeProfileCalculator implements  ChargingStartEventHandler,ChargingEndEventHandler
		  {

	private Map<Id<Charger>, double[]> chargerProfiles = new HashMap<>();
	private Map<Id<Vehicle>,Double> chargingStartTime = new HashMap<>();
	private Map<Id<Vehicle>,Double> chargingStartEnergy = new HashMap<>();

	private final TimeDiscretizer timeDiscretizer;
	private final double qsimEndTime;
	private final int chargeTimeStep;

	@Inject
	public ChargerPowerTimeProfileCalculator(Config config) {
		chargeTimeStep = ConfigUtils.addOrGetModule(config, EvConfigGroup.class).chargeTimeStep;
		qsimEndTime = ConfigUtils.addOrGetModule(config, QSimConfigGroup.class).getEndTime().orElse(0.0);
		timeDiscretizer = new TimeDiscretizer((int)Math.ceil(qsimEndTime), chargeTimeStep);
	}

	private static final Map<Id<Charger>, List<Id<Vehicle>>> vehiclesAtCharger = new HashMap<>();
	private static final Map<Id<Vehicle>, Double> vehiclesEnergyPreviousTimeStep = new HashMap<>();

	private void normalizeProfile(double[] profile) {
		for (int i = 0; i < profile.length; i++) {
			profile[i] /= timeDiscretizer.getTimeInterval();
		}
	}
	public Map<Id<Charger>, double[]> getChargerProfiles() {
//		Map<Id<Charger>,double[]> chargerProfilesArray = new HashMap<>();
//		this.chargerProfiles.forEach((chargerId, doubles) -> {
//			double[] doubleArray = doubles.stream().mapToDouble(Double::doubleValue).toArray();
//			chargerProfilesArray.put(chargerId,doubleArray);
//		});
		chargerProfiles.values().forEach(this::normalizeProfile);
		return chargerProfiles;
	}

	@Override
	public void handleEvent(ChargingStartEvent event) {
//		vehiclesEnergyPreviousTimeStep.put(event.getVehicleId(), event.getCharge());
//		List<Id<Vehicle>> presentVehicles = vehiclesAtCharger.get(event.getChargerId());
//		if (presentVehicles == null) {
//			ArrayList<Id<Vehicle>> firstVehicle = new ArrayList<>();
//			firstVehicle.add(event.getVehicleId());
//			vehiclesAtCharger.put(event.getChargerId(), firstVehicle);
//		} else {
//			presentVehicles.add(event.getVehicleId());
//			vehiclesAtCharger.put(event.getChargerId(), presentVehicles);
//		}
		chargingStartTime.put(event.getVehicleId(),event.getTime());
		chargingStartEnergy.put(event.getVehicleId(),EvUnits.J_to_kWh(event.getCharge()));
	}

	@Override
	public void handleEvent(ChargingEndEvent event) {
//		List<Id<Vehicle>> presentVehicles = vehiclesAtCharger.get(event.getChargerId());
//		presentVehicles.remove(event.getVehicleId());
//		vehiclesEnergyPreviousTimeStep.remove(event.getVehicleId());
//		vehiclesAtCharger.put(event.getChargerId(), presentVehicles);
		double chargingTimeIn_h = (event.getTime() - chargingStartTime.get(event.getVehicleId()))/3600.0;
		double averagePowerIn_kW = (EvUnits.J_to_kWh(event.getCharge())-chargingStartEnergy.get(event.getVehicleId()))/chargingTimeIn_h;
		increment(averagePowerIn_kW,event.getChargerId(),chargingStartTime.get(event.getVehicleId()),event.getTime());
	}
	private void increment(double averagePower,Id<Charger> chargerId, double beginTime, double endTime){
		if (beginTime == endTime && beginTime >= qsimEndTime) {
			return;
		}
		endTime = Math.min(endTime, qsimEndTime);

		int fromIdx = timeDiscretizer.getIdx(beginTime);
		int toIdx = timeDiscretizer.getIdx(endTime);

		for (int i = fromIdx; i < toIdx; i++) {
			double[] chargingVector = chargerProfiles.get(chargerId);
			chargingVector[i] += averagePower;
			chargerProfiles.put(chargerId,chargingVector);
		}
	}

	public TimeDiscretizer getTimeDiscretizer() {
		return timeDiscretizer;
	}
}
