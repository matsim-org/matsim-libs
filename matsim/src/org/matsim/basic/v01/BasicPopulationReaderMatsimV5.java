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
package org.matsim.basic.v01;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.interfaces.basic.v01.BasicHousehold;
import org.matsim.interfaces.basic.v01.HouseholdBuilder;
import org.matsim.interfaces.basic.v01.PopulationBuilder;
import org.matsim.population.PopulationReader;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.utils.io.MatsimXmlParser;
import org.matsim.utils.misc.Time;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;


/**
 * @author dgrether
 *
 */
public class BasicPopulationReaderMatsimV5 extends MatsimXmlParser implements PopulationReader{

	
	private static final Logger log = Logger
			.getLogger(BasicPopulationReaderMatsimV5.class);
	
	private PopulationBuilder populationBuilder;

	private BasicPerson currentPerson;

	private BasicPlan currentPlan;

	private String currentActType;

//	private double currentDuration;

	private double currentStartTime;

	private double currentEndTime;

	private BasicLocationImpl currentlocation;

	private Double currentXCoord;

	private Double currentYCoord;

	private BasicAct currentAct;

	private BasicLeg currentLeg;
	
	private List<Id> currentRouteLinkIds = new ArrayList<Id>();

	private BasicRoute currentRoute;

	private Double currentDistance;

	private Double currentTravelTime;

	private HouseholdBuilder householdBuilder;
	
	private BasicHouseholdsReaderV5 householdsDelegate = null;

	private List<BasicHousehold> households;
	
	protected BasicPopulationReaderMatsimV5() {
	}
		
  public BasicPopulationReaderMatsimV5(BasicPopulation pop, List<BasicHousehold> households) {
  	this.populationBuilder = new BasicPopulationBuilder(pop);
  	this.householdBuilder = new BasicHouseholdBuilder(households);
  	this.households = households;
  }
	
  protected void setPopulationBuilder(PopulationBuilder populationBuilder) {
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
			this.currentlocation.setCoord(new CoordImpl(this.currentXCoord, this.currentYCoord));
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
//		  this.currentAct.setDuration(currentDuration);
		  this.currentActType = null;
		  this.currentlocation = null;
		}
		else if (PopulationSchemaV5Names.ROUTE.equalsIgnoreCase(name)) {
			this.currentRoute = this.populationBuilder.createRoute(this.currentRouteLinkIds);
			if (null != this.currentDistance) {
				this.currentRoute.setDist(this.currentDistance);
			}
			if (null != this.currentTravelTime) {
				this.currentRoute.setTravTime(this.currentTravelTime);
			}
			this.currentRouteLinkIds.clear();
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
		else {
			log.warn("Ignoring endTag (beta implementation!): " + name);
		}
	} //end of endTag

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if (HouseholdsSchemaV5Names.HOUSEHOLD.equalsIgnoreCase(name)) {
			this.householdsDelegate = new BasicHouseholdsReaderV5(this.households);
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
		}
		else if (PopulationSchemaV5Names.ACT.equalsIgnoreCase(name)) {
			this.currentActType = atts.getValue(PopulationSchemaV5Names.TYPE);
			String end = atts.getValue(PopulationSchemaV5Names.ENDTIME);
			String start = atts.getValue(PopulationSchemaV5Names.STARTTIME);
//			String dur = atts.getValue(PopulationSchemaV5Names.DURATION);
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
//			if (dur != null) {
//				this.currentDuration = parseTime(dur);
//			}
//			else {
//				this.currentDuration = Time.UNDEFINED_TIME;
//			}
		}
		else if (PopulationSchemaV5Names.LOCATION.equalsIgnoreCase(name)) {
			this.currentlocation = new BasicLocationImpl();
		}
		else if (PopulationSchemaV5Names.LEG.equalsIgnoreCase(name)){
			this.currentLeg = populationBuilder.createLeg(this.currentPlan, getLegMode(atts.getValue(PopulationSchemaV5Names.MODE)));
		}
		else if (PopulationSchemaV5Names.ROUTE.equalsIgnoreCase(name)){
			String dist = atts.getValue(PopulationSchemaV5Names.DISTANCE);
			if (dist != null) {
				this.currentDistance = Double.valueOf(dist);
			}
			String tt = atts.getValue(PopulationSchemaV5Names.TRAVELTIME);
			if (tt != null) {
				this.currentTravelTime = Double.valueOf(tt);
			}
		}
		else if (PopulationSchemaV5Names.LINK.equalsIgnoreCase(name)) {
			Id id = new IdImpl(atts.getValue(PopulationSchemaV5Names.REFID));
			this.currentRouteLinkIds.add(id);
		}
		else if (PopulationSchemaV5Names.FACILITYID.equalsIgnoreCase(name)) {
			Id id = new IdImpl(atts.getValue(PopulationSchemaV5Names.REFID));
			this.currentlocation.setLocationId(id, true);
		}
		else if (PopulationSchemaV5Names.LINKID.equalsIgnoreCase(name)) {
			Id id = new IdImpl(atts.getValue(PopulationSchemaV5Names.REFID));
			this.currentlocation.setLocationId(id, false);
		}
		else if (PopulationSchemaV5Names.FISCALHOUSEHOLDID.equalsIgnoreCase(name)){
			((BasicPersonImpl)this.currentPerson).setHouseholdId(new IdImpl(atts.getValue(PopulationSchemaV5Names.REFID)));
		}
		else {
			log.warn("Ignoring startTag (beta implementation!): " + name);
		}
	} //end of startTag
	
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
