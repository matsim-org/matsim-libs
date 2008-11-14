/* *********************************************************************** *
 * project: org.matsim.*
 * PopulationWriterV5.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.matsim.basic.v01.BasicAct;
import org.matsim.basic.v01.BasicActivity;
import org.matsim.basic.v01.BasicKnowledge;
import org.matsim.basic.v01.BasicLeg;
import org.matsim.basic.v01.BasicOpeningTime;
import org.matsim.basic.v01.BasicPerson;
import org.matsim.basic.v01.BasicPlan;
import org.matsim.basic.v01.BasicPopulation;
import org.matsim.basic.v01.BasicRoute;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.MatsimCommonWriter;
import org.matsim.basic.v01.PopulationSchemaV5Names;
import org.matsim.basic.v01.BasicOpeningTime.DayType;
import org.matsim.basic.v01.BasicPlanImpl.ActLegIterator;
import org.matsim.interfaces.basic.v01.BasicHousehold;
import org.matsim.interfaces.networks.basicNet.BasicNode;
import org.matsim.utils.collections.Tuple;
import org.matsim.utils.misc.Time;
import org.matsim.writer.MatsimXmlWriter;

/**
 * @author dgrether
 */
public class PopulationWriterV5 extends MatsimXmlWriter  {

	private final List<Tuple<String, String>> atts = new ArrayList<Tuple<String, String>>();

	private final BasicPopulation<BasicPerson<BasicPlan, BasicKnowledge<BasicActivity>>> population;

	private List<BasicHousehold> households;

	private MatsimCommonWriter matsimCommonWriter;

	public PopulationWriterV5(final BasicPopulation<BasicPerson<BasicPlan, BasicKnowledge<BasicActivity>>> pop) {
		this.population = pop;
	}

	public PopulationWriterV5(final BasicPopulation<BasicPerson<BasicPlan, BasicKnowledge<BasicActivity>>> pop, final List<BasicHousehold> hh) {
		this.population = pop;
		this.households = hh;
	}

	public void writeFile(final String filename) throws FileNotFoundException, IOException {
		this.openFile(filename);
		this.matsimCommonWriter = new MatsimCommonWriter(this.writer);
		this.writeXmlHead();
		this.writePopulation(this.population);
		this.close();
	}

	private void writePopulation(
			final BasicPopulation<BasicPerson<BasicPlan, BasicKnowledge<BasicActivity>>> pop) throws IOException {
		this.atts.clear();
		this.atts.add(this.createTuple(XMLNS, MatsimXmlWriter.MATSIM_NAMESPACE));
		this.atts.add(this.createTuple(XMLNS + ":xsi", DEFAULTSCHEMANAMESPACELOCATION));
		this.atts.add(this.createTuple("xsi:schemaLocation", MATSIM_NAMESPACE + " " + DEFAULT_DTD_LOCATION + "population_v5.0.xsd"));
		this.writeStartTag(PopulationSchemaV5Names.POPULATION, this.atts);
		this.writePersons(pop.getPersons().values());
		if (this.households != null) {
			HouseholdsWriterV1 hhwriter = new HouseholdsWriterV1(this.households);
			hhwriter.writeToWriter(this.writer, this.getIndentationLevel());
		}
		this.writeEndTag(PopulationSchemaV5Names.POPULATION);
	}

	private void writePersons(final Collection<BasicPerson<BasicPlan, BasicKnowledge<BasicActivity>>> persons) throws IOException {
		for (BasicPerson<BasicPlan, BasicKnowledge<BasicActivity>> p : persons) {
			this.atts.clear();
			this.atts.add(this.createTuple(PopulationSchemaV5Names.ID, p.getId().toString()));
			if (p.getSex() != null) {
				this.atts.add(this.createTuple(PopulationSchemaV5Names.SEX, p.getSex()));
			}
			if (p.getAge() != Integer.MIN_VALUE)
				this.atts.add(this.createTuple(PopulationSchemaV5Names.AGE, p.getAge()));
			if (p.getLicense() != null)
				this.atts.add(this.createTuple(PopulationSchemaV5Names.LICENSE, p.hasLicense()));
			if (p.getCarAvail() != null)
				this.atts.add(this.createTuple(PopulationSchemaV5Names.CARAVAILABLE, p.getCarAvail()));
			if (p.getEmployed() != null)
				this.atts.add(this.createTuple(PopulationSchemaV5Names.ISEMPLOYED, p.isEmployed()));
			this.writeStartTag(PopulationSchemaV5Names.PERSON, this.atts);
			writeTravelCards(p.getTravelcards());
			writeKnowledge(p.getKnowledge());
			writePlans(p.getPlans());
			//TODO [DG]
			if (p.getFiscalHouseholdId() != null) {
				this.atts.clear();
				this.atts.add(this.createTuple(PopulationSchemaV5Names.REFID, p.getFiscalHouseholdId().toString()));
				this.writeStartTag(PopulationSchemaV5Names.FISCALHOUSEHOLDID, this.atts, true);
			}
			this.writeEndTag(PopulationSchemaV5Names.PERSON);
		}
	}

	private void writeKnowledge(final BasicKnowledge<BasicActivity> k) throws IOException {
		if (k != null) {
			this.writeStartTag(PopulationSchemaV5Names.KNOWLEDGE, null);
			if (k.getDescription() != null) {
				this.writeStartTag(PopulationSchemaV5Names.DESCRIPTION, null);
				this.writeContent(k.getDescription(), false);
				this.writeEndTag(PopulationSchemaV5Names.DESCRIPTION);
			}
			if (k.getActivities() != null) {
				for (BasicActivity ba : k.getActivities()) {
					this.writeActivity(ba);
				}
			}
			this.writeEndTag(PopulationSchemaV5Names.KNOWLEDGE);
		}
	}

	private void writeActivity(final BasicActivity ba) throws IOException {
		this.atts.clear();
		this.atts.add(this.createTuple(PopulationSchemaV5Names.TYPE, ba.getType()));
		if (ba.getFrequency() != null) {
			this.atts.add(this.createTuple(PopulationSchemaV5Names.FREQUENCY, ba.getFrequency()));
		}
		this.writeStartTag(PopulationSchemaV5Names.ACTIVITY, this.atts);
		this.matsimCommonWriter.writeLocation(ba.getLocation(), this.getIndentationLevel());
		if (ba.getCapacity() != null) {
			this.atts.clear();
			this.atts.add(this.createTuple(PopulationSchemaV5Names.PERSONS, ba.getCapacity()));
			this.writeStartTag(PopulationSchemaV5Names.CAPACITY, this.atts, true);
		}
		for (DayType dt : DayType.values()) {
			SortedSet<BasicOpeningTime> ot = ba.getOpeningTime(dt);
			if (ot != null) {
				for (BasicOpeningTime bot : ot) {
					this.writeOpeningTime(bot);
				}
			}
		}
		this.writeEndTag(PopulationSchemaV5Names.ACTIVITY);
	}

	private void writeOpeningTime(final BasicOpeningTime bot) throws IOException {
		this.atts.clear();
		this.atts.add(this.createTuple(PopulationSchemaV5Names.DAY, bot.getDay().toString()));
		this.atts.add(this.createTimeTuple(PopulationSchemaV5Names.STARTTIME, bot.getStartTime()));
		this.atts.add(this.createTimeTuple(PopulationSchemaV5Names.ENDTIME, bot.getEndTime()));
		this.writeStartTag(PopulationSchemaV5Names.OPENINGTIME, this.atts, true);
	}

	private void writePlans(final List<BasicPlan> plans) throws IOException {
		for (BasicPlan p : plans) {
			this.atts.clear();
			if (!p.hasUndefinedScore())
				this.atts.add(this.createTuple(PopulationSchemaV5Names.SCORE, p.getScore()));
			this.atts.add(this.createTuple(PopulationSchemaV5Names.SELECTED, p.isSelected()));
			this.writeStartTag(PopulationSchemaV5Names.PLAN, this.atts);
			ActLegIterator it = p.getIterator();
			while (it.hasNextLeg()) {
				this.writeAct(it.nextAct());
				this.writeLeg(it.nextLeg());
			}
			this.writeAct(it.nextAct());

			this.writeEndTag(PopulationSchemaV5Names.PLAN);
		}
	}

	private void writeLeg(final BasicLeg leg) throws IOException {
		this.atts.clear();
		this.atts.add(this.createTuple(PopulationSchemaV5Names.MODE, leg.getMode().toString()));
		if (leg.getDepartureTime() != Time.UNDEFINED_TIME) {
			this.atts.add(this.createTimeTuple(PopulationSchemaV5Names.DEPARTURETIME, leg.getDepartureTime()));
		}
		if (leg.getArrivalTime() != Time.UNDEFINED_TIME){
			this.atts.add(this.createTimeTuple(PopulationSchemaV5Names.ARRIVALTIME, leg.getArrivalTime()));
		}
		if (leg.getTravelTime() != Time.UNDEFINED_TIME){
			this.atts.add(this.createTimeTuple(PopulationSchemaV5Names.TRAVELTIME, leg.getTravelTime()));
		}

		this.writeStartTag(PopulationSchemaV5Names.LEG, this.atts);
		if (leg.getRoute() != null) {
			this.writeRoute(leg.getRoute());
		}
		this.writeEndTag(PopulationSchemaV5Names.LEG);
	}

	private void writeRoute(final BasicRoute<BasicNode> route) throws IOException {
		this.atts.clear();
		if (route.getTravTime() != Time.UNDEFINED_TIME) {
			this.atts.add(this.createTimeTuple(PopulationSchemaV5Names.TRAVELTIME, route.getTravTime()));
		}
		if (!Double.isNaN(route.getDist())) {
			this.atts.add(this.createTuple(PopulationSchemaV5Names.DISTANCE, route.getDist()));
		}
		this.writeStartTag(PopulationSchemaV5Names.ROUTE, this.atts);
		for (Id id : route.getLinkIds()) {
			this.atts.clear();
			this.atts.add(this.createTuple(PopulationSchemaV5Names.REFID, id.toString()));
			this.writeStartTag(PopulationSchemaV5Names.LINK, this.atts, true);
		}
		this.writeEndTag(PopulationSchemaV5Names.ROUTE);
	}

	private void writeAct(final BasicAct act) throws IOException {
		this.atts.clear();
		this.atts.add(this.createTuple(PopulationSchemaV5Names.TYPE, act.getType()));
		if (act.getStartTime() != Time.UNDEFINED_TIME)
			this.atts.add(this.createTimeTuple(PopulationSchemaV5Names.STARTTIME, act.getStartTime()));
		if (act.getDuration() != Time.UNDEFINED_TIME)
			this.atts.add(this.createTimeTuple(PopulationSchemaV5Names.DURATION, act.getDuration()));
		if (act.getEndTime() != Time.UNDEFINED_TIME)
			this.atts.add(this.createTimeTuple(PopulationSchemaV5Names.ENDTIME, act.getEndTime()));

		this.writeStartTag(PopulationSchemaV5Names.ACT, this.atts);
		this.matsimCommonWriter.writeLocation(act.getLinkId(), act.getFacilityId(), act.getCoord(), this.getIndentationLevel());

		this.writeEndTag(PopulationSchemaV5Names.ACT);
	}

	private void writeTravelCards(final TreeSet<String> travelcards) throws IOException {
		this.writeStartTag(PopulationSchemaV5Names.TRAVELCARD, null);
		for (String tc : travelcards) {
			this.writeStartTag(PopulationSchemaV5Names.SWISSTRAVELCARD, null);
			this.writeContent(tc.trim(), false);
			this.writeEndTag(PopulationSchemaV5Names.SWISSTRAVELCARD);
		}
		this.writeEndTag(PopulationSchemaV5Names.TRAVELCARD);
	}

}
