/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,     *
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
package playground.jjoubert.projects.capeTownFreight;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;

/**
 * Executes a single run of the Cape Town freight population.
 *
 * @author jwjoubert
 */
public class RunCapeTownFreight {
	final private static Logger LOG = Logger.getLogger(RunCapeTownFreight.class);

	public static void main(String[] args) {
		Header.printHeader(RunCapeTownFreight.class.toString(), args);

		String folder = args[0] + (args[0].endsWith("/") ? "" : "/");
		Machine machine = Machine.valueOf(args[1]);

		/* Check if output folder exists, and DELETE if it is there. */
		File f = new File(folder + "output/");
		if(f.exists() && f.isDirectory()){
			LOG.warn("Deleting the output folder " + folder + "output/");
			FileUtils.delete(f);
		}

		/* Set up the simulation run. */
		Config config = setupConfig(folder, machine);
		Scenario sc = setupScenario(config);
		Controler controler = setupControler(sc);

		controler.run();

		Header.printFooter();
	}


	/**
	 * Setting up the MATSim {@link Config} so that it runs 20 iterations for
	 * a commercial vehicle population.
	 * 
	 * @param folder the specific folder to which the output will be written.
	 * 		  Also, it assumes the following files are available as input
	 * 		  in the folder:
	 * 		  <ul>
	 * 			<li> {@code population.xml.gz} of commercial vehicles, each 
	 * 				 person with a plan. Only activity locations are required. 
	 * 				 The {@link Leg}s are allowed to take {@code commercial} 
	 * 				 as mode.
	 * 			<li> {@code populationAttributes.xml.gz} indicating which
	 * 				 individuals are part of the <i>commercial vehicle</i>
	 * 				 subpopulation. For that the only attribute required,
	 * 				 namely {@code subpopulation}, needs to be set as
	 * 				 {@code commercial}.
	 * 		  </ul>
	 * @param machine the computer on which the run is executed. This just
	 * 		  sets up the number of threads without having to hard code it
	 * 		  in the class. The following values are currently supported:
	 * 		  <ul>
	 * 			<li> {@code HOBBES} using 40 threads;
	 * 			<li> {@code MAC_MINI} the machine in Engineering 2, 
	 * 				 Room 3-13.1, a dual core i7 using 4 threads;
	 * 			<li> {@code MACBOOK_PRO} is Johan W. Joubert's laptop,
	 * 				 a dual core i5 using 4 threads.
	 * 		  </ul>
	 * @return
	 */
	public static Config setupConfig(String folder, Machine machine){
		Config config = ConfigUtils.createConfig();

		/* Set global settings. */
		config.global().setNumberOfThreads(machine.getThreads());
		config.global().setCoordinateSystem("WGS84_SA_Albers");

		/* Set files and folders. */
		config.controler().setOutputDirectory(folder + "output/");
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(100);
		config.controler().setWriteEventsInterval(20);

		/* Network. */
		config.network().setInputFile(folder + "network.xml.gz");

		/* Population */
		config.plans().setInputFile(folder + "population.xml.gz");
		config.plans().setInputPersonAttributeFile(folder + "populationAttributes.xml.gz");
		config.plans().setActivityDurationInterpretation(
				PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration );

		/* Facilities */
		config.facilities().setInputFile(folder + "facilities.xml.gz");
		
		/* QSim */
		config.qsim().setNumberOfThreads(machine.getThreads());
		String[] modes ={"car","commercial"};
		config.qsim().setMainModes( Arrays.asList(modes) );
		config.plansCalcRoute().setNetworkModes(Arrays.asList(modes));

		/* PlanCalcScore */
		ActivityParams major = new ActivityParams("major");
		major.setTypicalDuration(10*3600);
		config.planCalcScore().addActivityParams(major);

		ActivityParams minor = new ActivityParams("minor");
		minor.setTypicalDuration(1880);
		config.planCalcScore().addActivityParams(minor);

		/* Generic strategy */
		StrategySettings changeExpBetaStrategySettings = new StrategySettings(ConfigUtils.createAvailableStrategyId(config));
		changeExpBetaStrategySettings.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString());
		changeExpBetaStrategySettings.setWeight(0.8);
		config.strategy().addStrategySettings(changeExpBetaStrategySettings);
		/* Subpopulation strategy. */
		StrategySettings commercialStrategy = new StrategySettings(ConfigUtils.createAvailableStrategyId(config));
		commercialStrategy.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString());
		commercialStrategy.setWeight(0.85);
		commercialStrategy.setSubpopulation("commercial");
		config.strategy().addStrategySettings(commercialStrategy);
		/* Subpopulation ReRoute. Switch off after a time. */
		StrategySettings commercialReRoute = new StrategySettings(ConfigUtils.createAvailableStrategyId(config));
		commercialReRoute.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute.name());
		commercialReRoute.setWeight(0.15);
		commercialReRoute.setSubpopulation("commercial");
		commercialReRoute.setDisableAfter(85);
		config.strategy().addStrategySettings(commercialReRoute);

		return config;
	}
	
	
	/**
	 * Setting up the simulation {@link Scenario}, using a given {@link Config}.
	 * In the scenario setup each network link is given both {@code car} and 
	 * {@code commercial} as mode. Also, each person is assigned a vehicle, which
	 * is based on the Gauteng Freeway Improvement Project (GFIP) vehicle toll
	 * classes. the following vehicle types are available.
	 * <ul>
	 * 		<li> {@code A2}: Light delivery vehicle with length 7.0m, maximum
	 * 			 velocity of 100km/h, and accounting for 80% of all commercial
	 * 			 vehicles;
	 * 		<li> {@code B}: Short heavy vehicles with length 12.0m, maximum
	 * 			 velocity of 90km/h, and accounting for 15% of all commercial
	 * 			 vehicles; and
	 * 		<li> {@code C}: Long heavy vehicles with length 20.0m, maximum
	 * 			 velocity of 90km/h, and accounting for the remaining 5% of all
	 * 			 commercial vehicles.
	 * 	</ul>
	 * These vehicle classes are randomly sampled and assigned to the 
	 * individuals. No cognisance is given to intra- and inter-area traffic 
	 * (based on the activity chain structure). This <i><b>should</b></i> be
	 * refined.
	 * 
	 * @param config typically the {@link Config} resulting from calling the
	 * 		  method {@link #setupConfig(String, Machine)}.
	 * @return a scenario in which the network has the necessary {@code commercial}
	 * 		  mode; and each agent has a vehicle with a particular commercial 
	 * 		  vehicle class.
	 */
	public static Scenario setupScenario(Config config){
		Scenario sc = ScenarioUtils.loadScenario(config);
		
		/* Ensure that all links take 'commercial' as mode. */
		LOG.warn("Ensuring all links take 'commercial' as mode...");
		Collection<String> modesCollection = Arrays.asList("car", "commercial");
		Set<String> modesSet = new HashSet<>(modesCollection);
		for(Link link : sc.getNetwork().getLinks().values()){
			link.setAllowedModes(modesSet);
		}
		LOG.warn("Done adding 'commercial' modes.");

		/* Add all VehicleTypes. */
		Vehicles vehicles = sc.getVehicles();
		for(VehicleTypeSA vt : VehicleTypeSA.values()){
			vehicles.addVehicleType(vt.getVehicleType());
		}
		
		MatsimRandom.reset(2015093001l);
		for(Person person : sc.getPopulation().getPersons().values()){
			/* Randomly sample a vehicle type for each person. */
			VehicleType vehicleType = null; 
			double r = MatsimRandom.getRandom().nextDouble();
			if(r <= 0.8 ){
				vehicleType = VehicleTypeSA.A2.getVehicleType();
			} else if(r <= 0.95 ){
				vehicleType = VehicleTypeSA.B.getVehicleType();
			} else{
				vehicleType = VehicleTypeSA.C.getVehicleType();
			}
			
			Vehicle truck = VehicleUtils.getFactory().createVehicle(Id.create(person.getId(), Vehicle.class), vehicleType);
			vehicles.addVehicle(truck);
			
			/* Link the vehicle type to the person's attributes. */
			sc.getPopulation().getPersonAttributes().putAttribute(person.getId().toString(), "vehicleType", vehicleType.getId().toString());
		}

		return sc;
	}


	/**
	 * Currently (Oct 2015), the only controler adaption is binding the default
	 * (car) travel time and travel cost calculator to the {@code commercial}
	 * mode so that it too can be used as a network-routed mode.
	 * 
	 * <h4>TODO:</h4> The assignment of vehicle type to person should be done 
	 * more carefully, with cognisance of whether a vehicle is intra-area or
	 * inter-area. Subsequently, the travel time and travel cost calculators
	 * could be set more realistically. Still, what is <i>'more realistic'</i>?
	 * 
	 * @param sc the scenario to load. Typically this will be the output from
	 * 		  calling the method {@link #setupScenario(Config)}.
	 * 
	 * @return
	 */
	public static Controler setupControler(Scenario sc){
		Controler controler = new Controler(sc);

		/* Ensure that the necessary travel time and travel cost factories
		 * are binded to the controler. This is necessary since we use our own
		 * mode 'commercial' as a network mode. */
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addTravelTimeBinding("commercial").to(networkTravelTime());
				addTravelDisutilityFactoryBinding("commercial").to(carTravelDisutilityFactoryKey());
			}
		});

		return controler;
	}


	/**
	 * Setting up the default values for known machines on which simualtions
	 * are run.
	 *
	 * @author jwjoubert
	 */
	private enum Machine{
		HOBBES(40),
		MAC_MINI(4),
		MACBOOK_PRO(4);

		private final int threads;

		Machine(int threads){
			this.threads = threads;
		}

		public int getThreads(){
			return this.threads;
		}
	}
	
	
	/**
	 * Vehicle types based on the South African National Roads Agency Limited
	 * (SANRAL) toll classes used for the Gauteng Freeway Improvement Project 
	 * (GFIP).
	 *
	 * @author jwjoubert
	 */
	private enum VehicleTypeSA{
		A2(7.0, 100.0),
		B(12.0, 90.0),
		C(20.0, 90.0);
		
		private final double length;
		private final double maxVelocity;
		
		VehicleTypeSA(double length, double maxVelocity){
			this.length = length;
			this.maxVelocity = maxVelocity;
		}
		
		public VehicleType getVehicleType(){
			VehicleType type = new VehicleTypeImpl(Id.create("commercial_" + this.name(), VehicleType.class));
			type.setLength(this.length);
			type.setMaximumVelocity(this.maxVelocity / 3.6);
			return type;
		}
	}

}
