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
package playground.agarwalamit.patnaIndia;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

/**
 * @author amit
 */
public class CreateVehiclesForPatna {

	private final static String inputPlans = "./patnaOutput/plans.xml";

	public static void main(String[] args) {

		Config config = ConfigUtils.createConfig();
		config.plans().setInputFile(inputPlans);
		Scenario scenario = ScenarioUtils.loadScenario(config);

		Vehicles vehicles = VehicleUtils.createVehiclesContainer();

		Map<String, VehicleType> modesType = new HashMap<String, VehicleType>(); 
		VehicleType car = VehicleUtils.getFactory().createVehicleType(new IdImpl("car"));
		car.setMaximumVelocity(60.0/3.6);
		car.setPcuEquivalents(1.0);
		modesType.put("car", car);

		VehicleType motorcycle = VehicleUtils.getFactory().createVehicleType(new IdImpl("motorbike"));
		motorcycle.setMaximumVelocity(60.0/3.6);
		motorcycle.setPcuEquivalents(0.25);
		modesType.put("motorbike", motorcycle);

		VehicleType bicycle = VehicleUtils.getFactory().createVehicleType(new IdImpl("bike"));
		bicycle.setMaximumVelocity(15.0/3.6);
		bicycle.setPcuEquivalents(0.25);
		modesType.put("bike", bicycle);

		VehicleType walk = VehicleUtils.getFactory().createVehicleType(new IdImpl("walk"));
		walk.setMaximumVelocity(1.5);
		walk.setPcuEquivalents(0.10);  			// assumed pcu for walks is 0.1
		modesType.put("walk",walk);

		VehicleType pt = VehicleUtils.getFactory().createVehicleType(new IdImpl("pt"));
		pt.setMaximumVelocity(40/3.6);
		pt.setPcuEquivalents(5);  			// assumed pcu for walks is 0.1
		modesType.put("pt",pt);

		vehicles.addVehicleType(car);
		vehicles.addVehicleType(motorcycle);
		vehicles.addVehicleType(bicycle);
		vehicles.addVehicleType(walk);
		vehicles.addVehicleType(pt);
		
		for(Person p : scenario.getPopulation().getPersons().values()){
			PlanElement element = p.getSelectedPlan().getPlanElements().get(1);
			String travelMode =  ((Leg) element).getMode();

			if(!modesType.containsKey(travelMode)){
				throw new RuntimeException("Vehicle Type is not defined. Define"+ travelMode+ "vehicle Type.");	
			}

			VehicleType vType = modesType.get(travelMode);
			Vehicle veh =  VehicleUtils.getFactory().createVehicle(p.getId(), vType);
			vehicles.addVehicle(veh);
		}

		new VehicleWriterV1(vehicles).writeFile("./patnaoutput/vehiclesPatna.xml");
	}
}
