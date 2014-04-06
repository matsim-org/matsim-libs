/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.siouxFalls.simulationInputs;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

import playground.vsp.emissions.types.HbefaVehicleAttributes;
import playground.vsp.emissions.types.HbefaVehicleCategory;

/**
 * @author amit after benjamin
 *
 */
public class EmissionVehicleGeneration {

	private static final Logger log = Logger.getLogger(EmissionVehicleGeneration.class);

	private final String populationFile = "./input/baseCase/SiouxFalls_population_probably_v3.xml";//"./input/output_plans.xml";
	private final String outputVehicleFile = "./input/emissionFiles/SiouxFalls_emissionVehicles.xml"; 

	//	private final String simplifiedNetworkFile = "./input/output_simplifiedNetwork.xml"; 
	private final String networkWithRoadType = "./input/baseCase/SiouxFalls_networkWithRoadType.xml.gz";//"./input/output_networkWithRoadType.xml.gz";
	Scenario scenario;

	public static void main(String[] args) {
		//		NetworkSimplifier networkSimplifier = new NetworkSimplifier();
		//		networkSimplifier.getSimplifiedNetwork("./input/output_networkWithRoadType.xml.gz", "./input/output_simplifiedNetwork.xml");

		EmissionVehicleGeneration evg = new EmissionVehicleGeneration();
		evg.run();
	}

	private void run() {
		Config config = ConfigUtils.createConfig();
		config.plans().setInputFile(populationFile);
		config.network().setInputFile(networkWithRoadType);
		this.scenario = ScenarioUtils.loadScenario(config);

		Vehicles outputVehicles = VehicleUtils.createVehiclesContainer();

		HbefaVehicleCategory vehicleCategory;
		HbefaVehicleAttributes vehicleAttributes;

		for(Person person : this.scenario.getPopulation().getPersons().values()){
			Id personId = person.getId();

			if(personId.toString().startsWith("gv")){
				vehicleCategory = HbefaVehicleCategory.HEAVY_GOODS_VEHICLE;
			} else {
				vehicleCategory = HbefaVehicleCategory.PASSENGER_CAR;
			}
			vehicleAttributes = new HbefaVehicleAttributes();

			Id vehTypeId = new IdImpl(vehicleCategory + ";" + 
					vehicleAttributes.getHbefaTechnology() + ";" + 
					vehicleAttributes.getHbefaSizeClass() + ";" + 
					vehicleAttributes.getHbefaEmConcept());
			VehicleType vehicleType = VehicleUtils.getFactory().createVehicleType(vehTypeId);
			if(!(outputVehicles.getVehicleTypes().containsKey(vehTypeId))){//getVehicles().containsKey(vehTypeId))){
				outputVehicles.addVehicleType(vehicleType);
			} else {
				// do nothing
			}

			Vehicle vehicle = VehicleUtils.getFactory().createVehicle(personId, vehicleType);
			outputVehicles.addVehicle(vehicle);//getVehicles().put(vehicle.getId(), vehicle);
		}

		VehicleWriterV1 vehicleWriter = new VehicleWriterV1(outputVehicles);
		vehicleWriter.writeFile(outputVehicleFile);
		log.info("Writing emission Vehicles files is finished.");
	}
}
