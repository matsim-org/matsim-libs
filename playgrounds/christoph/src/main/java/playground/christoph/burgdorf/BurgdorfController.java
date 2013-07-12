/* *********************************************************************** *
 * project: org.matsim.*
 * BurgdorfController.java
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

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.network.NetworkImpl;
import org.matsim.withinday.controller.WithinDayController;
import org.matsim.withinday.replanning.identifiers.filter.CollectionAgentFilterFactory;
import org.matsim.withinday.replanning.identifiers.filter.LinkFilterFactory;
import org.matsim.withinday.replanning.identifiers.filter.TransportModeFilterFactory;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifierFactory;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplannerFactory;

import playground.christoph.burgdorf.withinday.identifiers.ParkingIdentifierFactory;
import playground.christoph.burgdorf.withinday.replanners.ParkingReplannerFactory;

/**
 *  
 * @author cdobler
 */
public class BurgdorfController extends WithinDayController {

//	private static String runId = "SamstagToBurgdorf";
	private static String runId = "SonntagFromBurgdorf";
	
	private DuringLegIdentifierFactory duringLegFactory;
	private DuringLegIdentifier parkingIdentifier;
	
	private CollectionAgentFilterFactory visitorAgentFilterFactory;
	private TransportModeFilterFactory carLegAgentsFilterFactory;
	private LinkFilterFactory linkFilterFactory;
		
	private boolean useWithinDayReplanning = false;
//	private boolean useWithinDayReplanning = true;
	
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
			final Controler controler = new BurgdorfController(args);
			controler.setOverwriteFiles(true);
			controler.getConfig().controler().setRunId(runId);
			controler.run();
		}
		System.exit(0);
	}
	
	public BurgdorfController(String[] args) {
		super(args);
	}
		
	@Override
	public void notifyStartup(StartupEvent event) {
		
		if (useWithinDayReplanning) {
			/*
			 * Initialize TravelTimeCollector.
			 */
			Set<String> analyzedModes = new HashSet<String>();
			analyzedModes.add(TransportMode.car);
			super.setModesAnalyzedByTravelTimeCollector(analyzedModes);
			
			super.setNumberOfReplanningThreads(this.config.global().getNumberOfThreads());
			
			super.notifyStartup(event);
		}
		
		/*
		 * If network capacities have to be adapted.
		 */
		if(reduceUpstreamCapacity) {
			NetworkImpl network = (NetworkImpl) scenarioData.getNetwork();
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
			networkChangeEvent.addLink(network.getLinks().get(scenarioData.createId(BurgdorfRoutes.burgdorfToBernUpstream)));
			networkChangeEvent.addLink(network.getLinks().get(scenarioData.createId(BurgdorfRoutes.burgdorfToZurichUpstream)));
			network.addNetworkChangeEvent(networkChangeEvent);
			
			// reset capacity
			networkChangeEvent = network.getFactory().createNetworkChangeEvent(resetCapacityTime);
//			changeValue = new ChangeValue(ChangeType.FACTOR, 2.0);
			changeValue = new ChangeValue(ChangeType.ABSOLUTE, 4000.0/3600.0);
			networkChangeEvent.setFlowCapacityChange(changeValue);
			changeValue = new ChangeValue(ChangeType.ABSOLUTE, 2);
			networkChangeEvent.setLanesChange(changeValue);
			networkChangeEvent.addLink(network.getLinks().get(scenarioData.createId(BurgdorfRoutes.burgdorfToBernUpstream)));
			networkChangeEvent.addLink(network.getLinks().get(scenarioData.createId(BurgdorfRoutes.burgdorfToZurichUpstream)));
			network.addNetworkChangeEvent(networkChangeEvent);
		}
	}
		
	private void initIdentifiers() {
		
		/*
		 * During Leg Identifiers
		 */		
		Set<String> duringLegRerouteTransportModes = new HashSet<String>();
		duringLegRerouteTransportModes.add(TransportMode.car);

		Set<Id> visitorAgents = new HashSet<Id>();
		for (Person person : scenarioData.getPopulation().getPersons().values()) {
			if (person.getId().toString().toLowerCase().contains("visitor")) visitorAgents.add(person.getId());
		}
		visitorAgentFilterFactory = new CollectionAgentFilterFactory(visitorAgents);
		
		carLegAgentsFilterFactory = new TransportModeFilterFactory(duringLegRerouteTransportModes);
		this.getMobsimListeners().add(carLegAgentsFilterFactory);			

		Set<Id> parkingDecisionLinks = new HashSet<Id>();
		for (String string : ParkingInfrastructure.parkingDecisionLinks) parkingDecisionLinks.add(scenarioData.createId(string));
		linkFilterFactory = new LinkFilterFactory(parkingDecisionLinks);
		this.getMobsimListeners().add(linkFilterFactory);
		
		duringLegFactory = new ParkingIdentifierFactory(this.getLinkReplanningMap());
		duringLegFactory.addAgentFilterFactory(visitorAgentFilterFactory);
		duringLegFactory.addAgentFilterFactory(carLegAgentsFilterFactory);
		duringLegFactory.addAgentFilterFactory(linkFilterFactory);
		this.parkingIdentifier = duringLegFactory.createIdentifier();
	}
	
	@Override
	protected void initReplanners(QSim qsim) {
		
		// initialize Identifiers and Replanners
		if (!useWithinDayReplanning) return;
		
		this.initIdentifiers();
		
		/*
		 * During Leg Replanner
		 */
		WithinDayDuringLegReplannerFactory duringLegReplannerFactory;
		
		duringLegReplannerFactory = new ParkingReplannerFactory(this.scenarioData, this.getWithinDayEngine());
		duringLegReplannerFactory.addIdentifier(this.parkingIdentifier);
		this.getWithinDayEngine().addDuringLegReplannerFactory(duringLegReplannerFactory);
	}
}