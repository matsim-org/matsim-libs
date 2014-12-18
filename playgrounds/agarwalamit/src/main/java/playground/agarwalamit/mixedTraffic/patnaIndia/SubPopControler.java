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
package playground.agarwalamit.mixedTraffic.patnaIndia;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.PlanStrategyImpl.Builder;
import org.matsim.core.replanning.modules.ChangeLegMode;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

/**
 * @author amit
 */

public class SubPopControler {


	public static void main(String[] args) {

		SubpopulationConfig subPopConfig = new SubpopulationConfig();
		subPopConfig.run();
		Config config = subPopConfig.getPatnaConfig();
		Scenario sc = ScenarioUtils.loadScenario(config);

		sc.getConfig().qsim().setUseDefaultVehicles(false);
		((ScenarioImpl) sc).createVehicleContainer();

		Map<String, VehicleType> modesType = new HashMap<String, VehicleType>(); 
		VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create("car",VehicleType.class));
		car.setMaximumVelocity(60.0/3.6);
		car.setPcuEquivalents(1.0);
		modesType.put("car", car);
		sc.getVehicles().addVehicleType(car);

		VehicleType motorcycle = VehicleUtils.getFactory().createVehicleType(Id.create("motorbike",VehicleType.class));
		motorcycle.setMaximumVelocity(60.0/3.6);
		motorcycle.setPcuEquivalents(0.25);
		modesType.put("motorbike", motorcycle);
		sc.getVehicles().addVehicleType(motorcycle);

		VehicleType bicycle = VehicleUtils.getFactory().createVehicleType(Id.create("bike",VehicleType.class));
		bicycle.setMaximumVelocity(15.0/3.6);
		bicycle.setPcuEquivalents(0.25);
		modesType.put("bike", bicycle);
		sc.getVehicles().addVehicleType(bicycle);

		VehicleType walk = VehicleUtils.getFactory().createVehicleType(Id.create("walk",VehicleType.class));
		walk.setMaximumVelocity(1.5);
		//		walk.setPcuEquivalents(0.10);  			
		modesType.put("walk",walk);
		sc.getVehicles().addVehicleType(walk);

		VehicleType pt = VehicleUtils.getFactory().createVehicleType(Id.create("pt",VehicleType.class));
		pt.setMaximumVelocity(40/3.6);
		//		pt.setPcuEquivalents(5);  			
		modesType.put("pt",pt);
		sc.getVehicles().addVehicleType(pt);

		for(Person p:sc.getPopulation().getPersons().values()){
			Id<Vehicle> vehicleId = Id.create(p.getId(),Vehicle.class);
			String travelMode = null;
			for(PlanElement pe :p.getSelectedPlan().getPlanElements()){
				if (pe instanceof Leg) {
					travelMode = ((Leg)pe).getMode();
					break;
				}
			}
			Vehicle vehicle = VehicleUtils.getFactory().createVehicle(vehicleId,modesType.get(travelMode));
			sc.getVehicles().addVehicle(vehicle);
		}

		Controler controler = new Controler(sc);
		controler.setOverwriteFiles(true);
		controler.setDumpDataAtEnd(true);
		controler.setCreateGraphs(true);
		
		controler.addPlanStrategyFactory("ChangeLegMode_slum", new PlanStrategyFactory() {
			String [] availableModes_slum = {"bike","motorbike","pt","walk"};
			@Override
			public PlanStrategy createPlanStrategy(Scenario scenario,
					EventsManager eventsManager) {
				PlanStrategyImpl.Builder builder = new Builder(new RandomPlanSelector<Plan, Person>());
				builder.addStrategyModule(new ChangeLegMode(scenario.getConfig().global().getNumberOfThreads(),availableModes_slum,true));
				builder.addStrategyModule(new ReRoute(scenario));
				return builder.build();
			}
		});
		
		controler.addPlanStrategyFactory("ChangeLegMode_nonSlum", new PlanStrategyFactory() {
			String [] availableModes_nonSlum = {"car","bike","motorbike","pt","walk"};
			@Override
			public PlanStrategy createPlanStrategy(Scenario scenario,
					EventsManager eventsManager) {
				PlanStrategyImpl.Builder builder = new Builder(new RandomPlanSelector<Plan, Person>());
				builder.addStrategyModule(new ChangeLegMode(scenario.getConfig().global().getNumberOfThreads(), availableModes_nonSlum, true));
				builder.addStrategyModule(new ReRoute(scenario));
				return builder.build();
			}
		});
		
		controler.run();

	}

}
