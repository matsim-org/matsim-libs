/* *********************************************************************** *
 * project: org.matsim.*
 * PlansReaderMatsimV0.java
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

package org.matsim.core.population;

import java.io.IOException;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.core.utils.misc.Time;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * A reader for plans files of MATSim according to <code>plans_v0.dtd</code>.
 *
 * @author mrieser
 * @author balmermi
 */
public class PopulationReaderMatsimV0 extends MatsimXmlParser implements PopulationReader {

	private final static String PLANS = "plans";
	private final static String DEMAND = "demand";
	private final static String SEGMENT = "segment";
	private final static String MODEL = "model";
	private final static String PARAM = "param";
	private final static String PERSON = "person";
	private final static String PLAN = "plan";
	private final static String ACT = "act";
	private final static String LEG = "leg";
	private final static String ROUTE = "route";

	private final Population plans;
	private final Network network;
	private PersonImpl currperson = null;
	private PlanImpl currplan = null;
	private LegImpl currleg = null;
	private NetworkRouteWRefs currroute = null;

	private ActivityImpl prevAct = null;
	private String routeNodes = null;

	private static final Logger log = Logger.getLogger(PopulationReaderMatsimV0.class);

	protected PopulationReaderMatsimV0(final Population plans, final Network network) {
		this.plans = plans;
		this.network = network;
	}

	@Override
	public void startTag(final String name, final Attributes atts, final Stack<String> context) {
		if (PERSON.equals(name)) {
			startPerson(atts);
		} else if (PLAN.equals(name)) {
			startPlan(atts);
		} else if (ACT.equals(name)) {
			startAct(atts);
		} else if (LEG.equals(name)) {
			startLeg(atts);
		} else if (ROUTE.equals(name)) {
			startRoute();
		} else if (DEMAND.equals(name)) {
			log.info("The tag <demand> is not supported");
		} else if (!SEGMENT.equals(name) && !MODEL.equals(name) && !PARAM.equals(name) && !PLANS.equals(name)) {
			Gbl.errorMsg(this + "[tag=" + name + " not known or not supported]");
		}
	}

	@Override
	public void endTag(final String name, final String content, final Stack<String> context) {
		if (PERSON.equals(name)) {
			if (this.plans instanceof PopulationImpl) {
				((PopulationImpl) this.plans).addPerson(this.currperson);
			} else {
				this.plans.addPerson(this.currperson);
			}
			this.currperson = null;
		} else if (PLAN.equals(name)) {
			this.currplan = null;
		} else if (LEG.equals(name)) {
			this.currleg = null;
		} else if (ROUTE.equals(name)) {
			this.routeNodes = content;
		}
	}

	/**
	 * Parses the specified plans file. This method calls {@link #parse(String)}, but handles all
	 * possible exceptions on its own.
	 *
	 * @param filename The name of the file to parse.
	 */
	public void readFile(final String filename) {
		try {
			parse(filename);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void startPerson(final Attributes atts) {
		this.currperson = new PersonImpl(new IdImpl(atts.getValue("id")));
	}

	private void startPlan(final Attributes atts) {
		String sel = atts.getValue("selected");
		boolean selected;
		if (sel.equals("yes")) {
			selected = true;
		}
		else if (sel.equals("no")) {
			selected = false;
		}
		else {
			throw new NumberFormatException("Attribute 'selected' of Element 'Plan' is neither 'yes' nor 'no'.");
		}
		this.currplan = this.currperson.createAndAddPlan(selected);
		this.routeNodes = null;

		String scoreString = atts.getValue("score");
		if (scoreString != null) {
			double score = Double.parseDouble(scoreString);
			this.currplan.setScore(score);
		}

	}

	private void startAct(final Attributes atts) {
		if (atts.getValue("zone") != null) {
			log.info("The attribute 'zone' of <act> will be ignored");
		}
		
		Link link = null;
		Coord coord = null;
		ActivityImpl act;
		if (atts.getValue("link") != null) {
			link = this.network.getLinks().get(new IdImpl(atts.getValue("link")));
			act = this.currplan.createAndAddActivity(atts.getValue("type"), link);
			if (atts.getValue("x100") != null && atts.getValue("y100") != null) {
				coord = new CoordImpl(atts.getValue("x100"), atts.getValue("y100"));
				act.setCoord(coord);
			}
		} else if (atts.getValue("x100") != null && atts.getValue("y100") != null) {
			coord = new CoordImpl(atts.getValue("x100"), atts.getValue("y100"));
			act = this.currplan.createAndAddActivity(atts.getValue("type"), coord);
		} else {
			throw new IllegalArgumentException("Either the coords or the link must be specified for an Act.");
		}
		act.setStartTime(Time.parseTime(atts.getValue("start_time")));
		act.setDuration(Time.parseTime(atts.getValue("dur")));
		act.setEndTime(Time.parseTime(atts.getValue("end_time")));
		
		if (this.routeNodes != null) {
			this.currroute.setNodes(this.prevAct.getLink(), NetworkUtils.getNodes(this.network, this.routeNodes), act.getLink());
			this.routeNodes = null;
			this.currroute = null;
		}
		this.prevAct = act;
	}

	private void startLeg(final Attributes atts) {
		this.currleg = this.currplan.createAndAddLeg(TransportMode.valueOf(atts.getValue("mode").toLowerCase()));
		this.currleg.setDepartureTime(Time.parseTime(atts.getValue("dep_time")));
		this.currleg.setTravelTime(Time.parseTime(atts.getValue("trav_time")));
		this.currleg.setArrivalTime(Time.parseTime(atts.getValue("arr_time")));
	}

	private void startRoute() {
		this.currroute = (NetworkRouteWRefs) ((NetworkFactoryImpl) this.network.getFactory()).createRoute(TransportMode.car, this.prevAct.getLink(), this.prevAct.getLink());
		this.currleg.setRoute(this.currroute);
	}

}
