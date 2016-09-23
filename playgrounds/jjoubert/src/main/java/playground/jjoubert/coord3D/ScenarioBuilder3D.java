/* *********************************************************************** *
 * project: org.matsim.*
 * ScenarioBuilder3D.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.jjoubert.coord3D;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.core.utils.misc.Time;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

import playground.southafrica.utilities.Header;

/**
 * Class to build all the necessary scenario components to run a 3D MATSim
 * model.
 * 
 * @author jwjoubert
 */
public class ScenarioBuilder3D {
	final private static Logger LOG = Logger.getLogger(ScenarioBuilder3D.class);
	
	public static void main(String[] args){
		Header.printHeader(ScenarioBuilder3D.class.toString(), args);
		
		String path = args[0];
		path += path.endsWith("/") ? "" : "/";
		
		int numberOfPersons = Integer.parseInt(args[1]);
		
		MatsimRandom.reset(20160728001l);
		buildScenario(path, numberOfPersons, MatsimRandom.getLocalInstance().nextLong(), MatsimRandom.getLocalInstance().nextInt());
		
		Header.printFooter();
	}

	/**
	 * Hidden constructor.
	 */
	private ScenarioBuilder3D(){
		/* Hide the constructor. */
	}
	
	/**
	 * Builds an equil-type network but with third dimension, elevation, and
	 * writes the scenario to files.
	 * 
	 * @param folder
	 * @return
	 */
	private static Scenario buildScenario(String folder, int numberOfPersons, long seed, int run){
		MatsimRandom.reset(seed*run);
		Scenario sc = Utils3D.elevateEquilNetwork();
		sc = buildPersonsWithPlans(sc, numberOfPersons);
		sc = buildVehicles(sc);
		
		if(folder != null){
			folder += folder.endsWith("/") ? "" : "/";
			new File(folder + "matsim/").mkdirs();
			new NetworkWriter(sc.getNetwork()).write(folder + "matsim/network.xml.gz");
			new PopulationWriter(sc.getPopulation()).write(folder + "matsim/population.xml.gz");
			new ObjectAttributesXmlWriter(sc.getPopulation().getPersonAttributes()).writeFile(folder + "matsim/populationAttributes.xml.gz");
			new VehicleWriterV1(sc.getVehicles()).writeFile(folder + "matsim/vehicles.xml.gz");
			
			write3dEquilNetwork(folder, sc);
		}

		return sc;
	}
	
	
	public static Scenario buildScenario(int numberOfPersons, long seed, int run){
		return buildScenario(null, numberOfPersons, seed, run);
	}
	
	
	public static Scenario buildScenario(int numberOfPersons){
		return buildScenario(null, numberOfPersons, MatsimRandom.getLocalInstance().nextLong(), MatsimRandom.getLocalInstance().nextInt());
	}
	
	
	private static Scenario buildPersonsWithPlans(Scenario sc, int numberOfPlans){
		PopulationFactory pf = sc.getPopulation().getFactory();
		for(int i = 0; i < numberOfPlans; i++){
			/* Create the plan. */
			Plan plan = pf.createPlan();
			
			Activity a1 = pf.createActivityFromLinkId("a1", Id.createLinkId("1"));
			a1.setEndTime(i*Time.parseTime("00:01:00"));
			plan.addActivity(a1);
			
			Leg l1 = pf.createLeg("car");
			Route sampledRoute = sampleRandomRoute(sc);
			l1.setRoute(sampledRoute);
			plan.addLeg(l1);
			
			Activity a2 = pf.createActivityFromLinkId("a2", Id.createLinkId("20"));
			a2.setMaximumDuration(Time.parseTime("01:00:00"));
			plan.addActivity(a2);
			
			Leg l2 = pf.createLeg("car");
			List<Id<Link>> returnList = new ArrayList<>();
			returnList.add(Id.createLinkId("20"));
			returnList.add(Id.createLinkId("21"));
			returnList.add(Id.createLinkId("22"));
			returnList.add(Id.createLinkId("23"));
			returnList.add(Id.createLinkId("1"));
			Route returnRoute = RouteUtils.createNetworkRoute(returnList, sc.getNetwork());
			returnRoute.setDistance(65000.);
			l2.setRoute(returnRoute );
			plan.addLeg(l2);
			
			Activity a3 = pf.createActivityFromLinkId("a1", Id.createLinkId("1"));
			plan.addActivity(a3);
			
			/* Create the person and add the plan. */
			Person person = pf.createPerson(Id.createPersonId(i));
			person.addPlan(plan);
			sc.getPopulation().addPerson(person);
		}
		
		return sc;
	}
	
	private static Route sampleRandomRoute(Scenario sc){
		List<Id<Link>> linkIds = new ArrayList<Id<Link>>();

		/* Add the first link. */
		linkIds.add(Id.createLinkId("1"));
		
		/* Add the next two links based on some random sample. */
		int routeChoice = MatsimRandom.getLocalInstance().nextInt(9);
		switch (routeChoice) {
		case 0:
			linkIds.add(Id.createLinkId("2"));
			linkIds.add(Id.createLinkId("11"));
			break;
		case 1:
			linkIds.add(Id.createLinkId("3"));
			linkIds.add(Id.createLinkId("12"));
			break;
		case 2:
			linkIds.add(Id.createLinkId("4"));
			linkIds.add(Id.createLinkId("13"));
			break;
		case 3:
			linkIds.add(Id.createLinkId("5"));
			linkIds.add(Id.createLinkId("14"));
			break;
		case 4:
			linkIds.add(Id.createLinkId("6"));
			linkIds.add(Id.createLinkId("15"));
			break;
		case 5:
			linkIds.add(Id.createLinkId("7"));
			linkIds.add(Id.createLinkId("16"));
			break;
		case 6:
			linkIds.add(Id.createLinkId("8"));
			linkIds.add(Id.createLinkId("17"));
			break;
		case 7:
			linkIds.add(Id.createLinkId("9"));
			linkIds.add(Id.createLinkId("18"));
			break;
		case 8:
			linkIds.add(Id.createLinkId("10"));
			linkIds.add(Id.createLinkId("19"));
			break;
		default:
			break;
		}
		
		/* Add the final link */
		linkIds.add(Id.createLinkId("20"));
		
		Route route = RouteUtils.createNetworkRoute(linkIds, sc.getNetwork());
		route.setDistance(25000);
		return route;
	}
	
	
	/**
	 * For each {@link Person} a {@link Vehicle} is added with the same 
	 * {@link Id} as the {@link Person}. The {@link VehicleType} is randomly
	 * drawn from {@link VehicleType3D} with each type having equal probability.  
	 * @param sc
	 * @return
	 */
	private static Scenario buildVehicles(Scenario sc){
		Vehicles vehicles = sc.getVehicles();
		
		/* Add all the vehicle types. */
		for(VehicleType3D vt : VehicleType3D.values()){
			vehicles.addVehicleType(vt.getVehicleType());
		}
		
		/* Randomly sample a vehicle type for each person. */
		for(Person p : sc.getPopulation().getPersons().values()){
			
			/* Sample random vehicle type */
			VehicleType vehicleType = null;
			double r = MatsimRandom.getLocalInstance().nextDouble();
			if(r <= 0.5){
				vehicleType = VehicleType3D.A.getVehicleType();
			} else{
				vehicleType = VehicleType3D.B.getVehicleType();
			}
			
			/* Create the vehicle. */
			Vehicle vehicle = VehicleUtils.getFactory().createVehicle(Id.createVehicleId(p.getId().toString()), vehicleType);
			vehicles.addVehicle(vehicle);
			
			/* Link the vehicle type to the person. */
			sc.getPopulation().getPersonAttributes().putAttribute(p.getId().toString(), "vehicleType", vehicleType.getId().toString());
		}
		
		return sc;
	}
	
	/**
	 * For visualisation purposes (in R), write the network links to CSV file.
	 * 
	 * @param path
	 * @param sc
	 */
	private static void write3dEquilNetwork(String path, Scenario sc){
		LOG.info("Writing the elevation network to CSV file...");
		BufferedWriter bw = IOUtils.getBufferedWriter(path + "data/equil_elevation.csv.gz");
		Counter counter = new Counter("  link # ");
		ObjectAttributes linkAttributes = new ObjectAttributes();
		try{
			bw.write("lid,fx,fy,fz,tx,ty,tz,length,grade");
			bw.newLine();
			for(Link l : sc.getNetwork().getLinks().values()){
				Coord cf = l.getFromNode().getCoord();
				Coord ct = l.getToNode().getCoord();
				
				double grade = Utils3D.calculateGrade(l);
				linkAttributes.putAttribute(l.getId().toString(), "grade", grade);
				
				String line = String.format("%s,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.1f,%.6f\n", 
						l.getId().toString(),
						cf.getX(), cf.getY(), cf.getZ(),
						ct.getX(), ct.getY(), ct.getZ(),
						l.getLength(),
						grade);
				bw.write(line);
				counter.incCounter();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to elevation file.");
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close elevation file.");
			}
		}
		counter.printCounter();
	}

	
	
	public enum VehicleType3D{
		A(7.0, 100.0, 1.0),
		B(12.0, 90.0, 1.25),
		C(20.0, 90.0, 1.5);
		
		private final double length;
		private final double maxVelocity;
		private final double gradePenalty;
		
		
		VehicleType3D(double length, double maxVelocity, double gradePenalty){
			this.length = length;
			this.maxVelocity = maxVelocity;
			this.gradePenalty = gradePenalty;
		}
		
		public VehicleType getVehicleType(){
			VehicleType type = new VehicleTypeImpl(Id.create(this.name(), VehicleType.class));
			type.setLength(this.length);
			type.setMaximumVelocity(this.maxVelocity / 3.6);
			return type;
		}
		
		public double getPenalty(double grade){
			switch (this) {
			case A:
				return 1.0;
			case B:
				return 1.25;
			case C:
				return 1.5;
			default:
				break;
			}
			
			return 1.0;
		}
	}
}


