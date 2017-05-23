/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura.integrationCNE.prepare;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.types.HbefaVehicleAttributes;
import org.matsim.contrib.emissions.types.HbefaVehicleCategory;
import org.matsim.contrib.emissions.utils.EmissionSpecificationMarker;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

/**
* @author ikaddoura
*/

public class EmissionVehicleGenerator {

	private final String populationFile = "/Users/ihab/Desktop/ils4/kaddoura/cne/berlin-dz-1pct-simpleNetwork/input/be_117j.output_plans_selected.xml.gz";
	private final String outputVehicleFile = "/Users/ihab/Desktop/ils4/kaddoura/cne/berlin-dz-1pct-simpleNetwork/input/be_117j.output_plans_selected_vehicles.xml";
	
	public static void main(String[] args) {
		EmissionVehicleGenerator generator = new EmissionVehicleGenerator();
		generator.run();		
	}
  
	private void run() {
		
		Config config = ConfigUtils.createConfig();
		config.plans().setInputFile(populationFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);

		Vehicles outputVehicles = VehicleUtils.createVehiclesContainer();

		for (Person person : scenario.getPopulation().getPersons().values()){
			
			Id<Person> personId = person.getId();
			Id<Vehicle> vehicleId = Id.create(personId, Vehicle.class);

			HbefaVehicleCategory vehicleCategory = HbefaVehicleCategory.PASSENGER_CAR;
			HbefaVehicleAttributes vehicleAttributes = new HbefaVehicleAttributes();
			Id<VehicleType> vehTypeId = Id.create(vehicleCategory + ";" + 
					vehicleAttributes.getHbefaTechnology() + ";" + 
					vehicleAttributes.getHbefaSizeClass() + ";" + 
					vehicleAttributes.getHbefaEmConcept(), VehicleType.class);
			VehicleType vehicleType = VehicleUtils.getFactory().createVehicleType(vehTypeId);
			
			vehicleType.setDescription(EmissionSpecificationMarker.BEGIN_EMISSIONS + vehTypeId.toString() + EmissionSpecificationMarker.END_EMISSIONS);
			
			if(!(outputVehicles.getVehicleTypes().containsKey(vehTypeId))){
				outputVehicles.addVehicleType(vehicleType);
			} else {
				// do nothing
			}			
			
			Vehicle vehicle = VehicleUtils.getFactory().createVehicle(vehicleId, vehicleType);
			outputVehicles.addVehicle(vehicle);
		}
		
		VehicleWriterV1 vehicleWriter = new VehicleWriterV1(outputVehicles);
		vehicleWriter.writeFile(outputVehicleFile);
	}

}

