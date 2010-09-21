/* *********************************************************************** *
 * project: org.matsim.*
 * BavariaGvCreator
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
package playground.dgrether.detailedEval;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.geotools.data.FeatureSource;
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
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.events.EventsManagerFactoryImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.gis.ShapeFileReader;

import playground.dgrether.DgPaths;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;


/**
 * @author dgrether
 *
 */
public class BavariaGvCreator {

	public static final String shapeFile = DgPaths.REPOS + "shared-svn/projects/detailedEval/Net/shapeFromVISUM/Verkehrszellen_Umrisse_area.SHP";
	
	private static final String popPrognose2025_2004 = "";
	
	private static final String netPrognose2025_2004 = DgPaths.REPOS + "shared-svn/studies/countries/de/prognose_2025/demand/network_cleaned_wgs84.xml.gz";
	
	private static final String events2004 = "";

	private Scenario scenario;

	private Network net;

	private Population pop;

	private FeatureSource bayernFeatures;

	private GeometryFactory factory;

	private EventsByLinkIdCollector collector;
	
	
	public BavariaGvCreator(){
	}
		
	private void readData() throws IOException{
		this.factory = new GeometryFactory();
		//read shape file
		this.bayernFeatures = ShapeFileReader.readDataFile(shapeFile);

		this.scenario = new ScenarioImpl();
		MatsimNetworkReader netReader	= new MatsimNetworkReader(scenario);
		netReader.readFile(netPrognose2025_2004);
		this.net = scenario.getNetwork();
		MatsimPopulationReader popReader = new MatsimPopulationReader(scenario); 
		this.pop = scenario.getPopulation();
		
		EventsManager manager = new EventsManagerFactoryImpl().createEventsManager();
		this.collector = new EventsByLinkIdCollector();
		manager.addHandler(this.collector);
		MatsimEventsReader eventsReader = new MatsimEventsReader(manager);
		eventsReader.readFile(events2004);
	}
	
	
	private boolean isLinkBlauWeiss(Link link) throws IOException{
		boolean found = false;
		Coord linkCoord = link.getCoord();
		Geometry geo = factory.createPoint(new Coordinate(linkCoord.getX(), linkCoord.getY()));
		for (Feature ft : (Collection<Feature>) bayernFeatures.getFeatures()){
			if (ft.getDefaultGeometry().contains(geo)){
				found = true;
				break;
			}
		}
		return found;
	}
	

	private void addNewPerson(Link startLink, Person person, Population newPop, Route route){
		PopulationFactory popFactory = newPop.getFactory();
		Person newPerson = popFactory.createPerson(person.getId());
		newPop.addPerson(newPerson);
		Plan newPlan = popFactory.createPlan();
		newPerson.addPlan(newPlan);
		//start activity
		Activity newAct = popFactory.createActivityFromCoord("gvHome", startLink.getCoord());
		LinkLeaveEvent leaveEvent = this.collector.getLinkLeaveEvent(person.getId(), startLink.getId());
		newAct.setEndTime(leaveEvent.getTime());
		newPlan.addActivity(newAct);
		//end activity
		Link endLink = net.getLinks().get(route.getEndLinkId());
		newAct = popFactory.createActivityFromCoord("gvHome", endLink.getCoord());
		newPlan.addActivity(newAct);
	}
	
	public void createBavariaGvPop() throws IOException{
		this.readData();
		Scenario newScenario = new ScenarioImpl();
		Population newPop = newScenario.getPopulation();
		
		for (Person person : pop.getPersons().values()){
			for (PlanElement pe : person.getPlans().get(0).getPlanElements()){
				if (pe instanceof Leg){
					Route route = ((Leg)pe).getRoute();
					NetworkRoute netRoute = (NetworkRoute) route;
					Link startLink = net.getLinks().get(route.getStartLinkId());
					if (isLinkBlauWeiss(startLink)){
						this.addNewPerson(startLink, person, newPop, route);
					}
					else {
						for (Id linkId : netRoute.getLinkIds()){
							Link link = net.getLinks().get(linkId);
							if (this.isLinkBlauWeiss(link)){
								//this route goes through bavaria, god bless us
								this.addNewPerson(link, person, newPop, netRoute);
								break;
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		new BavariaGvCreator().createBavariaGvPop();
	} 

}

class EventsByLinkIdCollector implements LinkLeaveEventHandler {
	// person -> linkId -> LinkLeaveEvent
	private Map<Id, Map<Id, LinkLeaveEvent>> events = new HashMap<Id, Map<Id, LinkLeaveEvent>>();
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Map<Id, LinkLeaveEvent> map = this.events.get(event.getPersonId());
		if (map == null){
			events.put(event.getPersonId(), new HashMap<Id, LinkLeaveEvent>());
		}
		map.put(event.getLinkId(), event);
	}

	public LinkLeaveEvent getLinkLeaveEvent(Id personId, Id linkId){
		return this.events.get(personId).get(linkId);
	}
	
	@Override
	public void reset(int iteration) {
	}
	
}

