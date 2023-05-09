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


public class ChargerPowerTimeProfileCalculator implements  ChargingStartEventHandler,ChargingEndEventHandler,
		 MobsimAfterSimStepListener {

	private Map<Id<Charger>, List<Double>> chargerProfiles;
	private final TimeDiscretizer timeDiscretizer;
	private final ElectricFleet evFleet;
	private final ChargingInfrastructure chargingInfrastructure;
	private final MatsimServices matsimServices;
	private final int chargeTimeStep;

	@Inject
	public ChargerPowerTimeProfileCalculator(ElectricFleet evFleet, ChargingInfrastructure chargingInfrastructure,
											 MatsimServices matsimServices, Config config) {
		this.evFleet = evFleet;
		this.chargingInfrastructure = chargingInfrastructure;
		this.matsimServices = matsimServices;
		chargeTimeStep = ConfigUtils.addOrGetModule(config, EvConfigGroup.class).chargeTimeStep;
		double qsimEndTime = ConfigUtils.addOrGetModule(config, QSimConfigGroup.class).getEndTime().orElse(0.0);
		timeDiscretizer = new TimeDiscretizer((int)Math.ceil(qsimEndTime), chargeTimeStep);
	}

	private static final Map<Id<Charger>, Double> chargerEnergy = new HashMap<>();

	private static final Map<Id<Charger>, List<Id<Vehicle>>> vehiclesAtCharger = new HashMap<>();
	private static final Map<Id<Vehicle>, Double> vehiclesEnergyPreviousTimeStep = new HashMap<>();

	//public static ProfileCalculator createChargerEnergyCalculator(final ChargingInfrastructure chargingInfrastructure) {
	//	List<Charger> allChargers = new ArrayList<>(chargingInfrastructure.getChargers().values());

	//	ImmutableList<String> header = allChargers.stream().map(charger -> charger.getId() + "").collect(toImmutableList());

	//	return TimeProfiles.createProfileCalculator(header, () -> allChargers.stream()
	//			.collect(toImmutableMap(charger -> charger.getId() + "",
	//					charger -> chargerEnergy.getOrDefault(charger.getId(), 0.0)
	//			)));
	//}
	private void normalizeProfile(double[] profile) {
		for (int i = 0; i < profile.length; i++) {
			profile[i] /= timeDiscretizer.getTimeInterval();
		}
	}
	public Map<Id<Charger>, double[]> getChargerProfiles() {
		Map<Id<Charger>,double[]> chargerProfilesArray = new HashMap<>();
		this.chargerProfiles.forEach((chargerId, doubles) -> {
			double[] doubleArray = doubles.stream().mapToDouble(Double::doubleValue).toArray();
			chargerProfilesArray.put(chargerId,doubleArray);
		});
		chargerProfilesArray.values().forEach(this::normalizeProfile);
		return chargerProfilesArray;
	}
//	@Override
//	public MobsimListener get() {
//		ProfileCalculator calc = createChargerEnergyCalculator(chargingInfrastructure);
//		return new TimeProfileCollector(calc, 300, "individual_chargers_charge_time_profiles", matsimServices);
//	}

	@Override
	public void handleEvent(ChargingStartEvent event) {
		vehiclesEnergyPreviousTimeStep.put(event.getVehicleId(), event.getCharge());
		List<Id<Vehicle>> presentVehicles = vehiclesAtCharger.get(event.getChargerId());
		if (presentVehicles == null) {
			ArrayList<Id<Vehicle>> firstVehicle = new ArrayList<>();
			firstVehicle.add(event.getVehicleId());
			vehiclesAtCharger.put(event.getChargerId(), firstVehicle);
		} else {
			presentVehicles.add(event.getVehicleId());
			vehiclesAtCharger.put(event.getChargerId(), presentVehicles);
		}
	}

	@Override
	public void handleEvent(ChargingEndEvent event) {
		List<Id<Vehicle>> presentVehicles = vehiclesAtCharger.get(event.getChargerId());
		presentVehicles.remove(event.getVehicleId());
		vehiclesEnergyPreviousTimeStep.remove(event.getVehicleId());
		vehiclesAtCharger.put(event.getChargerId(), presentVehicles);
	}


	public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent event) {
		if ((event.getSimulationTime() + 1) % chargeTimeStep == 0) {
			vehiclesAtCharger.forEach((charger, vehicleList) -> {
				List<Double> previousValues = chargerProfiles.get(charger);
				if (!vehicleList.isEmpty()) {
					double energy = vehicleList.stream().mapToDouble(vehicleId -> EvUnits.J_to_kWh((Objects.requireNonNull(evFleet.getElectricVehicles().get(vehicleId)).getBattery()
							.getCharge() - vehiclesEnergyPreviousTimeStep.get(vehicleId)) * (3600.0 / chargeTimeStep))).sum();
					if (!Double.isNaN(energy) && !(energy == 0.0)) {
						previousValues.add(energy);
						chargerProfiles.put(charger, previousValues);
						vehicleList.forEach(vehicleId -> vehiclesEnergyPreviousTimeStep.put(vehicleId, Objects.requireNonNull(evFleet.getElectricVehicles().get(vehicleId)).getBattery().getCharge()));
					} else {
						previousValues.add(0.0);
						chargerProfiles.put(charger, previousValues);
					}
				} else {
					previousValues.add(0.0);
					chargerProfiles.put(charger,previousValues);
				}
			});
		}
	}

	public TimeDiscretizer getTimeDiscretizer() {
		return timeDiscretizer;
	}
}
