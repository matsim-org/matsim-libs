/* *********************************************************************** *
 * project: org.matsim.*
 * BavariaDemandCreator
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
package playground.dgrether.prognose2025;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;


/**
 * Abstract class to reduce a population created from prognose 2025 data 
 * to a population in a certain area given as shape file. 
 * @author dgrether
 *
 */
public abstract class DgPrognose2025DemandFilter {

	private static final Logger log = Logger.getLogger(DgPrognose2025GvDemandFilter.class);

	protected CoordinateTransformation wgs84ToDhdnGk4 = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.DHDN_GK4); 
	
	protected Scenario scenario;

	protected Network net;

	protected Population pop;

	private GeometryFactory factory;

	private Set<Id> linkIdsInShapefile;

	private Set<Feature> featuesInShape;
	
		
	private void readData(String networkFilename, String populationFilename, String filterShapeFileName) throws IOException{
		this.factory = new GeometryFactory();
		//read shape file
		this.featuesInShape = new ShapeFileReader().readFileAndInitialize(filterShapeFileName);
		//read scenario
		this.scenario = new ScenarioImpl();
		MatsimNetworkReader netReader	= new MatsimNetworkReader(scenario);
		netReader.readFile(networkFilename);
		this.net = scenario.getNetwork();
		this.linkIdsInShapefile = this.detectLinkIdsInShape(this.net);
		MatsimPopulationReader popReader = new MatsimPopulationReader(scenario); 
		popReader.readFile(populationFilename);
		this.pop = scenario.getPopulation();
	}
	
	
	private Set<Id> detectLinkIdsInShape(Network network){
		Set<Id> linkIds = new HashSet<Id>();
		for (Link link : network.getLinks().values()){
			if (this.isLinkInShape(link)){
				linkIds.add(link.getId());
			}
		}
		return linkIds;
	}
	
	
	private boolean isLinkInShape(Link link) {
		boolean found = false;
		Coord linkCoord = link.getCoord();
		Coord dhdnCoord = this.wgs84ToDhdnGk4.transform(linkCoord);
		Geometry geo = factory.createPoint(new Coordinate(dhdnCoord.getX(), dhdnCoord.getY()));
		for (Feature ft : featuesInShape){
			if (ft.getDefaultGeometry().contains(geo)){
				found = true;
				break;
			}
		}
		return found;
	}
	

	protected abstract void addNewPerson(Link startLink, Person person, Population newPop, double legStartTimeSec, Link endLink); 
	
	public void filterAndWriteDemand(final String networkFilename, final String populationFilename, final String filterShapeFileName, final String populationOutputFilename) throws IOException{
		log.info("start to create blauweiss demand...");
		this.readData(networkFilename, populationFilename, filterShapeFileName);
		log.info("data loaded...");
		Scenario newScenario = new ScenarioImpl();
		Population newPop = newScenario.getPopulation();
		
		for (Person person : this.pop.getPersons().values()){
			List<PlanElement> planElements = person.getPlans().get(0).getPlanElements();
			Activity homeAct = (Activity)planElements.get(0);
			PlanElement pe = planElements.get(1);
			Route route = ((Leg)pe).getRoute();
			NetworkRoute netRoute = (NetworkRoute) route;
			Link startLink = this.net.getLinks().get(route.getStartLinkId());
			Link endLink = this.net.getLinks().get(route.getEndLinkId());
			if (startLink.getId().equals(netRoute.getEndLinkId())){
				continue;
			}
			if (this.linkIdsInShapefile.contains(startLink.getId())){
//				log.info("Person: " + person.getId() + " starts route in area of interest...");
				this.addNewPerson(startLink, person, newPop, homeAct.getEndTime(), endLink);
			}
			else {
				for (Id linkId : netRoute.getLinkIds()){
					if (this.linkIdsInShapefile.contains(linkId)){
						Link newStartLink = this.net.getLinks().get(linkId);
						double actEndTime = homeAct.getEndTime() +  this.calculateFreespeedTravelTimeToLink(this.net, netRoute, newStartLink);
//						log.info("Person: " + person.getId() + " drives through/into area of interest...");
						//this route goes through the area of interest
						this.addNewPerson(newStartLink, person, newPop, actEndTime, endLink);
						break;
					}
				}
			}
		}
		log.info("writing population...");
		PopulationWriter popWriter = new PopulationWriter(newPop, this.net);
		popWriter.write(populationOutputFilename);
		log.info("demand filtered and written. done.");
	}


	private double calculateFreespeedTravelTimeToLink(Network network, NetworkRoute netRoute, Link endLink) {
		double travelTime = 0.0;
		for (Id linkId : netRoute.getLinkIds()){
			Link l = network.getLinks().get(linkId);
			travelTime += l.getLength() / l.getFreespeed();
			if (linkId.equals(endLink.getId())){
				return travelTime;
			}
		}
		return travelTime;
	}
}