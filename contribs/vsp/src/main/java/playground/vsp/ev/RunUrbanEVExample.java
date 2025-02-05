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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.fleet.ElectricFleetUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehiclesFactory;

import static org.matsim.core.config.groups.ScoringConfigGroup.*;

/**
 * this is an example of how to run MATSim with the UrbanEV module which inserts charging activities for all legs which use a EV.
 * By default, {@link ElectricFleetUpdater} is used, which declares any vehicle as an EV
 * that has a vehicle type with HbefaTechnology set to 'electricity'.
 * At the beginning of each iteration, the consumption is estimated. Charging is planned to take place during the latest possible activity in the agent's plan
 * that fits certain criteria (ActivityType and minimum duration) and takes place before the estimated SOC drops below a defined threshold.
 */
public class RunUrbanEVExample {

	static final double CAR_BATTERY_CAPACITY_kWh = 20.;
	static final double CAR_INITIAL_SOC = 0.5;

	public static void main(String[] args) {
//		String pathToConfig = args.length > 0 ?
//				args[0] :
//				"contribs/vsp/test/input/playground/vsp/ev/chessboard-config.xml";

		Config config = prepareConfig( args );

		Scenario scenario = prepareScenario( config );

		Controler controler = prepareControler(scenario);

		controler.run();
	}
	private static Scenario prepareScenario( Config config ){
		Scenario scenario = ScenarioUtils.loadScenario( config );
		createAndRegisterPersonalCarAndBikeVehicles(scenario);
		return scenario;
	}
	private static Config prepareConfig( String[] args ){
		Config config = ConfigUtils.loadConfig( args );
		EvConfigGroup evConfigGroup = ConfigUtils.addOrGetModule( config, EvConfigGroup.class );
		evConfigGroup.timeProfiles = true;
		evConfigGroup.chargersFile = "chargers.xml";

		config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.fromVehiclesData);

//		UrbanEVConfigGroup urbanEVConfi = new UrbanEVConfigGroup();
//		config.addModule(urbanEVConfi );
		UrbanEVConfigGroup urbanEVConfig = ConfigUtils.addOrGetModule( config, UrbanEVConfigGroup.class );
		urbanEVConfig.setCriticalSOC(0.4);

		//TODO actually, should also work with all AccessEgressTypes but we have to check (write JUnit test)
		config.routing().setAccessEgressType(RoutingConfigGroup.AccessEgressType.none );

		//register charging interaction activities for car
		config.scoring().addActivityParams(
				new ActivityParams(TransportMode.car + UrbanEVModule.PLUGOUT_INTERACTION).setScoringThisActivityAtAll(false ) );
		config.scoring().addActivityParams(
				new ActivityParams( TransportMode.car + UrbanEVModule.PLUGIN_INTERACTION).setScoringThisActivityAtAll( false ) );
		return config;
	}

	public static Controler prepareControler(Scenario scenario) {

		Controler controler = new Controler(scenario);
		//plug in UrbanEVModule
		controler.addOverridingModule(new UrbanEVModule());
		//register EV qsim components
//		controler.configureQSimComponents(components -> components.addNamedComponent(EvModule.EV_COMPONENT));

//		controler.addOverridingQSimModule( new AbstractQSimModule(){
//			@Override protected void configureQSim(){
//				this.addQSimComponentBinding( EvModule.EV_COMPONENT );
//			}
//		} );

		return controler;
	}

	static void createAndRegisterPersonalCarAndBikeVehicles(Scenario scenario) {
		VehiclesFactory vehicleFactory = scenario.getVehicles().getFactory();

		for (Person person : scenario.getPopulation().getPersons().values()) {

			VehicleType carVehicleType = vehicleFactory.createVehicleType(Id.create(person.getId().toString(),
					VehicleType.class)); //TODO should at least have a suffix "_car"
			VehicleUtils.setHbefaTechnology(carVehicleType.getEngineInformation(), "electricity");
			VehicleUtils.setEnergyCapacity(carVehicleType.getEngineInformation(), CAR_BATTERY_CAPACITY_kWh);
			ElectricFleetUtils.setChargerTypes(carVehicleType.getEngineInformation(), Arrays.asList("a", "b", "default" ) );
			scenario.getVehicles().addVehicleType(carVehicleType);
			carVehicleType.setNetworkMode(TransportMode.car);
			Vehicle carVehicle = vehicleFactory.createVehicle(VehicleUtils.createVehicleId(person, TransportMode.car),
					carVehicleType);
			ElectricFleetUtils.setInitialSoc(carVehicle, CAR_INITIAL_SOC );
			scenario.getVehicles().addVehicle(carVehicle);

			VehicleType bikeVehicleType = vehicleFactory.createVehicleType(
					Id.create(person.getId().toString() + "_bike", VehicleType.class));
			bikeVehicleType.setNetworkMode(TransportMode.bike);
			Vehicle bikeVehicle = vehicleFactory.createVehicle(VehicleUtils.createVehicleId(person, TransportMode.bike),
					bikeVehicleType);

			scenario.getVehicles().addVehicleType(bikeVehicleType);
			scenario.getVehicles().addVehicle(bikeVehicle);

			Map<String, Id<Vehicle>> mode2Vehicle = new HashMap<>();
			mode2Vehicle.put(TransportMode.car, carVehicle.getId());
			mode2Vehicle.put(TransportMode.bike, bikeVehicle.getId());

			//override the attribute - we assume to need car and bike only
			VehicleUtils.insertVehicleIdsIntoAttributes(person, mode2Vehicle);
		}
	}
}
