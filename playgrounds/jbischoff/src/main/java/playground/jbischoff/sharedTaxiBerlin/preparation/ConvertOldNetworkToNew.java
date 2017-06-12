/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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
package playground.jbischoff.sharedTaxiBerlin.preparation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.contrib.dvrp.data.FleetImpl;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.data.VehicleImpl;
import org.matsim.contrib.dvrp.data.file.VehicleReader;
import org.matsim.contrib.dvrp.data.file.VehicleWriter;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkChangeEventsParser;
import org.matsim.core.network.io.NetworkChangeEventsWriter;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingActivityCosts.Time;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class ConvertOldNetworkToNew {

	Map<Id<Link>,Id<Link>> old2newId = new HashMap<>();
	String folder = "C:/Users/Joschka/Documents/shared-svn/projects/sustainability-w-michal-and-dlr/data/scenarios/drt/oldnetwork/";
	Network oldNet = NetworkUtils.createNetwork();
	Network newNet = NetworkUtils.createNetwork();;
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new ConvertOldNetworkToNew().run();

	}
	/**
	 * 
	 */
	private void run() {
		new MatsimNetworkReader(oldNet).readFile(folder+"berlin_brb.xml.gz");
		new MatsimNetworkReader(newNet).readFile(folder+"network_shortIds_v1.xml.gz");
		readMatching(folder+"shortIds.txt");
		convertTaxis(folder+"taxis4to4_EV0.0.xml",folder+"new_net.taxis4to4_EV0.0.xml");
		convertNetworkChangeEvents(folder+"changeevents.xml.gz",folder+"new_net.changeevents.xml.gz");
		convertPlans(folder+"plans4to3_1.0.xml.gz", folder + "new_net.plans4to3_1.0.xml.gz");
		convertPlans(folder+"plans4to3_1.5.xml.gz", folder + "new_net.plans4to3_1.5.xml.gz");
		convertPlans(folder+"plans4to3_2.0.xml.gz", folder + "new_net.plans4to3_2.0.xml.gz");
	}
	/**
	 * @param string
	 * @param string2
	 */
	private void convertPlans(String oldPlans, String newPlans) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenario).readFile(oldPlans);
		for (Person p : scenario.getPopulation().getPersons().values()){
			try{
			Plan plan = p.getSelectedPlan();
			Activity act0 = (Activity) plan.getPlanElements().get(0);
			Leg leg0 = (Leg) plan.getPlanElements().get(1);
			Activity act1 = (Activity) plan.getPlanElements().get(2);
			act0.setLinkId(null);
			leg0.setRoute(null);
			leg0.setDepartureTime(Time.UNDEFINED);
			act1.setLinkId(null);}
		
			
				catch (NullPointerException e){
					e.printStackTrace();
					System.err.println(p.getId());
			}
			
		}
		new PopulationWriter(scenario.getPopulation()).write(newPlans);
	}
	/**
	 * @param string
	 * @param string2
	 */
	private void convertNetworkChangeEvents(String oldFile, String newFile) {
		
		List<NetworkChangeEvent> changeEvents = new ArrayList<>() ;
		NetworkChangeEventsParser parser = new NetworkChangeEventsParser(oldNet,changeEvents);
		parser.readFile(oldFile);
		for (NetworkChangeEvent event : changeEvents){
			Set<Link> newLinks = new HashSet<>();
			Set<Link> oldLinks = new HashSet<>();
			for (Link l : event.getLinks()){
				newLinks.add(newNet.getLinks().get(old2newId.get(l.getId())));
				oldLinks.add(l);
			}
			for (Link o : oldLinks){
			event.removeLink(o);
			}
			for (Link n : newLinks){
				event.addLink(n);;
			}
		}
		new NetworkChangeEventsWriter().write(newFile, changeEvents);

	}
	/**
	 * @param string
	 * @param string2
	 */
	private void convertTaxis(String oldfile, String newfile) {
		FleetImpl oldFleet = new FleetImpl();
		FleetImpl newFleet = new FleetImpl();
		new VehicleReader(oldNet, oldFleet).readFile(oldfile);
		for (Vehicle v: oldFleet.getVehicles().values()){
			Link newLink = newNet.getLinks().get(old2newId.get(v.getStartLink().getId()));
			if (newLink == null) {
				System.out.println("hoop");
				
			}
			else {
			Vehicle newVehicle = new VehicleImpl(v.getId(), newLink, v.getCapacity(), v.getServiceBeginTime(), v.getServiceEndTime());
			newFleet.addVehicle(newVehicle);
			}
		}
		new VehicleWriter(newFleet.getVehicles().values()).write(newfile);
		
	}
	/**
	 * @param string
	 */

	private void readMatching(String file) {
		TabularFileParserConfig tb = new TabularFileParserConfig();
		tb.setDelimiterRegex(";");
		tb.setFileName(file);
		TabularFileParser tfp = new TabularFileParser();
		tfp.parse(tb, new TabularFileHandler() {
			
			@Override
			public void startRow(String[] row) {
				Id<Link> old = Id.createLinkId(row[0]);
				Id<Link> newL = Id.createLinkId(row[1]);
				old2newId.put(old, newL);
			}
		});
	}
	
	
}
