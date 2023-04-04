package org.matsim.contrib.ev.stats;


import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.common.timeprofile.TimeProfileCollector;
import org.matsim.contrib.ev.charging.ChargingEndEvent;
import org.matsim.contrib.ev.charging.ChargingEndEventHandler;
import org.matsim.contrib.ev.charging.ChargingStartEvent;
import org.matsim.contrib.ev.charging.ChargingStartEventHandler;
import org.matsim.contrib.ev.fleet.ElectricFleet;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.events.MobsimScopeEventHandler;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.contrib.common.timeprofile.TimeProfileCollector.ProfileCalculator;
import org.matsim.contrib.common.timeprofile.TimeProfiles;
import org.matsim.vehicles.Vehicle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;


public class ChargerPowerTimeProfileCollectorProvider implements Provider<MobsimListener>, ChargingStartEventHandler,ChargingEndEventHandler,
		MobsimScopeEventHandler {

	private final ElectricFleet evFleet;
	private final ChargingInfrastructure chargingInfrastructure;
	private final MatsimServices matsimServices;

	@Inject
	public ChargerPowerTimeProfileCollectorProvider(ElectricFleet evFleet, ChargingInfrastructure chargingInfrastructure, MatsimServices matsimServices) {
		this.evFleet = evFleet;
		this.chargingInfrastructure = chargingInfrastructure;
		this.matsimServices = matsimServices;
	}

	private static final Map<Id<Charger>,Double> chargerEnergy = new HashMap<>();

	private static final Map<Id<Charger>, List<Id<Vehicle>>> vehiclesAtCharger = new HashMap<>();
	private static final Map<Id<Vehicle>, Double> vehiclesEnergyPreviousTimeStep = new HashMap<>();

	public static ProfileCalculator createChargerEnergyCalculator (final ChargingInfrastructure chargingInfrastructure) {
		List<Charger> allChargers = new ArrayList<>(chargingInfrastructure.getChargers().values());

		ImmutableList<String> header = allChargers.stream().map(charger -> charger.getId() + "").collect(toImmutableList());

		return TimeProfiles.createProfileCalculator(header, () -> allChargers.stream()
				.collect(toImmutableMap(charger -> charger.getId() + "",
						charger -> chargerEnergy.getOrDefault(charger.getId(), 0.0)
				)));
	}

	@Override
	public MobsimListener get() {
		ProfileCalculator calc = createChargerEnergyCalculator(chargingInfrastructure);
		return new TimeProfileCollector(calc,300,"individual_chargers_charge_time_profiles",matsimServices);
	}
	@Override
	public void handleEvent(ChargingStartEvent event) {
		vehiclesEnergyPreviousTimeStep.put(event.getVehicleId(), event.getCharge());
		List<Id<Vehicle>> presentVehicles = vehiclesAtCharger.get(event.getChargerId());
		if (presentVehicles == null) {
			ArrayList<Id<Vehicle>> firstVehicle = new ArrayList<>();
			firstVehicle.add(event.getVehicleId());
			vehiclesAtCharger.put(event.getChargerId(),firstVehicle);
		} else {
			presentVehicles.add(event.getVehicleId());
			vehiclesAtCharger.put(event.getChargerId(),presentVehicles);
		}
	}
	@Override
	public void handleEvent(ChargingEndEvent event) {
		List<Id<Vehicle>> presentVehicles = vehiclesAtCharger.get(event.getChargerId());
		presentVehicles.remove(event.getVehicleId());
		vehiclesEnergyPreviousTimeStep.remove(event.getVehicleId());
		vehiclesAtCharger.put(event.getChargerId(),presentVehicles);
	}






	/** Setters and getters to be able to communicate with EvMobsimListener*/
	public Map<Id<Charger>, List<Id<Vehicle>>> getVehiclesAtCharger(){
		return vehiclesAtCharger;
	}
	public ElectricFleet getEvFleet(){
		return evFleet;
	}
	public void setChargerEnergy(Id<Charger> chargerId, double energy){
		chargerEnergy.put(chargerId, energy);
	}

	public Map<Id<Vehicle>, Double> getVehiclesEnergyPreviousTimeStep() {
		return vehiclesEnergyPreviousTimeStep;
	}

	public void setVehiclesEnergyPreviousTimeStep(Id<Vehicle> vehicleId, double newEnergy)  {
		vehiclesEnergyPreviousTimeStep.put(vehicleId,newEnergy);
	}
}
