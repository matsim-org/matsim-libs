/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.basic.v01;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.BasicOpeningTime.DayType;
import org.matsim.facilities.OpeningTime;
import org.matsim.interfaces.basic.v01.BasicAct;
import org.matsim.interfaces.basic.v01.BasicLeg;
import org.matsim.interfaces.basic.v01.BasicLocation;
import org.matsim.interfaces.basic.v01.BasicPerson;
import org.matsim.interfaces.basic.v01.BasicPlan;
import org.matsim.interfaces.basic.v01.BasicPopulation;
import org.matsim.basic.v01.BasicPopulationBuilder;
import org.matsim.interfaces.basic.v01.BasicRoute;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.population.Knowledge;
import org.matsim.population.PersonImpl;
import org.matsim.population.PopulationReader;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.utils.io.MatsimXmlParser;
import org.matsim.utils.misc.Time;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * @author dgrether
 */
public class BasicPopulationReaderV5 extends MatsimXmlParser implements PopulationReader{
	
	private static final Logger log = Logger
			.getLogger(BasicPopulationReaderV5.class);
	
	private BasicPopulationBuilder populationBuilder;

	private BasicPerson currentPerson;

	private BasicPlan currentPlan;

	private String currentActType;

	private double currentDuration;

	private double currentStartTime;

	private double currentEndTime;

	private BasicLocation currentlocation;

	private Double currentXCoord;

	private Double currentYCoord;

	private BasicAct currentAct;

	private BasicLeg currentLeg;
	
	private List<Id> currentRouteLinkIds = new ArrayList<Id>();
	
	private Id currentStartLinkId;
	
	private Id currentEndLinkId;

	private BasicRoute currentRoute;

	private Double currentDistance;

	private Double currentTravelTime;

	private HouseholdBuilder householdBuilder;
	
	private BasicHouseholdsReaderV1 householdsDelegate = null;

	private Map<Id, BasicHousehold> households;

	private List<BasicActivity> currentActivities = new ArrayList<BasicActivity>();

	private String currentDescription;

	private Integer currentCapacity;

	private OpeningTime currentOpeningTime;

	private BasicKnowledge currentKnowledge;

	private Integer currentFrequency;
	
	protected BasicPopulationReaderV5() {
	}
		
  public BasicPopulationReaderV5(BasicPopulation pop, Map<Id, BasicHousehold> households) {
  	this.populationBuilder = new BasicPopulationBuilderImpl(pop);
  	this.householdBuilder = new BasicHouseholdBuilder(households);
  	this.households = households;
  }
	
  protected void setPopulationBuilder(BasicPopulationBuilder populationBuilder) {
  	this.populationBuilder = populationBuilder;
  }
  
  protected void setHouseholdBuilder(HouseholdBuilder householdBuilder) {
  	this.householdBuilder = householdBuilder;
  	this.households = householdBuilder.getHouseholds();
  }
	
	@Override
	public void endTag(String name, String content, Stack<String> context) {
		if (PopulationSchemaV5Names.POPULATION.equalsIgnoreCase(name)) {
			this.householdsDelegate = null;
		}
		else if (this.householdsDelegate != null) {
			this.householdsDelegate.endTag(name, content, context);
		}
		else if (PopulationSchemaV5Names.COORDINATE.equalsIgnoreCase(name)) {
			((BasicLocationImpl)this.currentlocation).setCoord(new CoordImpl(this.currentXCoord, this.currentYCoord));
			this.currentXCoord = null;
			this.currentYCoord = null;
		}
		else if (PopulationSchemaV5Names.XCOORD.equalsIgnoreCase(name)) {
			this.currentXCoord = Double.valueOf(content);
		}
		else if (PopulationSchemaV5Names.YCOORD.equalsIgnoreCase(name)) {
			this.currentYCoord = Double.valueOf(content);
		}
		else if (PopulationSchemaV5Names.ACT.equalsIgnoreCase(name)){
			this.currentAct = this.populationBuilder.createAct(this.currentPlan, this.currentActType, this.currentlocation);		
		  this.currentAct.setEndTime(currentEndTime);
		  this.currentAct.setStartTime(currentStartTime);
		  this.currentAct.setDuration(currentDuration);
		  this.currentActType = null;
		  this.currentlocation = null;
		}
		else if (PopulationSchemaV5Names.ROUTE.equalsIgnoreCase(name)) {
			this.currentRoute = this.populationBuilder.createRoute(this.currentStartLinkId, this.currentEndLinkId, this.currentRouteLinkIds);
			if (null != this.currentDistance) {
				this.currentRoute.setDist(this.currentDistance);
			}
			if (null != this.currentTravelTime) {
				this.currentRoute.setTravelTime(this.currentTravelTime);
			}
			this.currentRouteLinkIds = null;
			this.currentDistance = null;
			this.currentTravelTime = null;
		}
		else if (PopulationSchemaV5Names.LEG.equalsIgnoreCase(name)){
			this.currentLeg.setRoute(this.currentRoute);
			this.currentRoute = null;
		}
		else if (PopulationSchemaV5Names.SWISSTRAVELCARD.equalsIgnoreCase(name)) {
			this.currentPerson.addTravelcard(content.trim());
		}
		else if (PopulationSchemaV5Names.DESCRIPTION.equalsIgnoreCase(name)){
			this.currentDescription = content.trim();
		}
		else if (PopulationSchemaV5Names.ACTIVITY.equalsIgnoreCase(name)){
			BasicActivity act = populationBuilder.createActivity(this.currentActType, this.currentlocation);
			this.currentActType = null;
			act.setCapacity(this.currentCapacity);
			this.currentCapacity = null;
			if (this.currentOpeningTime != null) {
				act.addOpeningTime(this.currentOpeningTime);
				this.currentOpeningTime = null;
			}
			if (this.currentFrequency != null) {
				act.setFrequency(this.currentFrequency);
			}
			this.currentActivities.add(act);
		}
		else if (PopulationSchemaV5Names.KNOWLEDGE.equalsIgnoreCase(name)){
			this.currentKnowledge = populationBuilder.createKnowledge(this.currentActivities);
			this.currentActivities.clear();
			if (this.currentDescription != null) {
				this.currentKnowledge.setDescription(this.currentDescription);
				this.currentDescription = null;
			}
			//the next lines should be placed in the PopulationReaderMatsim class
			//however this conceptual cleaness would produce duplicate code 
			//and is avoided in this case. dg nov 08
			if (this.currentPerson instanceof BasicPersonImpl) {
				((BasicPersonImpl)this.currentPerson).setKnowledge(this.currentKnowledge);				
			}
			else {
				((PersonImpl)this.currentPerson).setKnowledge((Knowledge)this.currentKnowledge);				
			}
			this.currentKnowledge = null;
		}
	} //end of endTag

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if (HouseholdsSchemaV1Names.HOUSEHOLD.equalsIgnoreCase(name)) {
			this.householdsDelegate = new BasicHouseholdsReaderV1(this.households);
			this.householdsDelegate.setHouseholdBuilder(this.householdBuilder);
		  this.householdsDelegate.startTag(name, atts, context);
		}
		else if (this.householdsDelegate != null) {
			this.householdsDelegate.startTag(name, atts, context);
		}
		else if (PopulationSchemaV5Names.PERSON.equalsIgnoreCase(name)) {
			try {
				this.currentPerson = populationBuilder.createPerson(getId(atts));
				for (int i = 0; i < atts.getLength(); i++) {
					if (atts.getLocalName(i).equalsIgnoreCase(PopulationSchemaV5Names.SEX)){
						this.currentPerson.setSex(atts.getValue(i));
					}
					else if (atts.getLocalName(i).equalsIgnoreCase(PopulationSchemaV5Names.AGE)){
						this.currentPerson.setAge(Integer.parseInt(atts.getValue(i)));
					}
					else if (atts.getLocalName(i).equalsIgnoreCase(PopulationSchemaV5Names.LICENSE)){
						this.currentPerson.setLicence(atts.getValue(i));
					}
					else if (atts.getLocalName(i).equalsIgnoreCase(PopulationSchemaV5Names.CARAVAILABLE)){
						this.currentPerson.setCarAvail(atts.getValue(i));
					}
					else if (atts.getLocalName(i).equalsIgnoreCase(PopulationSchemaV5Names.ISEMPLOYED)){
						this.currentPerson.setEmployed(atts.getValue(i));
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				throw new IllegalArgumentException("person exists twice!", e);
			}
		}
		else if (PopulationSchemaV5Names.PLAN.equalsIgnoreCase(name)) {
			if ("true".equalsIgnoreCase(atts.getValue(PopulationSchemaV5Names.SELECTED))) {
				this.currentPlan = populationBuilder.createPlan(this.currentPerson, true);
			}
			else {
				this.currentPlan = populationBuilder.createPlan(this.currentPerson);
			}
			String scoreString = atts.getValue(PopulationSchemaV5Names.SCORE);
			if (null != scoreString) {
				this.currentPlan.setScore(Double.parseDouble(scoreString));
			}
		}
		else if (PopulationSchemaV5Names.ACT.equalsIgnoreCase(name)) {
			this.currentActType = atts.getValue(PopulationSchemaV5Names.TYPE);
			String end = atts.getValue(PopulationSchemaV5Names.ENDTIME);
			String start = atts.getValue(PopulationSchemaV5Names.STARTTIME);
			String dur = atts.getValue(PopulationSchemaV5Names.DURATION);
			if (end != null) {
				this.currentEndTime = parseTime(end);
			}
			else {
				this.currentEndTime = Time.UNDEFINED_TIME;
			}
			if (start != null) {
				this.currentStartTime = parseTime(start);
			}
			else {
				this.currentStartTime = Time.UNDEFINED_TIME;
			}
			if (dur != null) {
				this.currentDuration = parseTime(dur);
			}
			else {
				this.currentDuration = Time.UNDEFINED_TIME;
			}
		}
		else if (PopulationSchemaV5Names.LOCATION.equalsIgnoreCase(name)) {
			this.currentlocation = new BasicLocationImpl();
		}
		else if (PopulationSchemaV5Names.LEG.equalsIgnoreCase(name)){
			this.currentLeg = populationBuilder.createLeg(this.currentPlan, getLegMode(atts.getValue(PopulationSchemaV5Names.MODE)));
			String tts = atts.getValue(PopulationSchemaV5Names.DEPARTURETIME);
			if (tts != null) {
				this.currentLeg.setDepartureTime(this.parseTime(tts));
			}
			tts = atts.getValue(PopulationSchemaV5Names.TRAVELTIME);
			if (tts != null) {
				this.currentLeg.setTravelTime(this.parseTime(tts));
			}
			tts = atts.getValue(PopulationSchemaV5Names.ARRIVALTIME);
			if (tts != null) {
				this.currentLeg.setArrivalTime(this.parseTime(tts));
			}
		}
		else if (PopulationSchemaV5Names.ROUTE.equalsIgnoreCase(name)){
			this.currentRouteLinkIds = new LinkedList<Id>();
			String dist = atts.getValue(PopulationSchemaV5Names.DISTANCE);
			if (dist != null) {
				this.currentDistance = Double.valueOf(dist);
			}
			String tt = atts.getValue(PopulationSchemaV5Names.TRAVELTIME);
			if (tt != null) {
				this.currentTravelTime = this.parseTime(tt);
			}
		}
		else if (PopulationSchemaV5Names.LINK.equalsIgnoreCase(name)) {
			Id id = new IdImpl(atts.getValue(PopulationSchemaV5Names.REFID));
			this.currentRouteLinkIds.add(id);
		}
		else if (PopulationSchemaV5Names.STARTLINK.equalsIgnoreCase(name)) {
			this.currentStartLinkId = new IdImpl(atts.getValue(PopulationSchemaV5Names.REFID));
		}
		else if (PopulationSchemaV5Names.ENDLINK.equalsIgnoreCase(name)) {
			this.currentEndLinkId = new IdImpl(atts.getValue(PopulationSchemaV5Names.REFID));
		}
		else if (PopulationSchemaV5Names.FACILITYID.equalsIgnoreCase(name)) {
			Id id = new IdImpl(atts.getValue(PopulationSchemaV5Names.REFID));
			((BasicLocationImpl)this.currentlocation).setLocationId(id, LocationType.FACILITY);
		}
		else if (PopulationSchemaV5Names.LINKID.equalsIgnoreCase(name)) {
			Id id = new IdImpl(atts.getValue(PopulationSchemaV5Names.REFID));
			((BasicLocationImpl)this.currentlocation).setLocationId(id, LocationType.LINK);
		}
		else if (PopulationSchemaV5Names.FISCALHOUSEHOLDID.equalsIgnoreCase(name)){
			((BasicPersonImpl)this.currentPerson).setHouseholdId(new IdImpl(atts.getValue(PopulationSchemaV5Names.REFID)));
		}
		else if (PopulationSchemaV5Names.CAPACITY.equalsIgnoreCase(name)){
			String capString = atts.getValue(PopulationSchemaV5Names.PERSONS);
			if (capString == null) {
				this.currentCapacity = null;
			}
			else {
				this.currentCapacity = Integer.parseInt(capString);
			}
		}
		else if (PopulationSchemaV5Names.OPENINGTIME.equalsIgnoreCase(name)){
			String day = atts.getValue(PopulationSchemaV5Names.DAY);
			String start = atts.getValue(PopulationSchemaV5Names.STARTTIME);
			String end = atts.getValue(PopulationSchemaV5Names.ENDTIME);
			if ((day != null) && (start != null) && (end != null)) {
				DayType dayt = parseDay(day);
				this.currentOpeningTime = new OpeningTime(dayt, this.parseTime(start), this.parseTime(end));
			}
			else {
				this.currentOpeningTime = null;
			}
		}
		else if (PopulationSchemaV5Names.ACTIVITY.equalsIgnoreCase(name)){
			this.currentActType = atts.getValue(PopulationSchemaV5Names.TYPE);
			String freq = atts.getValue(PopulationSchemaV5Names.FREQUENCY);
			if (freq != null) {
				this.currentFrequency = Integer.parseInt(freq);
			}
			else {
				this.currentFrequency = null;
			}
		}
	} //end of startTag
	
	private DayType parseDay(String day) {
		for (DayType dt : DayType.values()) {
			if (dt.toString().equalsIgnoreCase(day)){
				return dt;
			}	
		}
		throw new IllegalArgumentException("Unknown DayType: " + day);
	}

	private BasicLeg.Mode getLegMode(String mode) {
		if (BasicLeg.Mode.car.toString().equalsIgnoreCase(mode)){
			return BasicLeg.Mode.car;
		}
		else if (BasicLeg.Mode.bike.toString().equalsIgnoreCase(mode)){
			return BasicLeg.Mode.bike;
		}
		else if (BasicLeg.Mode.bus.toString().equalsIgnoreCase(mode)){
			return BasicLeg.Mode.bus;
		}
		else if (BasicLeg.Mode.miv.toString().equalsIgnoreCase(mode)){
			return BasicLeg.Mode.miv;
		}
		else if (BasicLeg.Mode.motorbike.toString().equalsIgnoreCase(mode)){
			return BasicLeg.Mode.motorbike;
		}
		else if (BasicLeg.Mode.pt.toString().equalsIgnoreCase(mode)){
			return BasicLeg.Mode.pt;
		}
		else if (BasicLeg.Mode.ride.toString().equalsIgnoreCase(mode)){
			return BasicLeg.Mode.ride;
		}
		else if (BasicLeg.Mode.train.toString().equalsIgnoreCase(mode)){
			return BasicLeg.Mode.train;
		}
		else if (BasicLeg.Mode.tram.toString().equalsIgnoreCase(mode)){
			return BasicLeg.Mode.tram;
		}
		else if (BasicLeg.Mode.walk.toString().equalsIgnoreCase(mode)){
			return BasicLeg.Mode.walk;
		}
		else {
			log.warn("Unknown leg mode: " + mode + " Setting mode to undefined!");
			return BasicLeg.Mode.undefined;
		}
		
	}
	
	private double parseTime(String time) {
		return Time.parseTime(time);
	}
	
	private Id getId(Attributes atts) {
		Id id = new IdImpl(atts.getValue(PopulationSchemaV5Names.ID));
		return id;
	}
	
	public void readFile(String filename) {
		try {
			super.parse(filename);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
