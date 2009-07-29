/* *********************************************************************** *
 * project: org.matsim.*
 * VisualizeTransitPlans.java
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

package playground.marcel.pt.application;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.basic.v01.population.PlanElement;
import org.matsim.core.api.experimental.ScenarioImpl;
import org.matsim.core.api.experimental.network.Link;
import org.matsim.core.api.experimental.population.Activity;
import org.matsim.core.api.experimental.population.Leg;
import org.matsim.core.api.experimental.population.Person;
import org.matsim.core.api.experimental.population.Plan;
import org.matsim.core.api.experimental.population.Population;
import org.matsim.core.api.experimental.population.PopulationBuilder;
import org.matsim.core.api.experimental.population.PopulationWriter;
import org.matsim.core.api.experimental.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.events.Events;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.routes.LinkNetworkRoute;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitScheduleReader;
import org.matsim.transitSchedule.api.TransitStopFacility;
import org.matsim.vis.otfvis.opengl.OnTheFlyQueueSimQuad;
import org.xml.sax.SAXException;

import playground.marcel.pt.routes.ExperimentalTransitRoute;
import playground.marcel.pt.routes.ExperimentalTransitRouteFactory;
import playground.marcel.pt.utils.CreatePseudoNetwork;

public class VisualizeTransitPlans {

	private final static String NETWORK_FILE = "";
	private final static String TRANSIT_SCHEDULE_FILE = "../thesis-data/application/transitschedule.oevModell.xml";
	private final static String POPULATION_FILE = "/Volumes/Data/VSP/coding/eclipse35/thesis-data/application/plans.census2000ivtch1pct.dilZh30km.pt-routedOevModell.xml.gz";

	private final ScenarioImpl realScenario;
	private final ScenarioImpl visScenario;

	public VisualizeTransitPlans() {
		this.realScenario = new ScenarioImpl();
		this.realScenario.getConfig().scenario().setUseTransit(true);
		this.visScenario = new ScenarioImpl();
	}

	private void loadRealScenario() {
		if (NETWORK_FILE != "") {
			new MatsimNetworkReader(this.realScenario.getNetwork()).readFile(NETWORK_FILE);
		}
		try {
			new TransitScheduleReader(this.realScenario).readFile(TRANSIT_SCHEDULE_FILE);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		this.realScenario.getNetwork().getFactory().setRouteFactory(TransportMode.pt, new ExperimentalTransitRouteFactory());
		new MatsimPopulationReader(this.realScenario).readFile(POPULATION_FILE);
		this.realScenario.getPopulation().printPlansCount();
	}

	private void convertData() {
		CreatePseudoNetwork pseudoNetCreator = new CreatePseudoNetwork(this.realScenario.getTransitSchedule(), this.visScenario.getNetwork());
		pseudoNetCreator.run();

		Population visPop = this.visScenario.getPopulation();
		PopulationBuilder pb = visPop.getBuilder();
		for (Person person : this.realScenario.getPopulation().getPersons().values()) {
			Person visPerson = pb.createPerson(person.getId());
			for (Plan plan : person.getPlans()) {
				Plan visPlan = pb.createPlan();
				for (PlanElement pe : plan.getPlanElements()) {
					if (pe instanceof Activity) {
						Activity act = (Activity) pe;
						ActivityImpl visAct = (ActivityImpl) pb.createActivityFromCoord(act.getType(), act.getCoord());
						visAct.setStartTime(act.getStartTime());
						visAct.setEndTime(act.getEndTime());
						visAct.setLink(this.visScenario.getNetwork().getNearestLink(act.getCoord()));
						visPlan.addActivity(visAct);
					} else if (pe instanceof Leg) {
						Leg leg = (Leg) pe;

						Leg visLeg = pb.createLeg(leg.getMode() == TransportMode.pt ? TransportMode.car : leg.getMode());
						if (leg.getMode() == TransportMode.pt) {
							visLeg.setRoute(convertRoute((ExperimentalTransitRoute) leg.getRoute(), pseudoNetCreator));
						} else {
							visLeg.setRoute(leg.getRoute()); // reuse route
						}
						visPlan.addLeg(visLeg);
					}
				}
				visPerson.addPlan(visPlan);
			}
			visPop.addPerson(visPerson);
		}

		new PopulationWriter(visPop).write("vis.plans.xml");
		new NetworkWriter(this.visScenario.getNetwork(), "vis.network.xml").write();
		Config visConfig = new Config();
		visConfig.addCoreModules();
		visConfig.network().setInputFile("vis.network.xml");
		visConfig.plans().setInputFile("vis.plans.xml");
		new ConfigWriter(visConfig, "vis.config.xml").write();

	}

	private Route convertRoute(final ExperimentalTransitRoute route, final CreatePseudoNetwork pseudoNetCreator) {
		TransitLine tLine = null;
		try {
			tLine = this.realScenario.getTransitSchedule().getTransitLines().get(route.getLineId());
		} catch (NullPointerException e) {
//			e.printStackTrace();
		}
		if (tLine == null) {
			System.err.println("could not find transit line '" + route.getLineId() + "' from route " + route.getRouteDescription());
			return null;
		}

		TransitRoute tRoute = tLine.getRoutes().get(route.getRouteId());
		TransitStopFacility accessStop = this.realScenario.getTransitSchedule().getFacilities().get(route.getAccessStopId());
		TransitStopFacility egressStop = this.realScenario.getTransitSchedule().getFacilities().get(route.getEgressStopId());

		NetworkRoute netRoute = new LinkNetworkRoute(accessStop.getLink(), egressStop.getLink());
		List<Link> links = new ArrayList<Link>();
		boolean include = false;
		TransitStopFacility prevStop = null;
		for (TransitRouteStop stop : tRoute.getStops()) {
			if (egressStop == stop.getStopFacility()) {
				include = false;
			}
			if (include) {
				links.add(pseudoNetCreator.getLinkBetweenStops(prevStop, stop.getStopFacility()));
			}
			if (accessStop == stop.getStopFacility()) {
				include = true;
			}
			prevStop = stop.getStopFacility();
		}
		netRoute.setLinks(accessStop.getLink(), links, egressStop.getLink());
		return netRoute;
	}

	private void visualize() {
		Events events = new Events();
		OnTheFlyQueueSimQuad client = new OnTheFlyQueueSimQuad(this.visScenario, events);
		client.run();
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		VisualizeTransitPlans app = new VisualizeTransitPlans();
		app.loadRealScenario();
		app.convertData();
		app.visualize();
	}

}
