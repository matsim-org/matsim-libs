/* *********************************************************************** *
 * project: org.matsim.*
 * BurgdorfRunner.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.christoph.burgdorf;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.util.MultiNodeDijkstraFactory;
import org.matsim.withinday.controller.WithinDayControlerListener;
import org.matsim.withinday.replanning.identifiers.filter.CollectionAgentFilterFactory;
import org.matsim.withinday.replanning.identifiers.filter.LinkFilterFactory;
import org.matsim.withinday.replanning.identifiers.filter.TransportModeFilterFactory;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegAgentSelector;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifierFactory;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplannerFactory;
import playground.christoph.burgdorf.withinday.identifiers.ParkingIdentifierFactory;
import playground.christoph.burgdorf.withinday.replanners.ParkingReplannerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 *  
 * @author cdobler
 */
public class BurgdorfRunner implements StartupListener {

//	private static String runId = "SamstagToBurgdorf";
	private static String runId = "SonntagFromBurgdorf";
	
	private WithinDayControlerListener withinDayControlerListener;
	
	private DuringLegIdentifierFactory duringLegFactory;
	private DuringLegAgentSelector parkingIdentifier;
	
	private CollectionAgentFilterFactory visitorAgentFilterFactory;
	private TransportModeFilterFactory carLegAgentsFilterFactory;
	private LinkFilterFactory linkFilterFactory;
		
//	private boolean useWithinDayReplanning = false;
	private boolean useWithinDayReplanning = true;
	
//	private boolean reduceUpstreamCapacity = false;
	private boolean reduceUpstreamCapacity = true;
	
	private double reduceCapacityTime = 17 * 3600;
	private double resetCapacityTime = 20 * 3600;
	
	/*
	 * ===================================================================
	 * main
	 * ===================================================================
	 */
	public static void main(final String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: BurgdorfController config-file [dtd-file]");
			System.out.println();
		} else {
			final Controler controler = new Controler(args);
			controler.getConfig().controler().setOverwriteFileSetting(
					true ?
							OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
							OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
			controler.getConfig().controler().setRunId(runId);
			controler.addControlerListener(new BurgdorfRunner(controler));
			controler.run();
		}
		System.exit(0);
	}
	
	public BurgdorfRunner(Controler controler) {
		
		this.withinDayControlerListener = new WithinDayControlerListener();
		
		// workaround
		this.withinDayControlerListener.setLeastCostPathCalculatorFactory(new MultiNodeDijkstraFactory());
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		
		if (useWithinDayReplanning) {
			// Set analyzed modes for TravelTimeCollector.
			Set<String> analyzedModes = new HashSet<String>();
			analyzedModes.add(TransportMode.car);
			this.withinDayControlerListener.setModesAnalyzedByTravelTimeCollector(analyzedModes);

			// initialze within-day module
			this.withinDayControlerListener.notifyStartup(event);
			
			this.initIdentifiers(event.getControler().getScenario());
			this.initReplanners(event.getControler().getScenario());
		}
		
		/*
		 * If network capacities have to be adapted.
		 */
		if(reduceUpstreamCapacity) {
            NetworkImpl network = (NetworkImpl) event.getControler().getScenario().getNetwork();
			ChangeValue changeValue;
			NetworkChangeEvent networkChangeEvent;

//			network.getLinks().get(scenarioData.createId(BurgdorfRoutes.burgdorfToBernUpstream)).setCapacity(200);
			
			// reduce capacity
			networkChangeEvent = network.getFactory().createNetworkChangeEvent(reduceCapacityTime);
//			changeValue = new ChangeValue(ChangeType.FACTOR, 0.5);
			changeValue = new ChangeValue(ChangeType.ABSOLUTE, 500.0/3600.0);
			networkChangeEvent.setFlowCapacityChange(changeValue);
			changeValue = new ChangeValue(ChangeType.ABSOLUTE, 1);
			networkChangeEvent.setLanesChange(changeValue);
			networkChangeEvent.addLink(network.getLinks().get(Id.create(BurgdorfRoutes.burgdorfToBernUpstream, Link.class)));
			networkChangeEvent.addLink(network.getLinks().get(Id.create(BurgdorfRoutes.burgdorfToZurichUpstream, Link.class)));
			network.addNetworkChangeEvent(networkChangeEvent);
			
			// reset capacity
			networkChangeEvent = network.getFactory().createNetworkChangeEvent(resetCapacityTime);
//			changeValue = new ChangeValue(ChangeType.FACTOR, 2.0);
			changeValue = new ChangeValue(ChangeType.ABSOLUTE, 4000.0/3600.0);
			networkChangeEvent.setFlowCapacityChange(changeValue);
			changeValue = new ChangeValue(ChangeType.ABSOLUTE, 2);
			networkChangeEvent.setLanesChange(changeValue);
			networkChangeEvent.addLink(network.getLinks().get(Id.create(BurgdorfRoutes.burgdorfToBernUpstream, Link.class)));
			networkChangeEvent.addLink(network.getLinks().get(Id.create(BurgdorfRoutes.burgdorfToZurichUpstream, Link.class)));
			network.addNetworkChangeEvent(networkChangeEvent);
		}
	}
		
	private void initIdentifiers(Scenario scenario) {
		
		/*
		 * During Leg Identifiers
		 */		
		Set<String> duringLegRerouteTransportModes = new HashSet<String>();
		duringLegRerouteTransportModes.add(TransportMode.car);

		Set<Id<Person>> visitorAgents = new HashSet<>();
		for (Person person : scenario.getPopulation().getPersons().values()) {
			if (person.getId().toString().toLowerCase().contains("visitor")) visitorAgents.add(person.getId());
		}
		visitorAgentFilterFactory = new CollectionAgentFilterFactory(visitorAgents);
		
		carLegAgentsFilterFactory = new TransportModeFilterFactory(duringLegRerouteTransportModes, 
				this.withinDayControlerListener.getMobsimDataProvider());

		Set<Id<Link>> parkingDecisionLinks = new HashSet<>();
		for (String string : ParkingInfrastructure.parkingDecisionLinks) parkingDecisionLinks.add(Id.create(string, Link.class));
		linkFilterFactory = new LinkFilterFactory(parkingDecisionLinks, this.withinDayControlerListener.getMobsimDataProvider());
		
		duringLegFactory = new ParkingIdentifierFactory(this.withinDayControlerListener.getLinkReplanningMap(),
				this.withinDayControlerListener.getMobsimDataProvider());
		duringLegFactory.addAgentFilterFactory(visitorAgentFilterFactory);
		duringLegFactory.addAgentFilterFactory(carLegAgentsFilterFactory);
		duringLegFactory.addAgentFilterFactory(linkFilterFactory);
		this.parkingIdentifier = duringLegFactory.createIdentifier();
	}
	
	private void initReplanners(Scenario scenario) {	
		/*
		 * During Leg Replanner
		 */
		WithinDayDuringLegReplannerFactory duringLegReplannerFactory;
		
		duringLegReplannerFactory = new ParkingReplannerFactory(scenario, this.withinDayControlerListener.getWithinDayEngine());
		duringLegReplannerFactory.addIdentifier(this.parkingIdentifier);
		this.withinDayControlerListener.getWithinDayEngine().addDuringLegReplannerFactory(duringLegReplannerFactory);
	}
}