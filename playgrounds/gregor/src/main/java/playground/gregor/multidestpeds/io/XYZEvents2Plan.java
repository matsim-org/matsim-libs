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
package playground.gregor.multidestpeds.io;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.xml.sax.SAXException;

import playground.gregor.multidestpeds.helper.QueueSimDepartCalculator;
import playground.gregor.sim2d_v2.events.XYVxVyEvent;
import playground.gregor.sim2d_v2.events.XYVxVyEventsFileReader;
import playground.gregor.sim2d_v2.events.XYVxVyEventsHandler;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * @author laemmel
 * 
 */
public class XYZEvents2Plan implements XYVxVyEventsHandler, AgentArrivalEventHandler {

	private final Scenario sc;

	Map<Id, P> lastPosition = new HashMap<Id, P>();

	private final PopulationFactory fac;

	private final String mode;

	private QueueSimDepartCalculator qSimDepart = null;

	protected XYZEvents2Plan() {
		this.sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		this.fac = this.sc.getPopulation().getFactory();
		this.mode = "walk2d";
	}
	
	public XYZEvents2Plan(Scenario sc,String mode) {
		this.sc = sc;
		this.fac = sc.getPopulation().getFactory();
		this.mode = mode;
	}
	
	public XYZEvents2Plan(Scenario sc,String mode, String eventsFile) {
		this.sc = sc;
		this.fac = sc.getPopulation().getFactory();
		this.mode = mode;
		this.qSimDepart = new QueueSimDepartCalculator(sc, eventsFile);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * playground.gregor.sim2d.events.XYZEventsHandler#handleEvent(playground
	 * .gregor.sim2d.events.XYZAzimuthEvent)
	 */
	@Override
	public void handleEvent(XYVxVyEvent event) {
		Id id = event.getPersonId();
		if (!this.lastPosition.containsKey(id)) {
			P p = new P();
			p.dep = event.getTime();
			p.orig = event.getCoordinate();
			p.id = id;
			p.vx = event.getVX();
			p.vy = event.getVY();
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
		ActivityImpl actS =  (ActivityImpl) this.fac.createActivityFromCoord("h", MGC.coordinate2Coord(p.orig));
		Id linkId = ((NetworkImpl)this.sc.getNetwork()).getNearestLink(MGC.coordinate2Coord(p.orig)).getId();
		if (linkId.toString().equals("16")) {
			linkId = new IdImpl("3");
		}
		actS.setLinkId(linkId);
		
		double time = getEndTime(p);
		actS.setEndTime(time);
		Leg leg = this.fac.createLeg(this.mode);
		ActivityImpl actE = (ActivityImpl) this.fac.createActivityFromCoord("h", MGC.coordinate2Coord(p.dest));
		actE.setLinkId(event.getLinkId());
		plan.addActivity(actS);
		plan.addLeg(leg);
		plan.addActivity(actE);
		pers.addPlan(plan);
//		((PersonImpl)pers).setLicence(Double.toString(p.vx));
//		((PersonImpl)pers).setSex(Double.toString(p.vy));
		this.sc.getPopulation().addPerson(pers);
	}

	private double getEndTime(P p) {
		if (this.qSimDepart == null) {
			return p.dep;
		} else {
			return this.qSimDepart.getQSimDepartures().get(p.id);
		}
	}

	private static class P {
		public double vx;
		public double vy;
		Coordinate orig;
		double dep;
		Coordinate dest;
		Id id;
	}

	public static void main(String[] args) throws SAXException, ParserConfigurationException, IOException {
		String eventsFile = "/Users/laemmel/devel/dfg/input/events.xml";
		String configFile = "/Users/laemmel/devel/dfg/config2d.xml";
		Config c = ConfigUtils.loadConfig(configFile);
		Scenario sc = ScenarioUtils.createScenario(c);
		new MatsimNetworkReader(sc).readFile(c.network().getInputFile());
		EventsManager mgr = EventsUtils.createEventsManager();
		XYZEvents2Plan planGen = new XYZEvents2Plan();
		mgr.addHandler(planGen);
		XYVxVyEventsFileReader reader = new XYVxVyEventsFileReader(mgr);
		reader.parse(eventsFile);
		planGen.write("/Users/laemmel/devel/dfg/input/plans.xml");
	}

	/**
	 * 
	 */
	private void write(String file) {
		new PopulationWriter(this.sc.getPopulation(), this.sc.getNetwork()).write(file);

	}
}
