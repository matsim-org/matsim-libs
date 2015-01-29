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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
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

import java.util.HashMap;
import java.util.Map;

/**
 * @author amit
 */

public class SubPopControler {


	public static void main(String[] args) {

//		SubpopulationConfig subPopConfig = new SubpopulationConfig();
//		subPopConfig.run();
//		Config config = subPopConfig.getPatnaConfig();
		Config config = ConfigUtils.loadConfig(args[0]);
		Scenario sc = ScenarioUtils.loadScenario(config);

		sc.getConfig().qsim().setUseDefaultVehicles(false);
		((ScenarioImpl) sc).createVehicleContainer();

		Map<String, VehicleType> modesType = new HashMap<String, VehicleType>(); 
		VehicleType slum_car = VehicleUtils.getFactory().createVehicleType(Id.create("slum_car",VehicleType.class));
		slum_car.setMaximumVelocity(60.0/3.6);
		slum_car.setPcuEquivalents(1.0);
		modesType.put("slum_car", slum_car);
		sc.getVehicles().addVehicleType(slum_car);
		
		VehicleType nonSlum_car = VehicleUtils.getFactory().createVehicleType(Id.create("nonSlum_car",VehicleType.class));
		nonSlum_car.setMaximumVelocity(60.0/3.6);
		nonSlum_car.setPcuEquivalents(1.0);
		modesType.put("nonSlum_car", nonSlum_car);
		sc.getVehicles().addVehicleType(nonSlum_car);

		VehicleType slum_motorbike = VehicleUtils.getFactory().createVehicleType(Id.create("slum_motorbike",VehicleType.class));
		slum_motorbike.setMaximumVelocity(60.0/3.6);
		slum_motorbike.setPcuEquivalents(0.25);
		modesType.put("slum_motorbike", slum_motorbike);
		sc.getVehicles().addVehicleType(slum_motorbike);
		
		VehicleType nonSlum_motorbike = VehicleUtils.getFactory().createVehicleType(Id.create("nonSlum_motorbike",VehicleType.class));
		nonSlum_motorbike.setMaximumVelocity(60.0/3.6);
		nonSlum_motorbike.setPcuEquivalents(0.25);
		modesType.put("nonSlum_motorbike", nonSlum_motorbike);
		sc.getVehicles().addVehicleType(nonSlum_motorbike);

		VehicleType slum_bike = VehicleUtils.getFactory().createVehicleType(Id.create("slum_bike",VehicleType.class));
		slum_bike.setMaximumVelocity(15.0/3.6);
		slum_bike.setPcuEquivalents(0.25);
		modesType.put("slum_bike", slum_bike);
		sc.getVehicles().addVehicleType(slum_bike);
		
		VehicleType nonSlum_bike = VehicleUtils.getFactory().createVehicleType(Id.create("nonSlum_bike",VehicleType.class));
		nonSlum_bike.setMaximumVelocity(15.0/3.6);
		nonSlum_bike.setPcuEquivalents(0.25);
		modesType.put("nonSlum_bike", nonSlum_bike);
		sc.getVehicles().addVehicleType(nonSlum_bike);

		VehicleType slum_walk = VehicleUtils.getFactory().createVehicleType(Id.create("slum_walk",VehicleType.class));
		slum_walk.setMaximumVelocity(1.5);
		//		walk.setPcuEquivalents(0.10);  			
		modesType.put("slum_walk",slum_walk);
		sc.getVehicles().addVehicleType(slum_walk);
		
		VehicleType nonSlum_walk = VehicleUtils.getFactory().createVehicleType(Id.create("nonSlum_walk",VehicleType.class));
		nonSlum_walk.setMaximumVelocity(1.5);
		//		walk.setPcuEquivalents(0.10);  			
		modesType.put("nonSlum_walk",nonSlum_walk);
		sc.getVehicles().addVehicleType(nonSlum_walk);

		VehicleType slum_pt = VehicleUtils.getFactory().createVehicleType(Id.create("slum_pt",VehicleType.class));
		slum_pt.setMaximumVelocity(40/3.6);
		//		pt.setPcuEquivalents(5);  			
		modesType.put("slum_pt",slum_pt);
		sc.getVehicles().addVehicleType(slum_pt);
		
		VehicleType nonSlum_pt = VehicleUtils.getFactory().createVehicleType(Id.create("nonSlum_pt",VehicleType.class));
		nonSlum_pt.setMaximumVelocity(40/3.6);
		//		pt.setPcuEquivalents(5);  			
		modesType.put("nonSlum_pt",nonSlum_pt);
		sc.getVehicles().addVehicleType(nonSlum_pt);

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
		
//		IOUtils.deleteDirectory(new File(controler.getConfig().controler().getOutputDirectory()));
		controler.setOverwriteFiles(true);
		controler.setDumpDataAtEnd(true);
        controler.getConfig().controler().setCreateGraphs(true);

        controler.addPlanStrategyFactory("ChangeLegMode_slum", new PlanStrategyFactory() {
			String [] availableModes_slum = {"slum_bike","slum_motorbike","slum_pt","slum_walk"};
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
			String [] availableModes_nonSlum = {"nonSlum_car","nonSlum_bike","nonSlum_motorbike","nonSlum_pt","nonSlum_walk"};
			@Override
			public PlanStrategy createPlanStrategy(Scenario scenario,
					EventsManager eventsManager) {
				PlanStrategyImpl.Builder builder = new Builder(new RandomPlanSelector<Plan, Person>());
				builder.addStrategyModule(new ChangeLegMode(scenario.getConfig().global().getNumberOfThreads(), availableModes_nonSlum, true));
				builder.addStrategyModule(new ReRoute(scenario));
				return builder.build();
			}
		});
		
//		controler.setScoringFunctionFactory(new SubPopulationScoringFactory(controler.getScenario()));
		controler.run();
	}
}
