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
package playground.agarwalamit.mixedTraffic.patnaIndia.subPop;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl.Builder;
import org.matsim.core.replanning.modules.ChangeLegMode;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import javax.inject.Provider;
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

		sc.getConfig().qsim().setVehiclesSource(VehiclesSource.fromVehiclesData);

		Map<String, VehicleType> modesType = new HashMap<>();
		VehicleType slumCar = VehicleUtils.getFactory().createVehicleType(Id.create("slum_car",VehicleType.class));
		slumCar.setMaximumVelocity(60.0/3.6);
		slumCar.setPcuEquivalents(1.0);
		modesType.put("slum_car", slumCar);
		sc.getVehicles().addVehicleType(slumCar);
		
		VehicleType nonSlumCar = VehicleUtils.getFactory().createVehicleType(Id.create("nonSlum_car",VehicleType.class));
		nonSlumCar.setMaximumVelocity(60.0/3.6);
		nonSlumCar.setPcuEquivalents(1.0);
		modesType.put("nonSlum_car", nonSlumCar);
		sc.getVehicles().addVehicleType(nonSlumCar);

		VehicleType slumMotorbike = VehicleUtils.getFactory().createVehicleType(Id.create("slum_motorbike",VehicleType.class));
		slumMotorbike.setMaximumVelocity(60.0/3.6);
		slumMotorbike.setPcuEquivalents(0.25);
		modesType.put("slum_motorbike", slumMotorbike);
		sc.getVehicles().addVehicleType(slumMotorbike);
		
		VehicleType nonSlumMotorbike = VehicleUtils.getFactory().createVehicleType(Id.create("nonSlum_motorbike",VehicleType.class));
		nonSlumMotorbike.setMaximumVelocity(60.0/3.6);
		nonSlumMotorbike.setPcuEquivalents(0.25);
		modesType.put("nonSlum_motorbike", nonSlumMotorbike);
		sc.getVehicles().addVehicleType(nonSlumMotorbike);

		VehicleType slumBike = VehicleUtils.getFactory().createVehicleType(Id.create("slum_bike",VehicleType.class));
		slumBike.setMaximumVelocity(15.0/3.6);
		slumBike.setPcuEquivalents(0.25);
		modesType.put("slum_bike", slumBike);
		sc.getVehicles().addVehicleType(slumBike);
		
		VehicleType nonSlumBike = VehicleUtils.getFactory().createVehicleType(Id.create("nonSlum_bike",VehicleType.class));
		nonSlumBike.setMaximumVelocity(15.0/3.6);
		nonSlumBike.setPcuEquivalents(0.25);
		modesType.put("nonSlum_bike", nonSlumBike);
		sc.getVehicles().addVehicleType(nonSlumBike);

		VehicleType slumWalk = VehicleUtils.getFactory().createVehicleType(Id.create("slum_walk",VehicleType.class));
		slumWalk.setMaximumVelocity(1.5);
		//		walk.setPcuEquivalents(0.10);  			
		modesType.put("slum_walk",slumWalk);
		sc.getVehicles().addVehicleType(slumWalk);
		
		VehicleType nonSlumWalk = VehicleUtils.getFactory().createVehicleType(Id.create("nonSlum_walk",VehicleType.class));
		nonSlumWalk.setMaximumVelocity(1.5);
		//		walk.setPcuEquivalents(0.10);  			
		modesType.put("nonSlum_walk",nonSlumWalk);
		sc.getVehicles().addVehicleType(nonSlumWalk);

		VehicleType slumPt = VehicleUtils.getFactory().createVehicleType(Id.create("slum_pt",VehicleType.class));
		slumPt.setMaximumVelocity(40/3.6);
		//		pt.setPcuEquivalents(5);  			
		modesType.put("slum_pt",slumPt);
		sc.getVehicles().addVehicleType(slumPt);
		
		VehicleType nonSlumPt = VehicleUtils.getFactory().createVehicleType(Id.create("nonSlum_pt",VehicleType.class));
		nonSlumPt.setMaximumVelocity(40/3.6);
		//		pt.setPcuEquivalents(5);  			
		modesType.put("nonSlum_pt",nonSlumPt);
		sc.getVehicles().addVehicleType(nonSlumPt);

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

		final Controler controler = new Controler(sc);
		
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		controler.getConfig().controler().setDumpDataAtEnd(true);
		controler.getConfig().controler().setCreateGraphs(true);

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addPlanStrategyBinding("ChangeLegMode_slum").toProvider(new javax.inject.Provider<PlanStrategy>() {
					final Provider<TripRouter> tripRouterProvider = binder().getProvider(TripRouter.class);
					final String[] availableModesSlum = {"slum_bike", "slum_motorbike", "slum_pt", "slum_walk"};

					@Override
					public PlanStrategy get() {
						final Builder builder = new Builder(new RandomPlanSelector<>());
						builder.addStrategyModule(new ChangeLegMode(controler.getConfig().global().getNumberOfThreads(), availableModesSlum, true));
						builder.addStrategyModule(new ReRoute(controler.getScenario(), tripRouterProvider));
						return builder.build();
					}
				});
			}
		});

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addPlanStrategyBinding("ChangeLegMode_nonSlum").toProvider(new javax.inject.Provider<PlanStrategy>() {
					final Provider<TripRouter> tripRouterProvider = binder().getProvider(TripRouter.class);
					final String[] availableModeNonSlum = {"nonSlum_car", "nonSlum_bike", "nonSlum_motorbike", "nonSlum_pt", "nonSlum_walk"};

					@Override
					public PlanStrategy get() {
						final Builder builder = new Builder(new RandomPlanSelector<>());
						builder.addStrategyModule(new ChangeLegMode(controler.getConfig().global().getNumberOfThreads(), availableModeNonSlum, true));
						builder.addStrategyModule(new ReRoute(controler.getScenario(), tripRouterProvider));
						return builder.build();
					}
				});
			}
		});

//		services.setScoringFunctionFactory(new SubPopulationScoringFactory(services.getScenario()));
		controler.run();
	}
}
