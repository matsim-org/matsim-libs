/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
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
 *
 */
public class PopulationWriterV5 extends MatsimXmlWriter  {

	private List<Tuple<String, String>> atts = new ArrayList<Tuple<String, String>>();
	
	private BasicPopulation<BasicPerson<BasicPlan, BasicKnowledge<BasicActivity>>> population;

	private List<BasicHousehold> households;

	private MatsimCommonWriter matsimCommonWriter;
	
	public PopulationWriterV5(BasicPopulation<BasicPerson<BasicPlan, BasicKnowledge<BasicActivity>>> pop) {
		this.population = pop;
	}
	
	public PopulationWriterV5(BasicPopulation<BasicPerson<BasicPlan, BasicKnowledge<BasicActivity>>> pop, List<BasicHousehold> hh) {
		this.population = pop;
		this.households = hh;
	}
	
	
	public void writeFile(String filename) throws FileNotFoundException, IOException {
		this.openFile(filename);
		this.matsimCommonWriter = new MatsimCommonWriter(this.writer);
		this.writeXmlHead();
		this.writePopulation(this.population);
		this.close();
	}
	
	private void writePopulation(
			BasicPopulation<BasicPerson<BasicPlan, BasicKnowledge<BasicActivity>>> pop) throws IOException {
		atts.clear();
		atts.add(this.createTuple(XMLNS, MatsimXmlWriter.MATSIM_NAMESPACE));
		atts.add(this.createTuple(XMLNS + ":xsi", DEFAULTSCHEMANAMESPACELOCATION));
		atts.add(this.createTuple("xsi:schemaLocation", MATSIM_NAMESPACE + " " + DEFAULT_DTD_LOCATION + "population_v5.00.xsd"));
		this.writeStartTag(PopulationSchemaV5Names.POPULATION, atts);
		this.writePersons(pop.getPersons().values());
		if (this.households != null) {
			HouseholdsWriterV1 hhwriter = new HouseholdsWriterV1(this.households);
			hhwriter.writeToWriter(this.writer, this.getIndentationLevel());
		}
		this.writeEndTag(PopulationSchemaV5Names.POPULATION);
	}

	
	
	private void writePersons(
			Collection<BasicPerson<BasicPlan, BasicKnowledge<BasicActivity>>> persons) throws IOException {
		for (BasicPerson<BasicPlan, BasicKnowledge<BasicActivity>> p : persons) {
			atts.clear();
			atts.add(this.createTuple(PopulationSchemaV5Names.ID, p.getId().toString()));
			if (p.getSex() != null) {
				atts.add(this.createTuple(PopulationSchemaV5Names.SEX, p.getSex()));
			}
			if (p.getAge() != Integer.MIN_VALUE)
				atts.add(this.createTuple(PopulationSchemaV5Names.AGE, p.getAge()));
			if (p.getLicense() != null)
				atts.add(this.createTuple(PopulationSchemaV5Names.LICENSE, p.hasLicense()));
			if (p.getCarAvail() != null)
				atts.add(this.createTuple(PopulationSchemaV5Names.CARAVAILABLE, p.getCarAvail()));
			if (p.getEmployed() != null)
				atts.add(this.createTuple(PopulationSchemaV5Names.ISEMPLOYED, p.isEmployed()));
			this.writeStartTag(PopulationSchemaV5Names.PERSON, atts);
			writeTravelCards(p.getTravelcards());
			writeKnowledge(p.getKnowledge());
			writePlans(p.getPlans());
			//TODO
			if (p.getFiscalHouseholdId() != null) {
				atts.clear();
				atts.add(this.createTuple(PopulationSchemaV5Names.REFID, p.getFiscalHouseholdId().toString()));
				this.writeStartTag(PopulationSchemaV5Names.FISCALHOUSEHOLDID, atts, true);
			}
			this.writeEndTag(PopulationSchemaV5Names.PERSON);
		}
	}

	private void writeKnowledge(BasicKnowledge<BasicActivity> k) throws IOException {
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

	private void writeActivity(BasicActivity ba) throws IOException {
		atts.clear();
		atts.add(this.createTuple(PopulationSchemaV5Names.TYPE, ba.getType()));
		if (ba.getFrequency() != null) {
			atts.add(this.createTuple(PopulationSchemaV5Names.FREQUENCY, ba.getFrequency()));
		}
		this.writeStartTag(PopulationSchemaV5Names.ACTIVITY, atts);
		this.matsimCommonWriter.writeLocation(ba.getLocation(), this.getIndentationLevel());
		if (ba.getCapacity() != null) {
			atts.clear();
			atts.add(this.createTuple(PopulationSchemaV5Names.PERSONS, ba.getCapacity()));
			this.writeStartTag(PopulationSchemaV5Names.CAPACITY, atts, true);
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

	private void writeOpeningTime(BasicOpeningTime bot) throws IOException {
		atts.clear();
		atts.add(this.createTuple(PopulationSchemaV5Names.DAY, bot.getDay().toString()));
		atts.add(this.createTimeTuple(PopulationSchemaV5Names.STARTTIME, bot.getStartTime()));
		atts.add(this.createTimeTuple(PopulationSchemaV5Names.ENDTIME, bot.getEndTime()));
		this.writeStartTag(PopulationSchemaV5Names.OPENINGTIME, atts, true);
	}


	private void writePlans(List<BasicPlan> plans) throws IOException {
		for (BasicPlan p : plans) {
			atts.clear();
			if (!p.hasUndefinedScore())
				atts.add(this.createTuple(PopulationSchemaV5Names.SCORE, p.getScore()));
			atts.add(this.createTuple(PopulationSchemaV5Names.SELECTED, p.isSelected()));
			this.writeStartTag(PopulationSchemaV5Names.PLAN, atts);
			ActLegIterator it = p.getIterator(); 
			while (it.hasNextLeg()) {
				this.writeAct(it.nextAct());
				this.writeLeg(it.nextLeg());
			}
			this.writeAct(it.nextAct());
			
			this.writeEndTag(PopulationSchemaV5Names.PLAN);
		}
	}

	private void writeLeg(BasicLeg leg) throws IOException {
		atts.clear();
		atts.add(this.createTuple(PopulationSchemaV5Names.MODE, leg.getMode().toString()));
		if (leg.getDepartureTime() != Time.UNDEFINED_TIME) {
			atts.add(this.createTimeTuple(PopulationSchemaV5Names.DEPARTURETIME, leg.getDepartureTime()));
		}
		if (leg.getArrivalTime() != Time.UNDEFINED_TIME){
			atts.add(this.createTimeTuple(PopulationSchemaV5Names.ARRIVALTIME, leg.getArrivalTime()));
		}
		if (leg.getTravelTime() != Time.UNDEFINED_TIME){
			atts.add(this.createTimeTuple(PopulationSchemaV5Names.TRAVELTIME, leg.getTravelTime()));
		}
		
		this.writeStartTag(PopulationSchemaV5Names.LEG, atts);
		if (leg.getRoute() != null) {
			this.writeRoute(leg.getRoute());
		}
		this.writeEndTag(PopulationSchemaV5Names.LEG);
	}

	private void writeRoute(BasicRoute<BasicNode> route) throws IOException {
		atts.clear();
		if (route.getTravTime() != Time.UNDEFINED_TIME) {
			atts.add(this.createTimeTuple(PopulationSchemaV5Names.TRAVELTIME, route.getTravTime()));
		}
		if (!Double.isNaN(route.getDist())) {
			atts.add(this.createTuple(PopulationSchemaV5Names.DISTANCE, route.getDist()));
		}
		this.writeStartTag(PopulationSchemaV5Names.ROUTE, atts);
		for (Id id : route.getLinkIds()) {
			atts.clear();
			atts.add(this.createTuple(PopulationSchemaV5Names.REFID, id.toString()));
			this.writeStartTag(PopulationSchemaV5Names.LINK, atts, true);
		}
		this.writeEndTag(PopulationSchemaV5Names.ROUTE);
	}


	private void writeAct(BasicAct act) throws IOException {
		atts.clear();
		atts.add(this.createTuple(PopulationSchemaV5Names.TYPE, act.getType()));		
		if (act.getStartTime() != Time.UNDEFINED_TIME)
			atts.add(this.createTimeTuple(PopulationSchemaV5Names.STARTTIME, act.getStartTime()));
		if (act.getDuration() != Time.UNDEFINED_TIME)
			atts.add(this.createTimeTuple(PopulationSchemaV5Names.DURATION, act.getDuration()));
		if (act.getEndTime() != Time.UNDEFINED_TIME)
			atts.add(this.createTimeTuple(PopulationSchemaV5Names.ENDTIME, act.getEndTime()));
		
		this.writeStartTag(PopulationSchemaV5Names.ACT, atts);
		this.matsimCommonWriter.writeLocation(act.getLinkId(), act.getFacilityId(), act.getCoord(), this.getIndentationLevel());

		this.writeEndTag(PopulationSchemaV5Names.ACT);
	}



	

	
	private void writeTravelCards(TreeSet<String> travelcards) throws IOException {
		this.writeStartTag(PopulationSchemaV5Names.TRAVELCARD, null);
		for (String tc : travelcards) {
			this.writeStartTag(PopulationSchemaV5Names.SWISSTRAVELCARD, null);
			this.writeContent(tc.trim(), false);
			this.writeEndTag(PopulationSchemaV5Names.SWISSTRAVELCARD);
		}
		this.writeEndTag(PopulationSchemaV5Names.TRAVELCARD);
	}

}
