/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.andreas.fcd;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkTransform;
import org.matsim.core.network.io.NetworkReaderMatsimV1;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.population.*;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.utils.gis.matsim2esri.network.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Fcd {
	
	private static final Logger log = Logger.getLogger(Fcd.class);
	
	private TreeMap<Id,FcdNetworkPoint> networkMap;
	private LinkedList<FcdEvent> fcdEventsList;
	private CoordinateTransformation coordTransform = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.DHDN_GK4);
	private Set<String> linksUsed = new TreeSet<String>();
	private double minDistanceBetweenTwoActs;
	
	public Fcd(String netInFile, String fcdEventsInFile, double minDistanceBetweenTwoActs) {
		this.minDistanceBetweenTwoActs = minDistanceBetweenTwoActs;
		try {
			log.info("Reading fcd network file...");
			this.networkMap = ReadFcdNetwork.readFcdNetwork(netInFile);
			log.info("...done. Network map contains " + this.networkMap.size() + " entries");
			
			log.info("Reading fcd events file...");
			this.fcdEventsList = ReadFcdEvents.readFcdEvents(fcdEventsInFile);
			log.info("...done. Events list contains " + this.fcdEventsList.size() + " entries");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static Set<String> readFcdReturningLinkIdsUsed(String fcdNetInFile, String fcdEventsInFile, String outDir, String matsimNetwork, double minDistanceBetweenTwoActs){
		MutableScenario sc = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		NetworkReaderMatsimV1 reader = new NetworkReaderMatsimV1(sc.getNetwork());
		reader.readFile(matsimNetwork);
		
		Fcd fcd = new Fcd(fcdNetInFile, fcdEventsInFile, minDistanceBetweenTwoActs);
		fcd.writeNetworkFromEvents(outDir + "fcd_netFromEvents.xml.gz");
		fcd.writeSimplePlansFromEvents(outDir + "fcd_simplePlans.xml.gz");
		fcd.writeComplexPlansFromEvents(outDir + "fcd_complexPlans.xml.gz", (Network) sc.getNetwork());
		fcd.writeLinksUsed(outDir + "fcd_linksInUseByFcd.txt");
		
		return fcd.linksUsed;
	}

	public static void main(String[] args) {
		String netInFile = "D:\\Berlin\\DLR_FCD\\20110207_Analysis\\berlin_2010_anonymized.ext";
//		String fcdEventsInFile = "D:\\Berlin\\DLR_FCD\\20110207_Analysis\\fcd-20101028_10min.ano";
//		String fcdEventsInFile = "D:\\Berlin\\DLR_FCD\\20110207_Analysis\\11438971.txt";
		String fcdEventsInFile = "D:\\Berlin\\DLR_FCD\\20110207_Analysis\\11446526.txt";
		String netOutFile = "D:\\Berlin\\DLR_FCD\\20110207_Analysis\\netFromEvents.xml";
		String plansOutFile = "D:\\Berlin\\DLR_FCD\\20110207_Analysis\\plansFromEvents.xml";
		String matsimNetwork = "D:\\Berlin\\DLR_FCD\\20110207_Analysis\\counts_network.xml";
		String linksUsed = "D:\\Berlin\\DLR_FCD\\20110207_Analysis\\linksUsed.txt";
		
		MutableScenario sc = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		NetworkReaderMatsimV1 reader = new NetworkReaderMatsimV1(sc.getNetwork());
		reader.readFile(matsimNetwork);
		
		Fcd fcd = new Fcd(netInFile, fcdEventsInFile, 0.0);
		fcd.writeNetworkFromEvents(netOutFile);
		fcd.writeSimplePlansFromEvents(plansOutFile);
		fcd.writeComplexPlansFromEvents(plansOutFile + ".complex.xml", (Network) sc.getNetwork());
		fcd.writeLinksUsed(linksUsed);
	}

	private void writeLinksUsed(String linksUsedOutFile) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(linksUsedOutFile)));
			for (String idString : this.linksUsed) {
				writer.write(idString);
				writer.newLine();
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	private void writeSimplePlansFromEvents(String plansOutFile) {
		log.info("Creating plans from fcd events...");
		int numberOfPlans = 1;
        MutableScenario sc = ((MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig()));
        Population pop = PopulationUtils.createPopulation(sc.getConfig(), sc.getNetwork());
		
		FcdEvent lastEvent = null;
		Person currentPerson = null;
		
		for (Iterator<FcdEvent> iterator = this.fcdEventsList.iterator(); iterator.hasNext();) {
			FcdEvent currentEvent = iterator.next();
			
			if(lastEvent == null){
				lastEvent = currentEvent;
				currentPerson = PopulationUtils.getFactory().createPerson(Id.create(numberOfPlans + "-" + currentEvent.getVehId().toString(), Person.class));
				pop.addPerson(currentPerson);
				numberOfPlans++;
				currentPerson.addPlan(PopulationUtils.createPlan());
				currentPerson.getSelectedPlan().addActivity(createActivityFromFcdEvent(currentEvent));
				continue;
			}
			
			if(lastEvent.getVehId().toString().equalsIgnoreCase(currentEvent.getVehId().toString())){
				// same track, add activities
				currentPerson.getSelectedPlan().addLeg(PopulationUtils.createLeg(TransportMode.car));
				currentPerson.getSelectedPlan().addActivity(createActivityFromFcdEvent(currentEvent));
				
			} else {
				// different one, new person
				currentPerson = PopulationUtils.getFactory().createPerson(Id.create(numberOfPlans + "-" + currentEvent.getVehId().toString(), Person.class));
				pop.addPerson(currentPerson);
				numberOfPlans++;
				currentPerson.addPlan(PopulationUtils.createPlan());
				currentPerson.getSelectedPlan().addActivity(createActivityFromFcdEvent(currentEvent));
			}
		
			lastEvent = currentEvent;
		}
		
		log.info("...done");
		
		PopulationWriter writer = new PopulationWriter(pop, ((MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig())).getNetwork());
		writer.write(plansOutFile);
		log.info(pop.getPersons().size() + " plans written to " + plansOutFile);
	}
	
	private void writeComplexPlansFromEvents(String plansOutFile, Network net) {
		log.info("Creating plans from fcd events...");
		int numberOfPlans = 1;
        MutableScenario sc = ((MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig()));
        Population pop = PopulationUtils.createPopulation(sc.getConfig(), sc.getNetwork());
		
		FcdEvent lastEvent = null;
		Person currentPerson = null;
		Link lastLink = null;
		
		for (Iterator<FcdEvent> iterator = this.fcdEventsList.iterator(); iterator.hasNext();) {
			FcdEvent currentEvent = iterator.next();
			
			Link link = getLinkWithRightDirection(currentEvent, net);
			
			if(link == null){
				lastEvent = currentEvent;
				continue;
			}
			
			if(link == lastLink){
				lastEvent = currentEvent;
				continue;
			}
			
			if(lastLink != null){
				if(CoordUtils.calcEuclideanDistance(link.getCoord(), lastLink.getCoord()) < this.minDistanceBetweenTwoActs){
					lastEvent = currentEvent;
					continue;
				}
			}
			
			lastLink = link;
			
			if(lastEvent == null){
				lastEvent = currentEvent;
				currentPerson = PopulationUtils.getFactory().createPerson(Id.create(numberOfPlans + "-" + currentEvent.getVehId().toString(), Person.class));
				pop.addPerson(currentPerson);
				numberOfPlans++;
				currentPerson.addPlan(PopulationUtils.createPlan());
				currentPerson.getSelectedPlan().addActivity(createActivityWithLinkFromFcdEvent(currentEvent, link));
				this.linksUsed.add(link.getId().toString());
				continue;
			}
			
			if(lastEvent.getVehId().toString().equalsIgnoreCase(currentEvent.getVehId().toString())){
				// same track, add activities
				currentPerson.getSelectedPlan().addLeg(PopulationUtils.createLeg(TransportMode.car));
				currentPerson.getSelectedPlan().addActivity(createActivityWithLinkFromFcdEvent(currentEvent, link));
				this.linksUsed.add(link.getId().toString());
				
			} else {
				// different one, new person
				currentPerson = PopulationUtils.getFactory().createPerson(Id.create(numberOfPlans + "-" + currentEvent.getVehId().toString(), Person.class));
				pop.addPerson(currentPerson);
				numberOfPlans++;
				currentPerson.addPlan(PopulationUtils.createPlan());
				currentPerson.getSelectedPlan().addActivity(createActivityWithLinkFromFcdEvent(currentEvent, link));
				this.linksUsed.add(link.getId().toString());
			}
		
			lastEvent = currentEvent;
		}
		
		log.info("...done");
		
		PopulationWriter writer = new PopulationWriter(pop, ((MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig())).getNetwork());
		writer.write(plansOutFile);
		log.info(pop.getPersons().size() + " plans written to " + plansOutFile);
	}
	
	private void writeNetworkFromEvents(String netOutFile) {
		log.info("Creating network from fcd events...");
		Network net = NetworkUtils.createNetwork();
		
		FcdEvent lastEvent = null;
		for (Iterator<FcdEvent> iterator = this.fcdEventsList.iterator(); iterator.hasNext();) {
			FcdEvent currentEvent = iterator.next();
			
			if(lastEvent == null){
				lastEvent = currentEvent;
				continue;
			}
			
			if(lastEvent.getVehId().toString().equalsIgnoreCase(currentEvent.getVehId().toString())){
				// same track, create link
				if(net.getNodes().get(currentEvent.getLinkId()) == null){
					NetworkUtils.createAndAddNode(net, (Id<Node>) currentEvent.getLinkId(), this.coordTransform.transform(this.networkMap.get(currentEvent.getLinkId()).getCoord()));
				}
				if(net.getNodes().get(lastEvent.getLinkId()) == null){
					NetworkUtils.createAndAddNode(net, (Id<Node>) lastEvent.getLinkId(), this.coordTransform.transform(this.networkMap.get(lastEvent.getLinkId()).getCoord()));
				}
				
				Id<Link> newLinkId = Id.create(lastEvent.getLinkId().toString() + "-" + currentEvent.getLinkId().toString(), Link.class);
				if(net.getLinks().get(newLinkId) == null){
					final Id<Link> id = newLinkId;
					NetworkUtils.createAndAddLink(net,id, net.getNodes().get(lastEvent.getLinkId()), net.getNodes().get(currentEvent.getLinkId()), 999.9, 9.9, 9999.9, 9.9 );
				}
			}			

			lastEvent = currentEvent;
		}
		log.info("...done.");
		
		log.info("Dumping matsim network to " + netOutFile + "...");
		NetworkWriter writer = new NetworkWriter(net);
		writer.write(netOutFile);
		log.info("...done");
		
		log.info("Writing network shape file...");
		NetworkTransform nT = new NetworkTransform(TransformationFactory.getCoordinateTransformation(TransformationFactory.DHDN_GK4, TransformationFactory.WGS84));
		nT.run(net);	
		
		//write shape file
		final WidthCalculator wc = new WidthCalculator() {
			@Override
			public double getWidth(Link link) {
				return 1.0;
			}
		};
		
		FeatureGeneratorBuilder builder = new FeatureGeneratorBuilder() {
			@Override
			public FeatureGenerator createFeatureGenerator() {
				FeatureGenerator fg = new LineStringBasedFeatureGenerator(wc, MGC.getCRS(TransformationFactory.WGS84));
				return fg;
			}
		};
		
		Links2ESRIShape l2ES = new Links2ESRIShape(net, netOutFile + ".shp", builder);
		l2ES.write();
		log.info("...done");
	}
	
	
	
	// HELPER
	
	private Activity createActivityFromFcdEvent(FcdEvent fcdEvent){
		Activity act = PopulationUtils.createActivityFromCoord("fcd", this.coordTransform.transform(this.networkMap.get(fcdEvent.getLinkId()).getCoord()));
		act.setEndTime(fcdEvent.getTime());
		return act;
	}

	private Activity createActivityWithLinkFromFcdEvent(FcdEvent fcdEvent, Link link) {
		Activity act = PopulationUtils.createActivityFromCoordAndLinkId("fcd", this.coordTransform.transform(this.networkMap.get(fcdEvent.getLinkId()).getCoord()), link.getId());
		act.setEndTime(fcdEvent.getTime());
		return act;
	}

	private Link getLinkWithRightDirection(FcdEvent fcdEvent, Network net) {
		Id linkId = fcdEvent.getLinkId();
		Coord coord = this.networkMap.get(linkId).getCoord();
		double direction = this.networkMap.get(linkId).getDirection();
		return getNearestLinkWithRightDirection(net, this.coordTransform.transform(coord), direction);
	}

	public static Link getNearestLinkWithRightDirection(Network net, Coord coord, double direction) {
		Link link = NetworkUtils.getNearestLink(net, coord);
		double directionGiven_rad = direction * 2 * Math.PI / 360.0;
		Coord vectorGiven = new Coord(Math.cos(directionGiven_rad), Math.sin(directionGiven_rad));
		// could be done without converting to a vector
		
		if(angleIsWithinEpsilon(getVector(link), vectorGiven)){
			return link;
		}
	
		// try the opposite link, if possible
		for (Link tempLink : link.getToNode().getOutLinks().values()) {
			if(tempLink.getToNode().getId().toString().equalsIgnoreCase(link.getFromNode().getId().toString())){
				if(angleIsWithinEpsilon(getVector(tempLink), vectorGiven)){
					return tempLink;
				}
			}
		}		
		return null;
	}

	private static boolean angleIsWithinEpsilon(Coord coordOne, Coord coordTwo){
		if(Math.abs(getAngleBetweenVectors(coordOne, coordTwo)) < 0.52){ // 30 Grad
			return true;
		}		
		return false;
	}

	private static double getAngleBetweenVectors(Coord coordInLink, Coord coordOutLink){
		double thetaInLink = Math.atan2(coordInLink.getY(), coordInLink.getX());
		double thetaOutLink = Math.atan2(coordOutLink.getY(), coordOutLink.getX());
		double thetaDiff = thetaOutLink - thetaInLink;
				
		if (thetaDiff < -Math.PI){
			thetaDiff += 2 * Math.PI;
		} else if (thetaDiff > Math.PI){
			thetaDiff -= 2 * Math.PI;
		}
	
		return thetaDiff;
	}	
	
	private static Coord getVector(Link link){
		double x = link.getToNode().getCoord().getX() - link.getFromNode().getCoord().getX();
		double y = link.getToNode().getCoord().getY() - link.getFromNode().getCoord().getY();
		return new Coord(x, y);
	}

}
