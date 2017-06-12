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
import org.matsim.contrib.emissions.types.HbefaVehicleAttributes;
import org.matsim.contrib.emissions.types.HbefaVehicleCategory;
import org.matsim.contrib.emissions.utils.EmissionSpecificationMarker;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.*;

/**
 * @author amit after benjamin
 */
public class EmissionVehicleGenerator {


	private final String outputVehicleFile;
	private final Scenario scenario;

	public EmissionVehicleGenerator(Scenario scenario, String outputVehicleFile) {
		this.outputVehicleFile = outputVehicleFile;
		this.scenario = scenario;
	}

	private static final Logger LOG = Logger.getLogger(EmissionVehicleGenerator.class);

	public static void main(String[] args) {
		//		NetworkSimplifier networkSimplifier = new NetworkSimplifier();
		//		networkSimplifier.getSimplifiedNetwork("./input/output_networkWithRoadType.xml.gz", "./input/output_simplifiedNetwork.xml");
		String outputVehicleFile = "./input/emissionFiles/SiouxFalls_emissionVehicles.xml"; 
		String populationFile = "./input/baseCase/SiouxFalls_population_probably_v3.xml";//"./input/output_plans.xml";
		String networkWithRoadType = "./input/baseCase/SiouxFalls_networkWithRoadType.xml.gz";//"./input/output_networkWithRoadType.xml.gz";

		Config config = ConfigUtils.createConfig();
		config.plans().setInputFile(populationFile);
		config.network().setInputFile(networkWithRoadType);
		Scenario scenario = ScenarioUtils.loadScenario(config);


		EmissionVehicleGenerator evg = new EmissionVehicleGenerator(scenario, outputVehicleFile);
		evg.run();
	}

	public void run() {
		Vehicles outputVehicles = VehicleUtils.createVehiclesContainer();

		HbefaVehicleCategory vehicleCategory;
		HbefaVehicleAttributes vehicleAttributes;

		for(Person person : this.scenario.getPopulation().getPersons().values()){
			Id<Person> personId = person.getId();

			if(personId.toString().startsWith("gv")){
				vehicleCategory = HbefaVehicleCategory.HEAVY_GOODS_VEHICLE;
			} else {
				vehicleCategory = HbefaVehicleCategory.PASSENGER_CAR;
			}
			vehicleAttributes = new HbefaVehicleAttributes();

			Id<VehicleType> vehTypeId = Id.create(vehicleCategory + ";" + 
					vehicleAttributes.getHbefaTechnology() + ";" + 
					vehicleAttributes.getHbefaSizeClass() + ";" + 
					vehicleAttributes.getHbefaEmConcept(),VehicleType.class);
			VehicleType vehicleType = VehicleUtils.getFactory().createVehicleType(vehTypeId);
//			vehicleType.setDescription(vehTypeId.toString());
			vehicleType.setDescription(EmissionSpecificationMarker.BEGIN_EMISSIONS+vehTypeId.toString()+EmissionSpecificationMarker.END_EMISSIONS);
			if(!(outputVehicles.getVehicleTypes().containsKey(vehTypeId))){//getVehicles().containsKey(vehTypeId))){
				outputVehicles.addVehicleType(vehicleType);
			} else {
				// do nothing
			}

			Vehicle vehicle = VehicleUtils.getFactory().createVehicle(Id.create(personId, Vehicle.class), vehicleType);
			outputVehicles.addVehicle(vehicle);//getVehicles().put(vehicle.getId(), vehicle);
		}

		VehicleWriterV1 vehicleWriter = new VehicleWriterV1(outputVehicles);
		vehicleWriter.writeFile(outputVehicleFile);
		LOG.info("Writing emission Vehicles files is finished.");
	}
}