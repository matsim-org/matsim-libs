/* *********************************************************************** *
 * project: org.matsim.*
 * XYZEvents2Plan.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.gregor.pedvis;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.misc.ConfigUtils;
import org.xml.sax.SAXException;

import com.vividsolutions.jts.geom.Coordinate;

import playground.gregor.sim2d_v2.events.XYZAzimuthEvent;
import playground.gregor.sim2d_v2.events.XYZEventsFileReader;
import playground.gregor.sim2d_v2.events.XYZEventsHandler;

/**
 * @author laemmel
 * 
 */
public class XYZEvents2Plan implements XYZEventsHandler, AgentArrivalEventHandler {

	private final ScenarioImpl sc;

	Map<Id, P> lastPosition = new HashMap<Id, P>();

	private final PopulationFactory fac;

	public XYZEvents2Plan() {
		this.sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		this.fac = this.sc.getPopulation().getFactory();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * playground.gregor.sim2d.events.XYZEventsHandler#handleEvent(playground
	 * .gregor.sim2d.events.XYZAzimuthEvent)
	 */
	@Override
	public void handleEvent(XYZAzimuthEvent event) {
		Id id = event.getPersonId();
		if (!this.lastPosition.containsKey(id)) {
			P p = new P();
			p.dep = event.getTime();
			p.orig = event.getCoordinate();
			p.id = id;
			this.lastPosition.put(id, p);
		} else {
			this.lastPosition.get(id).dest = event.getCoordinate();
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.core.events.handler.EventHandler#reset(int)
	 */
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler
	 * #handleEvent(org.matsim.core.api.experimental.events.AgentArrivalEvent)
	 */
	@Override
	public void handleEvent(AgentArrivalEvent event) {
		P p = this.lastPosition.get(event.getPersonId());
		Person pers = this.fac.createPerson(p.id);
		Plan plan = this.fac.createPlan();
		Activity actS = this.fac.createActivityFromCoord("h", MGC.coordinate2Coord(p.orig));
		actS.setEndTime(p.dep);
		Leg leg = this.fac.createLeg("car");
		Activity actE = this.fac.createActivityFromCoord("h", MGC.coordinate2Coord(p.dest));
		plan.addActivity(actS);
		plan.addLeg(leg);
		plan.addActivity(actE);
		pers.addPlan(plan);
		this.sc.getPopulation().addPerson(pers);
	}

	private static class P {
		Coordinate orig;
		double dep;
		Coordinate dest;
		Id id;
	}

	public static void main(String[] args) throws SAXException, ParserConfigurationException, IOException {
		String eventsFile = "/Users/laemmel/devel/dfg/events.xml";
		String configFile = "/Users/laemmel/devel/dfg/config2d.xml";
		Config c = ConfigUtils.loadConfig(configFile);
		Scenario sc = ScenarioUtils.createScenario(c);
		new MatsimNetworkReader(sc).readFile(c.network().getInputFile());
		EventsManager mgr = EventsUtils.createEventsManager();
		XYZEvents2Plan planGen = new XYZEvents2Plan();
		mgr.addHandler(planGen);
		XYZEventsFileReader reader = new XYZEventsFileReader(mgr);
		reader.parse(eventsFile);
		planGen.write("/Users/laemmel/devel/dfg/data/90gradPlans.xml");
	}

	/**
	 * 
	 */
	private void write(String file) {
		new PopulationWriter(this.sc.getPopulation(), this.sc.getNetwork()).write(file);

	}
}
