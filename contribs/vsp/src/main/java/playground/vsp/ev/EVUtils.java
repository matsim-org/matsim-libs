/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.vsp.ev;

import com.google.common.collect.ImmutableList;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.*;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class EVUtils {

	private static final String INITIALENERGY_KWH = "initialEnergyInKWh";
	private static final String CHARGERTYPES = "chargerTypes";

	/**
	 *
	 * @param engineInformation
	 * @return the initial energy in kWh
	 */
	static Double getInitialEnergy(EngineInformation engineInformation ){
		return (Double) engineInformation.getAttributes().getAttribute(INITIALENERGY_KWH);
	}

	/**
	 *
	 * @param engineInformation
	 * @param initialEnergyInKWh initial energy [kWh]
	 */
	public static void setInitialEnergy(EngineInformation engineInformation, double initialEnergyInKWh){
		engineInformation.getAttributes().putAttribute(INITIALENERGY_KWH,  initialEnergyInKWh);
	}

	static ImmutableList<String> getChargerTypes(EngineInformation engineInformation ){
		return ImmutableList.copyOf((Collection<String>) engineInformation.getAttributes().getAttribute( CHARGERTYPES));
	}

	public static void setChargerTypes(EngineInformation engineInformation, Collection<String> chargerTypes){
		engineInformation.getAttributes().putAttribute(CHARGERTYPES,  chargerTypes);
	}

	static void createAndRegisterEVForPersonsAndMode(Scenario scenario, Set<Id<Person>> persons, VehicleType eVehiclyType, String mode){
		if(! VehicleUtils.getHbefaTechnology(eVehiclyType.getEngineInformation()).equals("electricity")) throw new IllegalArgumentException();

		VehiclesFactory vFactory = scenario.getVehicles().getFactory();

		for (Id<Person> personId : persons) {
			Person person = scenario.getPopulation().getPersons().get(personId);
			Id<Vehicle> vehicleId = VehicleUtils.createVehicleId(person, mode);

			Vehicle vehicle = vFactory.createVehicle(vehicleId, eVehiclyType);
			scenario.getVehicles().addVehicle(vehicle);

			Map<String, Id<Vehicle>> mode2Vehicle = VehicleUtils.getVehicleIds(person);
			mode2Vehicle.put(mode, vehicleId);
			VehicleUtils.insertVehicleIdsIntoAttributes(person, mode2Vehicle);//probably unnecessary
		}

	}

}
