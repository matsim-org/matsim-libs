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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.EventsManager;
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
	
	public ComputationTimeCalculator(String outDir) {
		this.outDir = outDir;
	}

	public static void main(String[] args) {

		String outDir = "../../../repos/shared-svn/projects/mixedTraffic/computationalTime/";
		ComputationTimeCalculator calc = new ComputationTimeCalculator(outDir);

		calc.run(LinkDynamics.FIFO.name());
		calc.run(LinkDynamics.PassingQ.name());
		calc.run("seepage");
	}

	private BufferedWriter writer;
	private String outDir;

	private void openFile(String outFile){
		writer = IOUtils.getBufferedWriter(outFile);
		printLine("RunNr \t SimulationTime \n");
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
//			events.addHandler(new EventWriterXML(outDir+"/events.xml.gz"));
			processSceanarios(sc, events);

		} else {
			outDir = outDir+"seepage_";
			Scenario sc = createBasicSceanrio();
			sc.getConfig().qsim().setLinkDynamics(LinkDynamics.PassingQ.name()); // must for seepage

			sc.getConfig().setParam("seepage", "isSeepageAllowed", "true");
			sc.getConfig().setParam("seepage", "seepMode", "bike");
			sc.getConfig().setParam("seepage","isSeepModeStorageFree","false");

			EventsManager events = EventsUtils.createEventsManager();
			processSceanarios(sc, events);
		}
	}

	private void processSceanarios(Scenario sc, EventsManager events){

		//-- w/o holes -- old (slow) capacity update
		sc.getConfig().qsim().setTrafficDynamics(TrafficDynamics.queue);
		sc.getConfig().qsim().setUsingFastCapacityUpdate(false);

		openFile(outDir+"test.txt");
		printLine(sc.getConfig().qsim().getLinkDynamics()+"_"+sc.getConfig().qsim().getTrafficDynamics()+"_"+sc.getConfig().qsim().isUsingFastCapacityUpdate()+"\t");
		double sumTimeTaken = 0.;
		int countSum = 0;
		for (int i=0;i<26;i++){
			double startTime = System.currentTimeMillis();
			QSimUtils.createDefaultQSim(sc, events).run();
			double endTime = System.currentTimeMillis();
			if(i>5) {
				sumTimeTaken += ((endTime -startTime)/1000);
				countSum ++;
			}
		}
		printLine(countSum+"\t"+sumTimeTaken+"\n");
//		closeFile();

		//-- w/ holes -- old (slow) capacity update
		sc.getConfig().qsim().setUsingFastCapacityUpdate(false);
		sc.getConfig().qsim().setTrafficDynamics(TrafficDynamics.withHoles);
		
		printLine(sc.getConfig().qsim().getLinkDynamics()+"_"+sc.getConfig().qsim().getTrafficDynamics()+"_"+sc.getConfig().qsim().isUsingFastCapacityUpdate()+"\t");
//		openFile(outDir+sc.getConfig().qsim().getLinkDynamics()+"_"+sc.getConfig().qsim().getTrafficDynamics()+"_"+sc.getConfig().qsim().isUsingFastCapacityUpdate()+".txt");
		sumTimeTaken = 0.;
		countSum=0;
		for (int i=0;i<26;i++){
			double startTime = System.currentTimeMillis();
			QSimUtils.createDefaultQSim(sc, events).run();
			double endTime = System.currentTimeMillis();
			if(i>5) {
				sumTimeTaken += ((endTime -startTime)/1000);
				countSum ++;
			}
		}
		printLine(countSum+"\t"+sumTimeTaken+"\n");
//		closeFile();
		
		//-- w/o holes -- fast capacity update
		sc.getConfig().qsim().setTrafficDynamics(TrafficDynamics.queue);
		sc.getConfig().qsim().setUsingFastCapacityUpdate(true);
		
		printLine(sc.getConfig().qsim().getLinkDynamics()+"_"+sc.getConfig().qsim().getTrafficDynamics()+"_"+sc.getConfig().qsim().isUsingFastCapacityUpdate()+"\t");
//		openFile(outDir+sc.getConfig().qsim().getLinkDynamics()+"_"+sc.getConfig().qsim().getTrafficDynamics()+"_"+sc.getConfig().qsim().isUsingFastCapacityUpdate()+".txt");
		sumTimeTaken = 0.;
		countSum=0;
		for (int i=0;i<26;i++){
			double startTime = System.currentTimeMillis();
			QSimUtils.createDefaultQSim(sc, events).run();
			double endTime = System.currentTimeMillis();
			if(i>5) {
				sumTimeTaken += ((endTime -startTime)/1000);
				countSum ++;
			}
		}
		printLine(countSum+"\t"+sumTimeTaken+"\n");
//		closeFile();
		
		//-- w/ holes -- fast capacity update
		sc.getConfig().qsim().setTrafficDynamics(TrafficDynamics.withHoles);
		sc.getConfig().qsim().setUsingFastCapacityUpdate(true);
		
		printLine(sc.getConfig().qsim().getLinkDynamics()+"_"+sc.getConfig().qsim().getTrafficDynamics()+"_"+sc.getConfig().qsim().isUsingFastCapacityUpdate()+"\t");
		//		openFile(outDir+sc.getConfig().qsim().getLinkDynamics()+"_"+sc.getConfig().qsim().getTrafficDynamics()+"_"+sc.getConfig().qsim().isUsingFastCapacityUpdate()+".txt");
		sumTimeTaken = 0.;
		countSum=0;
		for (int i=0;i<26;i++){
			double startTime = System.currentTimeMillis();
			QSimUtils.createDefaultQSim(sc, events).run();
			double endTime = System.currentTimeMillis();
			if(i>5) {
				sumTimeTaken += ((endTime -startTime)/1000);
				countSum ++;
			}
		}
		printLine(countSum+"\t"+sumTimeTaken+"\n");
		closeFile();
	}

	private void printLine(String str){
		try {
			writer.write(str);
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: "+ e);
		}
	}

	private Scenario createBasicSceanrio(){

		Config config = ConfigUtils.createConfig();
		config.network().setInputFile("../../matsim/examples/equil/network.xml");

		config.qsim().setEndTime(36*3600);
		config.qsim().setMainModes(Arrays.asList("car","bike"));
		config.qsim().setUseDefaultVehicles(false);
		config.qsim().setFlowCapFactor(0.01);
		config.qsim().setStorageCapFactor(0.01);

		Scenario sc = ScenarioUtils.loadScenario(config);
		((ScenarioImpl)sc).createVehicleContainer();
		
//		NetworkImpl net = (NetworkImpl) sc.getNetwork();
//		
//		Node n1 =  net.createAndAddNode(Id.createNodeId(1), sc.createCoord(0, 0));
//		Node n2 =  net.createAndAddNode(Id.createNodeId(2), sc.createCoord(0, 100));
//		Node n3 =  net.createAndAddNode(Id.createNodeId(3), sc.createCoord(10000, 100));
//		Node n4 =  net.createAndAddNode(Id.createNodeId(4), sc.createCoord(15000, 50));
//		Node n5 =  net.createAndAddNode(Id.createNodeId(5), sc.createCoord(15000, 0));
//		Node n6 =  net.createAndAddNode(Id.createNodeId(6), sc.createCoord(14000, 0));
//		Node n7 =  net.createAndAddNode(Id.createNodeId(7), sc.createCoord(5000, 0));
//		
//		Link l1 = net.createAndAddLink(Id.createLinkId(1), n1, n2, 1000, 20, 1500, 1);
//		Link l2 = net.createAndAddLink(Id.createLinkId(2), n2, n3, 10000, 20, 2800, 1);
//		Link l3 = net.createAndAddLink(Id.createLinkId(3), n3, n4, 5000, 20, 2000, 1);
//		Link l4 = net.createAndAddLink(Id.createLinkId(4), n4, n5, 1000, 20, 2000, 1);
//		Link l5 = net.createAndAddLink(Id.createLinkId(5), n5, n6, 1000, 20, 1500, 1);
//		Link l6 = net.createAndAddLink(Id.createLinkId(6), n6, n7, 5000, 20, 2800, 1);
//		Link l7 = net.createAndAddLink(Id.createLinkId(7), n7, n1, 5000, 20, 2800, 1);
		
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

		int noOfPerson = 10000;
		
		for (int i =0; i< noOfPerson; i++){

			String mode;
			double endTime = 0;
			double dur = 0;
			if(i%2==0) {
				mode = TransportMode.bike;
				endTime = 0*3600+i; 
				dur = 3*3600;
			}
			else {
				mode = TransportMode.car;
				endTime = 0*3600+1.2*i;
				dur = 4*3600;
			}

			Id<Person> personId = Id.createPersonId(i+"_"+mode);
			Person p = sc.getPopulation().getFactory().createPerson(personId);
			Plan plan = sc.getPopulation().getFactory().createPlan();
			p.addPlan(plan);

			Id<Link> startLinkId = Id.createLinkId(1);//l1.getId();
			Id<Link> endLinkId = Id.createLinkId(20);//l4.getId();

			Activity home = sc.getPopulation().getFactory().createActivityFromLinkId("home", startLinkId);
			home.setEndTime(endTime);
			plan.addActivity( home );

			Leg leg = sc.getPopulation().getFactory().createLeg(mode);
			LinkNetworkRouteFactory factory = new LinkNetworkRouteFactory();

			NetworkRoute route = (NetworkRoute) factory.createRoute(startLinkId, endLinkId);
			
			List<Id<Link>> routesList = new ArrayList<Id<Link>>();
			
			if(i<noOfPerson/9) routesList = Arrays.asList(Id.createLinkId(2),Id.createLinkId(11));
			else if(i< 2*noOfPerson/9) routesList = Arrays.asList(Id.createLinkId(3),Id.createLinkId(12));
			else if(i< 3*noOfPerson/9) routesList = Arrays.asList(Id.createLinkId(4),Id.createLinkId(13));
			else if(i< 4*noOfPerson/9) routesList = Arrays.asList(Id.createLinkId(5),Id.createLinkId(14));
			else if(i< 5*noOfPerson/9) routesList = Arrays.asList(Id.createLinkId(6),Id.createLinkId(15));
			else if(i< 6*noOfPerson/9) routesList = Arrays.asList(Id.createLinkId(7),Id.createLinkId(16));
			else if(i< 7*noOfPerson/9) routesList = Arrays.asList(Id.createLinkId(8),Id.createLinkId(17));
			else if(i< 8*noOfPerson/9) routesList = Arrays.asList(Id.createLinkId(9),Id.createLinkId(18));
			else 					   routesList = Arrays.asList(Id.createLinkId(10),Id.createLinkId(19));
			
			route.setLinkIds(startLinkId, routesList, endLinkId);
			leg.setRoute(route);
			plan.addLeg(leg);

			Activity work = sc.getPopulation().getFactory().createActivityFromLinkId("work", endLinkId);
			work.setEndTime(  endTime + dur);
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
