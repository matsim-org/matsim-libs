/* *********************************************************************** *
 * project: org.matsim.*
 * PlansReaderMatsimV4.java
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

import org.apache.log4j.Logger;
import org.matsim.basic.v01.BasicLeg;
import org.matsim.basic.v01.BasicPopulation;
import org.matsim.basic.v01.IdImpl;
import org.matsim.facilities.Activity;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.network.NetworkLayer;
import org.matsim.population.routes.CarRoute;
import org.matsim.utils.io.MatsimXmlParser;
import org.matsim.utils.misc.Time;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * A reader for plans files of MATSim according to <code>plans_v4.dtd</code>.
 *
 * @author mrieser
 * @author balmermi
 */
public class PopulationReaderMatsimV4 extends MatsimXmlParser implements PopulationReader {

	private final static String PLANS = "plans";
	private final static String PERSON = "person";
	private final static String TRAVELCARD = "travelcard";
	private final static String DESIRES = "desires";
	private final static String ACTDUR = "actDur";
	private final static String KNOWLEDGE = "knowledge";
	private final static String ACTIVITYSPACE = "activityspace";
	private final static String PARAM = "param";
	private final static String ACTIVITY = "activity";
	private final static String LOCATION = "location";
	private final static String CAPACITY = "capacity";
	private final static String OPENTIME = "opentime";
	private final static String PLAN = "plan";
	private final static String ACT = "act";
	private final static String LEG = "leg";
	private final static String ROUTE = "route";

	private final BasicPopulation plans;
	private final NetworkLayer network;

	private Person currperson = null;
	private Desires currdesires = null;
	private Knowledge currknowledge = null;
	private ActivitySpace curractspace = null;
	private String curracttype = null;
	private Facility currfacility = null;
	private Activity curractivity = null;
	private Plan currplan = null;
	private Act curract = null;
	private Leg currleg = null;
	private CarRoute currroute = null;

	private Act prevAct = null;
	private CarRoute prevRoute = null;

	private final static Logger log = Logger.getLogger(PopulationReaderMatsimV4.class);

	private int warnPlanTypeCount = 0;

	public PopulationReaderMatsimV4(final BasicPopulation pop, final NetworkLayer network) {
		this.plans = pop;
		this.network = network;
	}

	@Override
	public void startTag(final String name, final Attributes atts, final Stack<String> context) {
		if (PLANS.equals(name)) {
			startPlans(atts);
		} else if (PERSON.equals(name)) {
			startPerson(atts);
		} else if (TRAVELCARD.equals(name)) {
			startTravelcard(atts);
		} else if (DESIRES.equals(name)) {
			startDesires(atts);
		} else if (ACTDUR.equals(name)) {
			startActDur(atts);
		} else if (KNOWLEDGE.equals(name)) {
			startKnowledge(atts);
		} else if (ACTIVITYSPACE.equals(name)) {
			startActivitySpace(atts);
			this.curractspace = this.currknowledge.createActivitySpace(atts.getValue("type"), atts.getValue("activity_type"));
		} else if (PARAM.equals(name)) {
			startParam(atts);
		} else if (ACTIVITY.equals(name)) {
			startActivityFacility(atts);
		} else if (LOCATION.equals(name)) {
			startLocation(atts);
		} else if (CAPACITY.equals(name)) {
			startCapacity();
		} else if (OPENTIME.equals(name)) {
			startOpenTime();
		} else if (PLAN.equals(name)) {
			startPlan(atts);
		} else if (ACT.equals(name)) {
			startAct(atts);
		} else if (LEG.equals(name)) {
			startLeg(atts);
		} else if (ROUTE.equals(name)) {
			startRoute(atts);
		} else {
			Gbl.errorMsg(this + "[tag=" + name + " not known or not supported]");
		}
	}

	@Override
	public void endTag(final String name, final String content, final Stack<String> context) {
		if (PERSON.equals(name)) {
			this.plans.addPerson(this.currperson);
			this.currperson = null;
		} else if (DESIRES.equals(name)) {
			this.currdesires = null;
		} else if (KNOWLEDGE.equals(name)) {
				this.currknowledge = null;
		} else if (ACTIVITYSPACE.equals(name)) {
			if (!this.curractspace.isComplete()) {
				Gbl.errorMsg("[person_id="+this.currperson.getId()+" holds an incomplete act-space!]");
			}
			this.curractspace = null;
		} else if (ACTIVITY.equals(name)) {
			this.curracttype = null;
		} else if (LOCATION.equals(name)) {
			this.currfacility = null;
			this.curractivity = null;
		} else if (PLAN.equals(name)) {
			this.currplan.getActsLegs().trimToSize();
			this.currplan = null;
		} else if (ACT.equals(name)) {
			this.prevAct = this.curract;
			this.curract = null;
		} else if (LEG.equals(name)) {
			this.currleg = null;
		} else if (ROUTE.equals(name)) {
			this.currroute.setNodes(content);
			this.prevRoute = this.currroute;
			this.currroute = null;
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

	private void startPlans(final Attributes atts) {
		this.plans.setName(atts.getValue("name"));
		if (atts.getValue("reference_layer") != null) {
			log.warn("plans.reference_layer is no longer supported.");
		}
	}

	private void startPerson(final Attributes atts) {
		String ageString = atts.getValue("age");
		int age = Integer.MIN_VALUE;
		if (ageString != null)
			age = Integer.parseInt(ageString);
		this.currperson = new PersonImpl(new IdImpl(atts.getValue("id")));
		this.currperson.setSex(atts.getValue("sex"));
		this.currperson.setAge(age);
		this.currperson.setLicence(atts.getValue("license"));
		this.currperson.setCarAvail(atts.getValue("car_avail"));
		this.currperson.setEmployed(atts.getValue("employed"));
	}

	private void startTravelcard(final Attributes atts) {
		this.currperson.addTravelcard(atts.getValue("type"));
	}

	private void startDesires(final Attributes atts) {
		this.currdesires = this.currperson.createDesires(atts.getValue("desc"));
	}

	private void startActDur(final Attributes atts) {
		this.currdesires.putActivityDuration(atts.getValue("type"),atts.getValue("dur"));
	}

	private void startKnowledge(final Attributes atts) {
		this.currknowledge = this.currperson.createKnowledge(atts.getValue("desc"));
	}

	private void startActivitySpace(final Attributes atts) {
		this.curractspace = this.currknowledge.createActivitySpace(atts.getValue("type"), atts.getValue("activity_type"));
	}

	private void startParam(final Attributes atts) {
		this.curractspace.addParam(atts.getValue("name"), atts.getValue("value"));
	}

	private void startActivityFacility(final Attributes atts) {
		this.curracttype = atts.getValue("type");
	}

	private void startLocation(final Attributes atts) {
		String type = atts.getValue("type");
		String id = atts.getValue("id");
		String x = atts.getValue("x");
		String y = atts.getValue("y");
		String freq = atts.getValue("freq");
		String iP = atts.getValue("isPrimary");
		boolean isPrimary = false;
		if ("yes".equals(iP)) { isPrimary = true; }

		if (type != null) { log.info("Attribute type in <location> is deprecated!"); }
		if (id == null) { Gbl.errorMsg("NEW: location must have an id!"); }
		if ((x != null) || (y != null)) { log.info("NEW: coords in <location> will be ignored!"); }
		if (freq != null) { log.info("NEW: Attribute freq in <location> is not supported at the moment!"); }

		this.currfacility = (Facility)Gbl.getWorld().getLayer(Facilities.LAYER_TYPE).getLocation(id);
		if (this.currfacility == null) { Gbl.errorMsg("facility id=" + id + " does not exist!"); }
		this.curractivity = this.currfacility.getActivity(this.curracttype);
		if (this.curractivity == null) { Gbl.errorMsg("facility id=" + id + ": Activity of type=" + this.curracttype + " does not exist!"); }
		this.currknowledge.addActivity(this.curractivity,isPrimary);
	}

	private void startCapacity() {
		log.warn("<capcity> will be ignored!");
	}

	private void startOpenTime() {
		log.warn("<opentime> will be ignored!");
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
		this.prevRoute = null;
		this.currplan = this.currperson.createPlan(selected);

		String scoreString = atts.getValue("score");
		if (scoreString != null) {
			double score = Double.parseDouble(scoreString);
			this.currplan.setScore(score);
		}

		String type = atts.getValue("type");
		if (type == null) {
			this.currplan.setType(Plan.Type.UNDEFINED);
		}
		else if ("car".equalsIgnoreCase(type)) {
				this.currplan.setType(Plan.Type.CAR);
		}
		else if ("pt".equalsIgnoreCase(type)) {
			this.currplan.setType(Plan.Type.PT);
		}
		else if ("ride".equalsIgnoreCase(type)) {
			this.currplan.setType(Plan.Type.RIDE);
		}
		else if ("bike".equalsIgnoreCase(type)) {
			this.currplan.setType(Plan.Type.BIKE);
		}
		else if ("walk".equalsIgnoreCase(type)) {
			this.currplan.setType(Plan.Type.WALK);
		}
		else {
			if (this.warnPlanTypeCount < 10 ) {
				log.warn("Type " + type + " of plan not known! Setting plan to type undefined!");
				this.warnPlanTypeCount++;
			}
			this.currplan.setType(Plan.Type.UNDEFINED);
		}
	}

	private void startAct(final Attributes atts) {
		this.curract = this.currplan.createAct(atts.getValue("type"), atts.getValue("x"), atts.getValue("y"), atts.getValue("link"),
				atts.getValue("start_time"), atts.getValue("end_time"), atts.getValue("dur"),null);
		if (atts.getValue("facility") != null) {
			this.curract.setFacility(atts.getValue("facility"));
		}
		if (this.prevRoute != null) {
			this.prevRoute.setEndLink(this.curract.getLink());
		}
	}

	private void startLeg(final Attributes atts) {
		this.currleg = this.currplan.createLeg(BasicLeg.Mode.valueOf(atts.getValue("mode").toLowerCase()));
		this.currleg.setDepartureTime(Time.parseTime(atts.getValue("dep_time")));
		this.currleg.setTravelTime(Time.parseTime(atts.getValue("trav_time")));
		this.currleg.setArrivalTime(Time.parseTime(atts.getValue("arr_time")));
	}

	private void startRoute(final Attributes atts) {
		this.currroute = (CarRoute) this.network.getFactory().createRoute(BasicLeg.Mode.car);
		this.currleg.setRoute(this.currroute);
		if (atts.getValue("dist") != null) {
			this.currroute.setDist(Double.parseDouble(atts.getValue("dist")));
		}
		if (atts.getValue("trav_time") != null) {
			this.currroute.setTravelTime(Time.parseTime(atts.getValue("trav_time")));
		}
		this.currroute.setStartLink(this.prevAct.getLink());
	}

}
