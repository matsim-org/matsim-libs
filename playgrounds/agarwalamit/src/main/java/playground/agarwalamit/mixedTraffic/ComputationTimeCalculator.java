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
package playground.agarwalamit.mixedTraffic;

import java.io.BufferedWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.handler.TeleportationArrivalEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

/**
 * @author amit
 */

public class ComputationTimeCalculator {

	public static void main(String[] args) {

		String outFile = "./output/computaionalPerformance.txt";
		ComputationTimeCalculator calc = new ComputationTimeCalculator();

		calc.openFile(outFile);

		calc.run(LinkDynamics.FIFO.name());
		calc.run(LinkDynamics.PassingQ.name());
		calc.run("seepage");

		calc.closeFile();

	}

	private BufferedWriter writer;

	private void openFile(String outFile){
		writer = IOUtils.getBufferedWriter(outFile);
		try {
			writer.write("######## Run Nr ########\n");
			writer.write("SimulationTime \t LinkDynamics \t TrafficDynamics \t CapacityUpdate \n");
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: "+ e);
		}
	}

	private void closeFile(){
		try {
			writer.close();	
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: "+ e);
		}
	}

	private void run (String ld) {

		if(! ld.equals("seepage")){

			Scenario sc = createBasicSceanrio();
			sc.getConfig().qsim().setLinkDynamics(ld);
			EventsManager events = EventsUtils.createEventsManager();

			for (int i =1; i<26; i++){
				try {
					writer.write("######## "+i+" ########\n");
				} catch (Exception e) {
					throw new RuntimeException(
							"Data is not written in file. Reason: " + e);
				}
				processSceanarios(sc, events);
			}

		} else {

			Scenario sc = createBasicSceanrio();
			sc.getConfig().qsim().setLinkDynamics(LinkDynamics.PassingQ.name()); // must for seepage

			sc.getConfig().setParam("seepage", "isSeepageAllowed", "true");
			sc.getConfig().setParam("seepage", "seepMode", "bike");
			sc.getConfig().setParam("seepage","isSeepModeStorageFree","false");

			EventsManager events = EventsUtils.createEventsManager();
			for (int i =1; i<26; i++){
				try {
					writer.write("######## "+i+"(seepage) ########\n");
				} catch (Exception e) {
					throw new RuntimeException(
							"Data is not written in file. Reason: " + e);
				}
				processSceanarios(sc, events);
			}
		}
	}

	private void processSceanarios(Scenario sc, EventsManager events){
		//-- w/o holes -- old (slow) capacity update
		sc.getConfig().qsim().setTrafficDynamics(TrafficDynamics.queue);
		sc.getConfig().qsim().setUsingFastCapacityUpdate(false);

		double startTime = System.currentTimeMillis();
		QSimUtils.createDefaultQSim(sc, events).run();
		double endTime = System.currentTimeMillis();
		String out = (endTime - startTime) +"\t"+sc.getConfig().qsim().getLinkDynamics()+"\t"+sc.getConfig().qsim().getTrafficDynamics()+"\t"+sc.getConfig().qsim().isUsingFastCapacityUpdate()+"\n";

		//-- w/ holes -- old (slow) capacity update
		sc.getConfig().qsim().setUsingFastCapacityUpdate(false);
		sc.getConfig().qsim().setTrafficDynamics(TrafficDynamics.withHoles);

		startTime = System.currentTimeMillis();
		QSimUtils.createDefaultQSim(sc, events).run();
		endTime = System.currentTimeMillis();
		out = out + (endTime - startTime) +"\t"+sc.getConfig().qsim().getLinkDynamics()+"\t"+sc.getConfig().qsim().getTrafficDynamics()+"\t"+sc.getConfig().qsim().isUsingFastCapacityUpdate()+"\n";


		//-- w/o holes -- fast capacity update
		sc.getConfig().qsim().setTrafficDynamics(TrafficDynamics.queue);
		sc.getConfig().qsim().setUsingFastCapacityUpdate(true);

		startTime = System.currentTimeMillis();
		QSimUtils.createDefaultQSim(sc, events).run();
		endTime = System.currentTimeMillis();
		out = out + (endTime - startTime) +"\t"+sc.getConfig().qsim().getLinkDynamics()+"\t"+sc.getConfig().qsim().getTrafficDynamics()+"\t"+sc.getConfig().qsim().isUsingFastCapacityUpdate()+"\n";

		//-- w/ holes -- fast capacity update
		sc.getConfig().qsim().setTrafficDynamics(TrafficDynamics.withHoles);
		sc.getConfig().qsim().setUsingFastCapacityUpdate(true);

		startTime = System.currentTimeMillis();
		QSimUtils.createDefaultQSim(sc, events).run();
		endTime = System.currentTimeMillis();
		out = out + (endTime - startTime) +"\t"+sc.getConfig().qsim().getLinkDynamics()+"\t"+sc.getConfig().qsim().getTrafficDynamics()+"\t"+sc.getConfig().qsim().isUsingFastCapacityUpdate()+"\n";


		try {
			writer.write(out);
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: "
					+ e);
		}
	}

	private Scenario createBasicSceanrio(){
		/*
		 * can't use triangular race track set up for comparing the computational performance
		 * since, stability criteria can influence result in false indication.
		 * Thus, using equil network and plans.  
		 */

		String netFileName = "../../matsim/examples/equil/network.xml";

		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(netFileName);

		config.qsim().setMainModes(Arrays.asList("car","bike"));
		config.qsim().setUseDefaultVehicles(false);
		config.qsim().setStorageCapFactor(3.0);

		Scenario sc = ScenarioUtils.loadScenario(config);
		((ScenarioImpl)sc).createVehicleContainer();

		Map<String, VehicleType> mode2VehicleType = new HashMap<>();
		VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create(TransportMode.car,VehicleType.class));
		car.setMaximumVelocity(20);
		car.setPcuEquivalents(1.0);
		sc.getVehicles().addVehicleType(car);
		mode2VehicleType.put(TransportMode.car, car);

		VehicleType bike = VehicleUtils.getFactory().createVehicleType(Id.create(TransportMode.bike,VehicleType.class));
		bike.setMaximumVelocity(5);
		bike.setPcuEquivalents(0.25);
		sc.getVehicles().addVehicleType(bike);
		mode2VehicleType.put(TransportMode.bike, bike);

		for (int i =0;i<10000;i++){

			String mode;
			double endTime = 0;
			if(i%2==0) {
				mode = TransportMode.bike;
				endTime = 5*3600;
			}
			else {
				mode = TransportMode.car;
				endTime = 6*3600;
			}

			Id<Person> personId = Id.createPersonId(i+"_"+mode);
			Person p = sc.getPopulation().getFactory().createPerson(personId);
			Plan plan = sc.getPopulation().getFactory().createPlan();
			p.addPlan(plan);

			Id<Link> startLinkId = Id.createLinkId(1);
			Id<Link> endLinkId = Id.createLinkId(20);

			Activity home = sc.getPopulation().getFactory().createActivityFromLinkId("home", startLinkId);
			home.setEndTime(endTime);
			plan.addActivity( home );

			Leg leg = sc.getPopulation().getFactory().createLeg(mode);
			LinkNetworkRouteFactory factory = new LinkNetworkRouteFactory();

			NetworkRoute route = (NetworkRoute) factory.createRoute(startLinkId, endLinkId);
			route.setLinkIds(startLinkId, Arrays.asList(Id.createLinkId(6), Id.createLinkId(15)), endLinkId);
			leg.setRoute(route);
			plan.addLeg(leg);

			Activity work = sc.getPopulation().getFactory().createActivityFromLinkId("work", endLinkId);
			work.setEndTime(  endTime + 3600*5);
			plan.addActivity(work);

			leg = sc.getPopulation().getFactory().createLeg(mode);
			route = (NetworkRoute) factory.createRoute(endLinkId, startLinkId);
			route.setLinkIds(endLinkId, Arrays.asList(Id.createLinkId(21), Id.createLinkId(22), Id.createLinkId(23)), startLinkId);
			leg.setRoute(route);
			plan.addLeg(leg);

			plan.addActivity(home);

			Id<Vehicle> bikeVehicleId = Id.create(personId, Vehicle.class);
			Vehicle veh = VehicleUtils.getFactory().createVehicle(bikeVehicleId, mode2VehicleType.get(mode));

			sc.getPopulation().addPerson(p);
			sc.getVehicles().addVehicle(veh);
		}
		return sc;
	}
}
