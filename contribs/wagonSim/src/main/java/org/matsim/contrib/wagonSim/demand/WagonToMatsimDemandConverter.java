/* *********************************************************************** *
 * project: org.matsim.*                                                   *
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

/**
 * 
 */
package org.matsim.contrib.wagonSim.demand;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.wagonSim.WagonSimConstants;
import org.matsim.contrib.wagonSim.demand.WagonDataContainer.Wagon;
import org.matsim.core.config.Config;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterImplFactory;
import org.matsim.utils.objectattributes.ObjectAttributes;

/**
 * @author balmermi
 * @since 2013-08-19
 */
public class WagonToMatsimDemandConverter {

	//////////////////////////////////////////////////////////////////////
	// variables
	//////////////////////////////////////////////////////////////////////
	
	private final Scenario scenario;
	private final ObjectAttributes wagonAttributes;
	private final Map<String, Id<Node>> zoneToNodeMap;
	private final ObjectAttributes transitVehicleAttributes;

	private static final Logger log = Logger.getLogger(WagonToMatsimDemandConverter.class);

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public WagonToMatsimDemandConverter(Scenario scenario, ObjectAttributes wagonAttributes, ObjectAttributes transitVehicleAttributes, Map<String, Id<Node>> zoneToNodeMap) {
		this.scenario = scenario;
		this.wagonAttributes = wagonAttributes;
		this.transitVehicleAttributes = transitVehicleAttributes;
		this.zoneToNodeMap = zoneToNodeMap;
	}

	//////////////////////////////////////////////////////////////////////
	// methods
	//////////////////////////////////////////////////////////////////////
	
	private final <T> void printIdSet(Set<T> ids, String desc) {
		System.out.println("--- START printing "+ids.size()+" "+desc+" ids ---");
		for (T id : ids) { System.out.println(id.toString()); }
		System.out.println("--- END   printing "+ids.size()+" "+desc+" ids ---");
	}

	//////////////////////////////////////////////////////////////////////
	
	public final void convert(WagonDataContainer dataContainer) {

		Config config = scenario.getConfig();
		Population population = scenario.getPopulation();
		PopulationFactory factory = population.getFactory();

//		WagonSimVehicleLoadListener listener = new WagonSimVehicleLoadListener(wagonAttributes);
//		TransitRouterFactory routerFactory = new WagonSimRouterFactoryImpl(
//				listener,
//				scenario.getTransitSchedule(),
//				new TransitRouterConfig(config.planCalcScore(), config.plansCalcRoute(), config.transitRouter(), config.vspExperimental()),
//				wagonAttributes,
//				transitVehicleAttributes);
//
//		TransitRouter router = routerFactory.createTransitRouter();
		
		TransitRouter router = new TransitRouterImplFactory(
				scenario.getTransitSchedule(),
				new TransitRouterConfig(scenario.getConfig())).get();

		
		Set<String> fromZonesNotConnected = new HashSet<>();
		Set<String> toZonesNotConnected = new HashSet<>();
		Set<Id<Node>> fromNodesNotInPseudoNetwork = new HashSet<>();
		Set<Id<Node>> toNodesNotInPseudoNetwork = new HashSet<>();
		Set<Id<Node>> fromNodesInPseudoNetwork = new HashSet<>();
		Set<Id<Node>> toNodesInPseudoNetwork = new HashSet<>();
		Set<Id<Person>> wagonWithoutRoute = new HashSet<>();
		int nofWagonsMissed = 0;
		
		for (Wagon wagon : dataContainer.wagons.values()) {
			Id<Node> fromNodeId = zoneToNodeMap.get(wagon.fromZoneId);
			Id<Node> toNodeId = zoneToNodeMap.get(wagon.toZoneId);

			if (fromNodeId == null) { fromZonesNotConnected.add(wagon.fromZoneId); nofWagonsMissed++; continue; }
			if (toNodeId == null) { toZonesNotConnected.add(wagon.toZoneId); nofWagonsMissed++; continue; }
			
			if (!scenario.getNetwork().getNodes().keySet().contains(fromNodeId)) { fromNodesNotInPseudoNetwork.add(fromNodeId); nofWagonsMissed++; continue; }
			if (!scenario.getNetwork().getNodes().keySet().contains(toNodeId)) { toNodesNotInPseudoNetwork.add(toNodeId); nofWagonsMissed++; continue; }

			fromNodesInPseudoNetwork.add(fromNodeId);
			toNodesInPseudoNetwork.add(toNodeId);
			
			Person person = factory.createPerson(Id.create(wagon.id, Person.class));
			Plan plan = factory.createPlan();
			person.addPlan(plan);
			Activity origin = factory.createActivityFromCoord(WagonSimConstants.ORIGIN,scenario.getNetwork().getNodes().get(fromNodeId).getCoord());
			origin.setEndTime(wagon.depTime);
			plan.addActivity(origin);
			Leg leg = factory.createLeg(TransportMode.pt);
			plan.addLeg(leg);
			Activity destination = factory.createActivityFromCoord(WagonSimConstants.DESTINATION,scenario.getNetwork().getNodes().get(toNodeId).getCoord());
			plan.addActivity(destination);

			if (router.calcRoute(origin.getCoord(),destination.getCoord(),wagon.depTime,person) != null) {
				population.addPerson(person);
				wagonAttributes.putAttribute(person.getId().toString(),WagonSimConstants.WAGON_GROSS_WEIGHT,wagon.weight+wagon.weightLoad);
				wagonAttributes.putAttribute(person.getId().toString(),WagonSimConstants.WAGON_LENGTH,wagon.length);
			}
			else { wagonWithoutRoute.add(person.getId()); }
		}
		
		printIdSet(fromZonesNotConnected,"not connected from-zone");
		printIdSet(toZonesNotConnected,"not connected to-zone");
		Set<String> zonesNotConnected = new HashSet<>(fromZonesNotConnected); zonesNotConnected.addAll(toZonesNotConnected); printIdSet(zonesNotConnected,"not connected zone");
		printIdSet(fromNodesNotInPseudoNetwork,"not defined from-node");
		printIdSet(toNodesNotInPseudoNetwork,"not defined to-node");
		Set<Id<Node>> nodesNotInPseudoNetwork = new HashSet<>(fromNodesNotInPseudoNetwork); nodesNotInPseudoNetwork.addAll(toNodesNotInPseudoNetwork); printIdSet(nodesNotInPseudoNetwork,"not defined node");
		printIdSet(fromNodesInPseudoNetwork,"demand origin node");
		printIdSet(toNodesInPseudoNetwork,"demand destination node");
		Set<Id<Node>> nodesInPseudoNetwork = new HashSet<>(fromNodesInPseudoNetwork); nodesInPseudoNetwork.addAll(toNodesInPseudoNetwork); printIdSet(nodesInPseudoNetwork,"demand origin or destination node");
		printIdSet(wagonWithoutRoute,"no route wagon");
		
		log.info("number of wagons in the container: "+dataContainer.wagons.size());
		log.info("number of agents (wagons) created: "+population.getPersons().size());
		log.info("number of agents (wagons) missed: "+nofWagonsMissed+" ("+wagonWithoutRoute.size()+" of them do not find a route)");
		log.info("number of from-zones not mapped to a node: "+fromZonesNotConnected.size());
		log.info("number of to-zones not mapped to a node: "+toZonesNotConnected.size());
		log.info("number of zones not mapped to a node: "+zonesNotConnected.size());
		log.info("number of from-Nodes not defined in the schedule (OTT Data): "+fromNodesNotInPseudoNetwork.size());
		log.info("number of to-Nodes not defined in the schedule (OTT Data): "+toNodesNotInPseudoNetwork.size());
		log.info("number of nodes not defined in the schedule (OTT Data): "+nodesNotInPseudoNetwork.size());
		log.info("number of Nodes as demand origin: "+fromNodesInPseudoNetwork.size());
		log.info("number of Nodes as demand destination: "+fromNodesInPseudoNetwork.size());
		log.info("number of Nodes as demand origin or destination: "+nodesInPseudoNetwork.size());
	}
}
