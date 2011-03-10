/* *********************************************************************** *
 * project: org.matsim.*
 * VehicleGenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.benjamin.szenarios.munich.testroad;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.EngineInformation.FuelType;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;
import org.matsim.vehicles.VehiclesImpl;


/**
 * @author benjamin
 *
 */
public class VehicleGenerator {
	
	private String populationFile = "../../detailedEval/teststrecke/sim/input/20090707_plans.xml.gz";
	private String netFile = "../../detailedEval/teststrecke/sim/input/network.xml";
	
	private String outputPath = "../../detailedEval/teststrecke/sim/inputVehicles/";
	
	private FuelType fuelType;

	public static void main(String[] args) {
		VehicleGenerator application = new VehicleGenerator();
		application.run(args);
	}

	private void run(String[] args) {
		Config config1 = ConfigUtils.createConfig();
		Scenario sc = (ScenarioImpl) ScenarioUtils.createScenario(config1);
		Config config = sc.getConfig();
		config.network().setInputFile(netFile);
		config.plans().setInputFile(populationFile);
		
		ScenarioLoaderImpl sl = new ScenarioLoaderImpl(sc) ;
		sl.loadScenario() ;
		Population population = sc.getPopulation();
		
		Vehicles vehicles = generateVehicles(population);
		writeVehicles (vehicles);
	}

	@SuppressWarnings("static-access")
	private Vehicles generateVehicles(Population population) {
		Vehicles vehicles =  new VehiclesImpl();
		VehiclesFactory vehicleFactory = vehicles.getFactory();

		VehicleType vehicleType = vehicleFactory.createVehicleType(new IdImpl("Mercedes"));
		double gasConsumption = 7.5;

		EngineInformation engineInfo = vehicleFactory.createEngineInformation(fuelType.diesel, gasConsumption);
		vehicleType.setEngineInformation(engineInfo);
		vehicles.getVehicleTypes().put(vehicleType.getId(), vehicleType);
		
		for(Person person : population.getPersons().values()){
			Id vehicleId = person.getId();
			
			Vehicle vehicle = vehicleFactory.createVehicle(vehicleId, vehicleType);
			
			vehicles.getVehicles().put(vehicleId, vehicle);
		}
		return vehicles;
	}

	private void writeVehicles(Vehicles vehicles) {
		VehicleWriterV1 vehicleWriter = new VehicleWriterV1(vehicles);
		try {
			vehicleWriter.writeFile(outputPath + "vehicles.xml");
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
