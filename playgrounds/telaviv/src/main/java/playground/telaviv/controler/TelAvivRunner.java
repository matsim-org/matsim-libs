/* *********************************************************************** *
 * project: org.matsim.*
 * TelAvivRunner.java
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

package playground.telaviv.controler;

import com.google.inject.Provider;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceInitializer;
import org.matsim.contrib.locationchoice.facilityload.FacilitiesLoadCalculator;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.roadpricing.ControlerDefaultsWithRoadPricingModule;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import playground.telaviv.config.TelAvivConfig;
import playground.telaviv.core.mobsim.qsim.TTAQSimFactory;
import playground.telaviv.locationchoice.matsimdc.DCScoringFunctionFactory;

public class TelAvivRunner {
	
	public void run(Scenario scenario, String basePath) {

		if (basePath != null) TelAvivConfig.basePath = basePath;
		
		Config config = scenario.getConfig();

		VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create("car", VehicleType.class));
		car.setPcuEquivalents(TelAvivConfig.carPcuEquivalents);
		car.setMaximumVelocity(TelAvivConfig.carMaximumVelocity);
		scenario.getVehicles().addVehicleType(car);

		VehicleType truck = VehicleUtils.getFactory().createVehicleType(Id.create("truck", VehicleType.class));
		truck.setPcuEquivalents(TelAvivConfig.truckPcuEquivalents);
		truck.setMaximumVelocity(TelAvivConfig.truckMaximumVelocity);
		scenario.getVehicles().addVehicleType(truck);

		VehicleType commercial = VehicleUtils.getFactory().createVehicleType(Id.create("commercial", VehicleType.class));
		commercial.setPcuEquivalents(TelAvivConfig.commercialPcuEquivalents);
		commercial.setMaximumVelocity(TelAvivConfig.commercialMaximumVelocity);
		scenario.getVehicles().addVehicleType(commercial);

		scenario.getConfig().qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);

		final Controler controler = new Controler(scenario);
		
		/*
		 * Add road pricing contrib.
		 * It registers a adapted TravelDisutilityFactory which replaces the one used by default.
		 */
        controler.setModules(new ControlerDefaultsWithRoadPricingModule());

        // use an adapted MobsimFactory
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindMobsim().toProvider(new Provider<Mobsim>() {
					@Override
					public Mobsim get() {
						return new TTAQSimFactory().createMobsim(controler.getScenario(), controler.getEvents());
					}
				});
			}
		});

		controler.addControlerListener(new TelAvivControlerListener());
		
		/*
		 * We use a Scoring Function that get the Facility Opening Times from
		 * the Facilities instead of the Config File.
		 * This is now included in the location choice scoring function.
		 */
//		PlanCalcScoreConfigGroup pcsConfigGroup = config.planCalcScore();
//		controler.setScoringFunctionFactory(new CharyparNagelOpenTimesScoringFunctionFactory(pcsConfigGroup, scenario));
//		controler.setScoringFunctionFactory(new CharyparNagelOpenTimesAndDesiresScoringFunctionFactory(pcsConfigGroup, scenario));

		/*
		 * Add location choice contrib 
		 */
		DestinationChoiceBestResponseContext dcContext = new DestinationChoiceBestResponseContext(scenario);
		dcContext.init();
		
		/* 
		 * Add location choice ScoringFunctionFactory to controler.
		 * 
		 * It uses an activity scoring function that get opening times from facilities.
		 * Typical activity duration are read from agents' desires and stored in the
		 * DCPrefs, which are used by the scoring function. Those two features are
		 * what is the difference between MATSims default scoring and the
		 * CharyparNagelOpenTimesAndDesiresScoringFunctionFactory.
		 * 
		 * In this way scoringFunction does not need to create new, identical k-vals by itself.    
		 */
		// does not use desires and also does not respect money events!
  		DCScoringFunctionFactory dcScoringFunctionFactory = new DCScoringFunctionFactory(config, controler, dcContext); 	
		controler.setScoringFunctionFactory(dcScoringFunctionFactory);	

		controler.addControlerListener(new DestinationChoiceInitializer(dcContext));
		
		DestinationChoiceConfigGroup lcConfigGroup = (DestinationChoiceConfigGroup) config.getModule("locationchoice");
		double restraintFcnExp = lcConfigGroup.getRestraintFcnExp();
		double restraintFcnFactor = lcConfigGroup.getRestraintFcnFactor();
		if (restraintFcnExp > 0.0 && restraintFcnFactor > 0.0) {		
			controler.addControlerListener(new FacilitiesLoadCalculator(dcContext.getFacilityPenalties()));
		}
		
		controler.run();
	}
}
