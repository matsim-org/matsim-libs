/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.jbischoff.av.preparation;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import com.vividsolutions.jts.geom.Geometry;

/**
 * @author jbischoff
 *
 */
public class CarEventsToTaxiPlans {
	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario)
				.readFile("C:/Users/Joschka/Documents/shared-svn/projects/audi_av/scenario/networkc.xml.gz");
		Geometry geometry = ScenarioPreparator.readShapeFileAndExtractGeometry(
				"C:/Users/Joschka/Documents/shared-svn/projects/audi_av/shp/UntersuchungsraumAll.shp");

		Scenario scenario2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario2)
				.readFile("C:/Users/Joschka/Documents/runs-svn/bvg.run132.25pct/bvg.run132.25pct.output_network.xml.gz");

		
		ConverterEventHandler ch = new ConverterEventHandler(scenario, geometry,scenario2.getNetwork(), true);
//		ConverterEventHandler ch = new ConverterEventHandler(scenario, geometry,scenario2.getNetwork(), false);
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(ch);
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile("C:/Users/Joschka/Documents/runs-svn/bvg.run192.100pct/ITERS/it.100/bvg.run192.100pct.100.events.xml.gz");
//		reader.readFile("C:/Users/Joschka/Documents/runs-svn/bvg.run189.10pct/ITERS/it.100/bvg.run189.10pct.100.events.filtered.xml.gz");
		new PopulationWriter(ch.population).write("C:/Users/Joschka/Documents/shared-svn/projects/audi_av/scenario/eventBasedPlansWithCars.xml.gz");
	}
	
}

class ConverterEventHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler {
	
	Population population;
	NetworkImpl network;
	Network oldNetwork;
	CoordinateTransformation dest = TransformationFactory.getCoordinateTransformation(TransformationFactory.DHDN_GK4,"EPSG:25833");

	Map<Id<Person>, Tuple<Id<Link>, Double>> departures = new HashMap<>();
	int i = 0;
	private Geometry shape;
	private boolean leaveCarTrips;

	public ConverterEventHandler(Scenario scenario, Geometry shape, Network oldNetwork) {
		this(scenario, shape, oldNetwork, false);
	}

	public ConverterEventHandler(Scenario scenario, Geometry shape, Network oldNetwork, boolean leaveCarTrips) {
		this.population = scenario.getPopulation();
		this.network = (NetworkImpl) scenario.getNetwork();
		this.shape = shape;
		this.oldNetwork = oldNetwork;
		this.leaveCarTrips = leaveCarTrips;
	}

	@Override
	public void reset(int iteration) {

	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (event.getLegMode().equals("car")) {
			Tuple<Id<Link>, Double> t = departures.remove(event.getPersonId());
			if (t == null) {
				System.err.println("arrival without departure?");
				return;
			}
			createAndAddPerson(t.getFirst(), t.getSecond(), event.getLinkId(), event.getTime());
		}
	}

	private void createAndAddPerson(Id<Link> fromLinkId, double departureTime, Id<Link> toLinkId, double arrivalTime) {
		fromLinkId = convertLink(fromLinkId);
		toLinkId = convertLink(toLinkId);
		String mode = "taxi";
		if (fromLinkId == null) return;
		if (toLinkId == null) return;
		if (!checkIfLinksinShape(fromLinkId, toLinkId))
			{if (leaveCarTrips)
			mode = "car";
			else return;
			}
		if (toLinkId == fromLinkId) return;
		
		Person p = population.getFactory().createPerson(Id.createPersonId(i++));
		Plan plan = population.getFactory().createPlan();
		p.addPlan(plan);
		Activity home = population.getFactory().createActivityFromLinkId("home", fromLinkId);
		home.setEndTime(departureTime);
		plan.addActivity(home);
		Leg leg = population.getFactory().createLeg(mode);
		leg.setRoute(new GenericRouteImpl(fromLinkId, toLinkId));
		plan.addLeg(leg);
		Activity work = population.getFactory().createActivityFromLinkId("work", toLinkId);
		work.setStartTime(arrivalTime);
		plan.addActivity(work);
		population.addPerson(p);
	}

	private Id<Link> convertLink(Id<Link> fromLinkId) {
		try {Coord coord =dest.transform( this.oldNetwork.getLinks().get(fromLinkId).getCoord());
		
		return network.getNearestLinkExactly(coord).getId();}
		catch (NullPointerException e){
			System.err.println(fromLinkId.toString() + " doesnt exist in Network.");
			return null;
		}
	}

	private boolean checkIfLinksinShape(Id<Link> fromLinkId, Id<Link> toLinkId) {
	
		Coord startLinkCoord = this.network.getLinks().get(fromLinkId).getCoord();
		Coord endLinkCoord = this.network.getLinks().get(toLinkId).getCoord();
		return (shape.contains(MGC.coord2Point(startLinkCoord)) && shape.contains(MGC.coord2Point(endLinkCoord)));
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().equals("car")) {
			departures.put(event.getPersonId(), new Tuple<Id<Link>, Double>(event.getLinkId(), event.getTime()));

		}

	}

	public Population getPopulation() {
		return population;
	}
}
