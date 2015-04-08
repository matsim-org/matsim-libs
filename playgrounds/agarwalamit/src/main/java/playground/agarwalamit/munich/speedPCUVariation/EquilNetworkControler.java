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
package playground.agarwalamit.munich.speedPCUVariation;

import java.io.BufferedWriter;
import java.util.Arrays;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.types.HbefaVehicleAttributes;
import org.matsim.contrib.emissions.types.HbefaVehicleCategory;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.replanning.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

import playground.agarwalamit.utils.LoadMyScenarios;
import playground.benjamin.internalization.EmissionCostModule;
import playground.benjamin.internalization.EmissionTravelDisutilityCalculatorFactory;
import playground.benjamin.internalization.InternalizeEmissionsControlerListener;

/**
 * @author amit
 */

public class EquilNetworkControler {

	private final String equilNetwork_raw = "../../matsim/examples/equil/network.xml";
	
	private final String equilNetwork = "./equil/input/equilNetworkRoadType.xml.gz";
	private final String equilNetworkPlans = "./equil/input/equilMixedTrafficPlans.xml.gz";
	private static final String equilEmissionVehicleFile = "./equil/input/emissionVehicleFile.xml.gz";
	private static final String equilRoadTypeMapping = "./equil/input/roadTypeMapping.txt";
	
	private final String [] mainModes = {"car","truck"};
	private final double fractionOfTrucks = 0.2;
	private static final String outputDir = "./equil/output/carTruck/";

	public static void main(String[] args) {

		EquilNetworkControler equil = new EquilNetworkControler();
		equil.generateRoadTypeMapping();

		Scenario sc = equil.createEquilScenario();
		Controler controler = new Controler(sc);
		
		//create emission vehicles now.
		EmissionsConfigGroup ecg = new EmissionsConfigGroup();
		controler.getConfig().addModule(ecg);
		
		ecg.setAverageColdEmissionFactorsFile("/Users/amit/Documents/workspace/input/matsimHBEFAStandardsFiles/EFA_ColdStart_vehcat_2005average.txt");
		ecg.setAverageWarmEmissionFactorsFile("/Users/amit/Documents/workspace/input/matsimHBEFAStandardsFiles/EFA_HOT_vehcat_2005average.txt");
		
		ecg.setEmissionRoadTypeMappingFile(equilRoadTypeMapping);
		
		equil.generateEmissionVehicles(sc);
		ecg.setEmissionVehicleFile(equilEmissionVehicleFile);
		ecg.setUsingDetailedEmissionCalculation(false);
		
		EmissionModule emissionModule = new EmissionModule(sc);
		emissionModule.createLookupTables();
		emissionModule.createEmissionHandler();

		EmissionCostModule ecm = new EmissionCostModule(1.0);
		EmissionTravelDisutilityCalculatorFactory emissFact = new EmissionTravelDisutilityCalculatorFactory(emissionModule, ecm);
		controler.setTravelDisutilityFactory(emissFact);
		
		controler.addControlerListener(new InternalizeEmissionsControlerListener(emissionModule, ecm));
		
		controler.setOverwriteFiles(true);
		controler.getConfig().controler().setOutputDirectory(outputDir);
		controler.run();
	}
	
	private void generateRoadTypeMapping(){
		Scenario sc = LoadMyScenarios.loadScenarioFromNetwork(equilNetwork_raw);
		Network net = sc.getNetwork();
		for(Link l :sc.getNetwork().getLinks().values()){
			((LinkImpl) l).setType("5");
			net.addLink(l);
		}
		new NetworkWriter(sc.getNetwork()).write(equilNetwork);
		
		BufferedWriter writer = IOUtils.getBufferedWriter(equilRoadTypeMapping);
		try {
			writer.write("VISUM_RT_NR" + ";" + "VISUM_RT_NAME" + ";"+ "HBEFA_RT_NAME" + "\n");
			writer.write("5;;URB/Trunk-Nat./80");
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: "
					+ e);
		}
	}
	
	private void generateEmissionVehicles(Scenario scenario) {
		Vehicles outputVehicles = VehicleUtils.createVehiclesContainer();

		HbefaVehicleCategory vehicleCategory;
		HbefaVehicleAttributes vehicleAttributes;

		for(Person person : scenario.getPopulation().getPersons().values()){
			Id<Person> personId = person.getId();

			if(personId.toString().startsWith("truck")){
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
			
			if(!(outputVehicles.getVehicleTypes().containsKey(vehTypeId))){
				outputVehicles.addVehicleType(vehicleType);
			} else {
				// do nothing
			}

			Vehicle vehicle = VehicleUtils.getFactory().createVehicle(Id.create(personId, Vehicle.class), vehicleType);
			outputVehicles.addVehicle(vehicle);
		}

		VehicleWriterV1 vehicleWriter = new VehicleWriterV1(outputVehicles);
		vehicleWriter.writeFile(equilEmissionVehicleFile);
	}

	private Scenario createEquilScenario(){

		// new scenario for mixed traffic
		Scenario sc = LoadMyScenarios.loadScenarioFromNetwork(equilNetwork);
		
		//vehicle types
		((ScenarioImpl)sc).createVehicleContainer();
		sc.getConfig().qsim().setUseDefaultVehicles(false);
		sc.getConfig().plansCalcRoute().setNetworkModes(Arrays.asList(mainModes));
		sc.getConfig().qsim().setMainModes(Arrays.asList(mainModes));
		
		VehicleType car =  VehicleUtils.getFactory().createVehicleType(Id.create("car", VehicleType.class));
		car.setPcuEquivalents(1.0);
		car.setMaximumVelocity(100/3.6);
		sc.getVehicles().addVehicleType(car);
		
		VehicleType truck =  VehicleUtils.getFactory().createVehicleType(Id.create("truck", VehicleType.class));
		truck.setPcuEquivalents(3.0);
		truck.setMaximumVelocity(60/3.6);
		sc.getVehicles().addVehicleType(truck);
		
		//now use same population but create 10 % of ppl as trucks
		Population popOut = sc.getPopulation();
		PopulationFactory popFactory = popOut.getFactory();

		int noOfCar =(int) (4000*(1-fractionOfTrucks));
		int noOfTrucks = 4000 - noOfCar;

		for(int i = 1; i <= noOfCar+noOfTrucks; i++){
			String mode ;
			if(i<3200) mode = mainModes[0];
			else mode = mainModes[1];
			
			Person p = popFactory.createPerson(Id.createPersonId(mode+"_"+i));
			popOut.addPerson(p);
			Plan plan = popFactory.createPlan();
			p.addPlan(plan);
			
			Id<Link> homeActLink = Id.createLinkId("1");
			Id<Link> workActLink = Id.createLinkId("20");
			Activity h = popFactory.createActivityFromLinkId("h", homeActLink);
			h.setEndTime(06*3600);
			plan.addActivity(h);

			Leg leg = popFactory.createLeg(mode);
			plan.addLeg(leg);
			
			Activity w = popFactory.createActivityFromLinkId("w", workActLink);
			w.setEndTime(14*3600);
			plan.addActivity(w);
			
			Leg leg2 = popFactory.createLeg(mode);
			plan.addLeg(leg2);
			
			plan.addActivity(popFactory.createActivityFromLinkId("h", homeActLink));
			
			VehicleType vehTyp = p.getId().toString().startsWith("car") ? car : truck;
			Id<Vehicle> vehId = Id.create(p.getId(), Vehicle.class);
			Vehicle veh = VehicleUtils.getFactory().createVehicle(vehId, vehTyp);
			sc.getVehicles().addVehicle(veh);
		}
		new PopulationWriter(popOut).write(equilNetworkPlans);
	
		// inputs files
		sc.getConfig().plans().setInputFile(equilNetworkPlans);

		//create config parameters
		sc.getConfig().qsim().setStartTime(00*3600); //  start time 0 --> start with earliest activity
		sc.getConfig().qsim().setEndTime(00*3600); //  end time 0 --> run as long as a vehicle is active
		sc.getConfig().qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.PassingQ.name());

		sc.getConfig().controler().setFirstIteration(0);
		sc.getConfig().controler().setLastIteration(10);
		sc.getConfig().controler().setMobsim("qsim");

		ActivityParams h = new ActivityParams("h");
		h.setTypicalDuration(12*3600.);
		sc.getConfig().planCalcScore().addActivityParams(h);

		ActivityParams w = new ActivityParams("w");
		w.setTypicalDuration(8*3600.);
		sc.getConfig().planCalcScore().addActivityParams(w);

		StrategySettings reRoute = new StrategySettings(ConfigUtils.createAvailableStrategyId(sc.getConfig()));
		reRoute.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute.name());
		reRoute.setWeight(0.15);
		sc.getConfig().strategy().addStrategySettings(reRoute);

		StrategySettings expChangeBeta = new StrategySettings(ConfigUtils.createAvailableStrategyId(sc.getConfig()));
		expChangeBeta.setStrategyName("ChangeExpBeta");
		expChangeBeta.setWeight(0.85);
		sc.getConfig().strategy().addStrategySettings(expChangeBeta);

		sc.getConfig().strategy().setFractionOfIterationsToDisableInnovation(0.8);

		return sc;
	}
}
