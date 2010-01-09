/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.mzilske.bvg09;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.pt.Umlauf;
import org.matsim.pt.UmlaufInterpolator;
import org.matsim.pt.UmlaufStueckI;
import org.matsim.transitSchedule.TransitScheduleReaderV1;
import org.matsim.transitSchedule.TransitScheduleWriterV1;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitScheduleWriter;
import org.matsim.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.BasicVehicle;
import org.matsim.vehicles.BasicVehicleCapacity;
import org.matsim.vehicles.BasicVehicleCapacityImpl;
import org.matsim.vehicles.BasicVehicleType;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.VehiclesFactory;
import org.xml.sax.SAXException;

import playground.mzilske.pt.queuesim.GreedyUmlaufBuilderImpl;

public class NullFallFacilityRollout {

	private static final Logger log = Logger.getLogger(NullFallFacilityRollout.class);

	private static String path = "../berlin-bvg09/pt/nullfall_alles/";
	
	private static String InNetworkFile = path + "intermediateNetwork.xml";
	private static String InTransitScheduleFile = path + "intermediateTransitSchedule.xml";
	private static String OutNetworkFile = path + "network.xml";
	private static String OutTransitScheduleFile = path + "transitSchedule.xml";
	private static String OutVehicleFile = path + "vehicles.xml";
	private final ScenarioImpl inScenario;
	private final Config inConfig;
	private final ScenarioImpl outScenario;
	private final Config outConfig;
	private Map<TransitStopFacility, Map<Link, TransitStopFacility>> transitStopInLinks = new HashMap<TransitStopFacility, Map<Link, TransitStopFacility>>();

	private Collection<Umlauf> umlaeufe;

	

	public static void main(final String[] args) {
		rollOutFacilitiesAndAssignVehicles();
	}

	public NullFallFacilityRollout() {
		this.inScenario = new ScenarioImpl();
		this.inConfig = this.inScenario.getConfig();
		this.outScenario = new ScenarioImpl();
		this.outConfig = this.outScenario.getConfig();
	}

	private static void rollOutFacilitiesAndAssignVehicles() {
		NullFallFacilityRollout app = new NullFallFacilityRollout();
		app.prepareConfig();
		app.loadData();
		app.copyRoutes();
		app.enterFacilities();
		app.emptyVehicles();
		app.buildUmlaeufe();
		app.removeUnusedNetworkParts();
		try {
			app.writeNetworkAndScheduleAndVehicles();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("done.");
	}

	private void prepareConfig() {
		this.inConfig.scenario().setUseTransit(true);
		this.inConfig.scenario().setUseVehicles(true);
		this.inConfig.network().setInputFile(InNetworkFile);
		this.outConfig.scenario().setUseTransit(true);
		this.outConfig.scenario().setUseVehicles(true);
		this.outConfig.network().setInputFile(InNetworkFile);
		this.outConfig.network().setOutputFile(OutNetworkFile);
	}

	private void loadData() {
		ScenarioLoaderImpl inLoader = new ScenarioLoaderImpl(inScenario);
		inLoader.loadScenario();
		try {
			new TransitScheduleReaderV1(inScenario.getTransitSchedule(), inScenario.getNetwork()).readFile(InTransitScheduleFile);
		} catch (SAXException e) {
			throw new RuntimeException("could not read transit schedule.", e);
		} catch (ParserConfigurationException e) {
			throw new RuntimeException("could not read transit schedule.", e);
		} catch (IOException e) {
			throw new RuntimeException("could not read transit schedule.", e);
		}
		ScenarioLoaderImpl outLoader = new ScenarioLoaderImpl(outScenario);
		outLoader.loadScenario();
	}

	private void copyRoutes() {
		TransitSchedule inTransitSchedule = this.inScenario.getTransitSchedule();
		TransitSchedule outTransitSchedule = this.outScenario.getTransitSchedule();
		for (TransitLine transitLine : inTransitSchedule.getTransitLines().values()) {
			outTransitSchedule.addTransitLine(transitLine);
		}
	}

	private void enterFacilities() {
		TransitSchedule inTransitSchedule = this.inScenario.getTransitSchedule();
		TransitSchedule outTransitSchedule = this.outScenario.getTransitSchedule();
		Map<Id, TransitStopFacility> facilityTemplates = new HashMap<Id, TransitStopFacility>(inTransitSchedule.getFacilities());
		Iterator<TransitLine> transitLineI = outTransitSchedule.getTransitLines().values().iterator();
		while (transitLineI.hasNext()) {
			TransitLine transitLine = transitLineI.next();
			try {
				enterFacilitiesForLine(facilityTemplates, transitLine);
			} catch (RuntimeException e) {
				e.printStackTrace();
				transitLineI.remove();
			}
		}
	}

	private void enterFacilitiesForLine(
			Map<Id, TransitStopFacility> facilityTemplates,
			TransitLine transitLine) {
		for (TransitRoute transitRouteI: transitLine.getRoutes().values()) {
			NetworkRouteWRefs linkNetworkRoute = transitRouteI.getRoute();
			Collection<Link> links = getAllLink(linkNetworkRoute);
			Iterator<Link> linkIterator = links.iterator();
			for (TransitRouteStop stop : transitRouteI.getStops()) {
				Link link = linkIterator.next();
				Id stopPointNo = stop.getStopFacility().getId();
				while (!stopPointNo.equals(link.getToNode().getId())) {
					link = linkIterator.next();
				}
				enterNewFacilityIfNecessary(transitRouteI, stopPointNo, link, facilityTemplates);
			}
		}
	}

	private List<Link> getAllLink(NetworkRouteWRefs linkNetworkRoute) {
		ArrayList<Link> links = new ArrayList<Link>();
		links.add(linkNetworkRoute.getStartLink());
		links.addAll(linkNetworkRoute.getLinks());
		links.add(linkNetworkRoute.getEndLink());
		return links;
	}

	private void enterNewFacilityIfNecessary(TransitRoute transitRoute,
			Id stopPointNo, Link link, Map<Id, TransitStopFacility> facilityTemplates) {
		if (stopPointNo != null) {
			TransitStopFacility facility = checkFacility(stopPointNo, link, facilityTemplates);
			enterFacility(stopPointNo, facility, transitRoute.getStops());
		}
	}

	private void enterFacility(Id stopPointNo, TransitStopFacility facility,
			List<TransitRouteStop> stops) {
		for (TransitRouteStop stop : stops) {
			if (stopPointNo.equals(stop.getStopFacility().getId())) {
				stop.setStopFacility(facility);
			}
		}
	}

	private TransitStopFacility checkFacility(Id stopPointNo, Link link, Map<Id, TransitStopFacility> facilityTemplates) {
		TransitSchedule outSchedule = this.outScenario.getTransitSchedule();
		TransitStopFacility toFacility = facilityTemplates.get(
				stopPointNo);
		Map<Link, TransitStopFacility> inLinks = getTransitStopInLinks(toFacility);
		TransitStopFacility facility = inLinks.get(link);
		if (facility == null) {
			IdImpl newId = new IdImpl(toFacility.getId().toString() + "."
					+ Integer.toString(inLinks.size() + 1));
			TransitStopFacility newFacility = outSchedule.getFactory()
					.createTransitStopFacility(newId, toFacility.getCoord(),
							toFacility.getIsBlockingLane());
			newFacility.setStopPostAreaId(toFacility.getId());
			newFacility.setLink(link);
			outSchedule.addStopFacility(newFacility);
			inLinks.put(link, newFacility);
			return newFacility;
		} else {
			return facility;
		}
	}

	private Map<Link, TransitStopFacility> getTransitStopInLinks(TransitStopFacility toFacility) {
		Map<Link, TransitStopFacility> inLinks = this.transitStopInLinks.get(toFacility);
		if (inLinks == null) {
			inLinks = new HashMap<Link, TransitStopFacility>();
			this.transitStopInLinks.put(toFacility, inLinks);
		}
		return inLinks;
	}

	private void writeNetworkAndScheduleAndVehicles() throws IOException,
			FileNotFoundException {
		NetworkLayer network = outScenario.getNetwork();
		log.info("writing network to file.");
		new NetworkWriter(network).writeFile(OutNetworkFile);
		log.info("writing TransitSchedule to file.");
		new TransitScheduleWriterV1(outScenario.getTransitSchedule()).write(OutTransitScheduleFile);
		log.info("writing vehicles to file.");
		new VehicleWriterV1(outScenario.getVehicles()).writeFile(OutVehicleFile);
		try {
			new TransitScheduleWriter(outScenario.getTransitSchedule()).writeFile(OutTransitScheduleFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void emptyVehicles() {
		outScenario.getVehicles().getVehicles().clear();
	}

	private void buildUmlaeufe() {
		Collection<TransitLine> transitLines = outScenario.getTransitSchedule().getTransitLines().values();
		GreedyUmlaufBuilderImpl greedyUmlaufBuilder = new GreedyUmlaufBuilderImpl(new UmlaufInterpolator(outScenario.getNetwork()), transitLines);
		umlaeufe = greedyUmlaufBuilder.build();
		
		VehiclesFactory vb = outScenario.getVehicles().getFactory();
		BasicVehicleType vehicleType = vb.createVehicleType(new IdImpl(
				"defaultTransitVehicleType"));
		BasicVehicleCapacity capacity = new BasicVehicleCapacityImpl();
		capacity.setSeats(Integer.valueOf(101));
		capacity.setStandingRoom(Integer.valueOf(0));
		vehicleType.setCapacity(capacity);
		outScenario.getVehicles().getVehicleTypes().put(vehicleType.getId(),
				vehicleType);

		long vehId = 0;
		for (Umlauf umlauf : umlaeufe) {
			BasicVehicle veh = vb.createVehicle(new IdImpl("veh_"+ Long.toString(vehId++)), vehicleType);
			outScenario.getVehicles().getVehicles().put(veh.getId(), veh);
			umlauf.setVehicleId(veh.getId());
		}
	}

	private void removeUnusedNetworkParts() {
		Collection<Node> usedNodes = new HashSet<Node>();
		for (Umlauf umlauf : umlaeufe) {
			for (UmlaufStueckI umlaufstueck : umlauf.getUmlaufStuecke()) {
				NetworkRouteWRefs route = umlaufstueck.getCarRoute();
				for (Link link : getAllLink(route)) {
					usedNodes.add(link.getFromNode());
					usedNodes.add(link.getToNode());
				}
			}
		}
		Collection<Node> allNodes = new ArrayList<Node>(outScenario.getNetwork().getNodes().values());
		for (Node node : allNodes) {
			if (!usedNodes.contains(node)) {
				outScenario.getNetwork().removeNode(node);
			}
		}
		Collection<TransitStopFacility> allFacilities = new ArrayList<TransitStopFacility>(outScenario.getTransitSchedule().getFacilities().values());
		for (TransitStopFacility transitStopFacility : allFacilities) {
			Link link = outScenario.getNetwork().getLinks().get(transitStopFacility.getLinkId());
			if (link == null) {
				outScenario.getTransitSchedule().getFacilities().remove(transitStopFacility.getId());
				log.warn("Removed facility "+transitStopFacility.getId());
			}
		}
	}

}
