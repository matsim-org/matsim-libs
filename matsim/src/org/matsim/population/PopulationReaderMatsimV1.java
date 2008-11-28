/* *********************************************************************** *
 * project: org.matsim.*
 * PlansReaderMatsimV1.java
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

package org.matsim.population;

import java.io.IOException;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.basic.v01.BasicLeg;
import org.matsim.basic.v01.IdImpl;
import org.matsim.gbl.Gbl;
import org.matsim.population.routes.CarRoute;
import org.matsim.utils.io.MatsimXmlParser;
import org.matsim.utils.misc.Time;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * A reader for plans files of MATSim according to <code>plans_v1.dtd</code>.
 *
 * @author mrieser
 * @author balmermi
 */
public class PopulationReaderMatsimV1 extends MatsimXmlParser implements
		PopulationReader {

	private final static String PLANS = "plans";

	private final static String PERSON = "person";

	private final static String PLAN = "plan";

	private final static String ACT = "act";

	private final static String LEG = "leg";

	private final static String ROUTE = "route";

	private final Population plans;

	private Person currperson = null;

	private Plan currplan = null;

	private Leg currleg = null;

	private CarRoute currroute = null;

	private Act prevAct = null;
	private CarRoute prevRoute = null;

	public PopulationReaderMatsimV1(final Population plans) {
		this.plans = plans;
	}

	@Override
	public void startTag(final String name, final Attributes atts,
			final Stack<String> context) {
		if (PLANS.equals(name)) {
			startPlans(atts);
		}
		else if (PERSON.equals(name)) {
			startPerson(atts);
		}
		else if (PLAN.equals(name)) {
			startPlan(atts);
		}
		else if (ACT.equals(name)) {
			startAct(atts);
		}
		else if (LEG.equals(name)) {
			startLeg(atts);
		}
		else if (ROUTE.equals(name)) {
			startRoute(atts);
		}
		else {
			Gbl.errorMsg(this + "[tag=" + name + " not known or not supported]");
		}
	}

	@Override
	public void endTag(final String name, final String content,
			final Stack<String> context) {
		if (PERSON.equals(name)) {
			this.plans.addPerson(this.currperson);
			this.currperson = null;
		}
		else if (PLAN.equals(name)) {
			this.currplan.getActsLegs().trimToSize();
			this.currplan = null;
		}
		else if (LEG.equals(name)) {
			this.currleg = null;
		}
		else if (ROUTE.equals(name)) {
			this.currroute.setNodes(content);
			this.prevRoute = this.currroute;
			this.currroute = null;
		}
	}

	/**
	 * Parses the specified plans file. This method calls {@link #parse(String)},
	 * but handles all possible exceptions on its own.
	 *
	 * @param filename
	 *          The name of the file to parse.
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

	private void startPlans(final Attributes atts) {
		this.plans.setName(atts.getValue("name"));
	}

	private void startPerson(final Attributes atts) {
		this.currperson = new PersonImpl(new IdImpl(atts.getValue("id")));
		this.currperson.setSex(atts.getValue("sex"));
		this.currperson.setAge(Integer.parseInt(atts.getValue("age")));
		this.currperson.setLicence(atts.getValue("license"));
		this.currperson.setCarAvail(atts.getValue("car_avail"));
		this.currperson.setEmployed(atts.getValue("employed"));
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
			throw new NumberFormatException(
					"Attribute 'selected' of Element 'Plan' is neither 'yes' nor 'no'.");
		}
		this.currplan = this.currperson.createPlan(selected);

		String scoreString = atts.getValue("score");
		if (scoreString != null) {
			double score = Double.parseDouble(scoreString);
			this.currplan.setScore(score);
		}

	}

	private void startAct(final Attributes atts) {
		this.prevAct = this.currplan.createAct(atts.getValue("type"), atts.getValue("x100"),
					atts.getValue("y100"), atts.getValue("link"), atts
							.getValue("start_time"), atts.getValue("end_time"), atts
							.getValue("dur"), null);
		if (this.prevRoute != null) {
			this.prevRoute.setEndLink(this.prevAct.getLink());
		}
	}

	private void startLeg(final Attributes atts) {
		this.currleg = this.currplan.createLeg(BasicLeg.Mode.valueOf(atts.getValue("mode").toLowerCase()));
		this.currleg.setDepartureTime(Time.parseTime(atts.getValue("dep_time")));
		this.currleg.setTravelTime(Time.parseTime(atts.getValue("trav_time")));
		this.currleg.setArrivalTime(Time.parseTime(atts.getValue("arr_time")));
	}

	private void startRoute(final Attributes atts) {
		this.currroute = this.currleg.createRoute();
		if (atts.getValue("dist") != null) {
			this.currroute.setDist(Double.parseDouble(atts.getValue("dist")));
		}
		if (atts.getValue("trav_time") != null) {
			this.currroute.setTravTime(Time.parseTime(atts.getValue("trav_time")));
		}
		this.currroute.setStartLink(this.prevAct.getLink());
	}

}
