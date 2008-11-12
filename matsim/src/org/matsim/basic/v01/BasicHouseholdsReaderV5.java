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
import java.util.List;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.interfaces.basic.v01.BasicHousehold;
import org.matsim.interfaces.basic.v01.HouseholdBuilder;
import org.matsim.interfaces.basic.v01.BasicIncome.IncomePeriod;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * @author dgrether
 */
public class BasicHouseholdsReaderV5 extends MatsimXmlParser {
	
	private List<BasicHousehold> households;

	private BasicHousehold currentHousehold;

	private List<Id> currentmembers;

	private BasicLocationImpl currentlocation;

	private Double currentXCoord;

	private Double currentYCoord;

	private BasicIncomeImpl currentincome;

	private HouseholdBuilder builder;

	private Id currentHhId;

	private String currentLanguage;

	private List<Id> currentVehicleIds;


	public BasicHouseholdsReaderV5(List<BasicHousehold> households) {
		if (households == null) {
			throw new IllegalArgumentException("Container for households must not be null!");
		}
		this.households = households;
		this.builder = new BasicHouseholdBuilder(this.households);
	}
	
	protected void setHouseholdBuilder(HouseholdBuilder householdBuilder) {
		this.builder = householdBuilder;
	}
	
	
	public void readFile(String filename) {
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
	
	/**
	 * @see org.matsim.utils.io.MatsimXmlParser#endTag(java.lang.String, java.lang.String, java.util.Stack)
	 */
	@Override
	public void endTag(String name, String content, Stack<String> context) {
		if (HouseholdsSchemaV5Names.HOUSEHOLD.equalsIgnoreCase(name)) {
			this.currentHousehold = this.builder.createHousehold(this.currentHhId, this.currentmembers, this.currentVehicleIds);
			this.currentHhId = null;
//			this.households.add(this.currentHousehold);
			this.currentHousehold.setLanguage(this.currentLanguage);
			this.currentLanguage = null;
			this.currentHousehold.setIncome(this.currentincome);
			this.currentincome = null;
			this.currentHousehold.setLocation(this.currentlocation);
			this.currentlocation = null;
			this.currentVehicleIds = null;
		}
		else if (HouseholdsSchemaV5Names.INCOME.equalsIgnoreCase(name)) {
			this.currentincome.setIncome(Double.parseDouble(content.trim()));
		}
		else if (HouseholdsSchemaV5Names.COORDINATE.equalsIgnoreCase(name)) {
			this.currentlocation.setCoord(new CoordImpl(this.currentXCoord, this.currentYCoord));
			this.currentXCoord = null;
			this.currentYCoord = null;
		}
		else if (HouseholdsSchemaV5Names.XCOORD.equalsIgnoreCase(name)) {
			this.currentXCoord = Double.valueOf(content);
		}
		else if (HouseholdsSchemaV5Names.YCOORD.equalsIgnoreCase(name)) {
			this.currentYCoord = Double.valueOf(content);
		}
	}

	/**
	 * @see org.matsim.utils.io.MatsimXmlParser#startTag(java.lang.String, org.xml.sax.Attributes, java.util.Stack)
	 */
	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if (HouseholdsSchemaV5Names.HOUSEHOLD.equalsIgnoreCase(name)) {
			this.currentHhId = new IdImpl(atts.getValue(HouseholdsSchemaV5Names.ID));
		}
		else if (HouseholdsSchemaV5Names.LANGUAGE.equalsIgnoreCase(name)) {
			this.currentLanguage = atts.getValue(HouseholdsSchemaV5Names.NAME);
		}
		else if (HouseholdsSchemaV5Names.MEMBERS.equalsIgnoreCase(name)) {
			this.currentmembers = new ArrayList<Id>();
		}
		else if (HouseholdsSchemaV5Names.PERSONID.equalsIgnoreCase(name)) {
			Id id = new IdImpl(atts.getValue(HouseholdsSchemaV5Names.REFID));
			this.currentmembers.add(id);
		}
		else if (HouseholdsSchemaV5Names.LOCATION.equalsIgnoreCase(name)) {
			this.currentlocation = new BasicLocationImpl();
		}
		else if (HouseholdsSchemaV5Names.FACILITYID.equalsIgnoreCase(name)) {
			Id id = new IdImpl(atts.getValue(HouseholdsSchemaV5Names.REFID));
			this.currentlocation.setLocationId(id, true);
		}
		else if (HouseholdsSchemaV5Names.LINKID.equalsIgnoreCase(name)) {
			Id id = new IdImpl(atts.getValue(HouseholdsSchemaV5Names.REFID));
			this.currentlocation.setLocationId(id, false);
		}
		else if (HouseholdsSchemaV5Names.INCOME.equalsIgnoreCase(name)) {
			IncomePeriod p = getIncomePeriod(atts.getValue(HouseholdsSchemaV5Names.PERIOD));
			this.currentincome = new BasicIncomeImpl(p);
			this.currentincome.setCurrency(atts.getValue(HouseholdsSchemaV5Names.CURRENCY));
		}
		else if (HouseholdsSchemaV5Names.VEHICLEDEFINITIONID.equalsIgnoreCase(name)) {
			Id id = new IdImpl(atts.getValue(HouseholdsSchemaV5Names.REFID));
			if (this.currentVehicleIds == null) 
				this.currentVehicleIds = new ArrayList<Id>();
			this.currentVehicleIds.add(id);
		}
	}


	private IncomePeriod getIncomePeriod(String s) {
		if (IncomePeriod.day.toString().equalsIgnoreCase(s)) {
			return IncomePeriod.day;
		}
		else if (IncomePeriod.month.toString().equalsIgnoreCase(s)) {
			return IncomePeriod.month;
		}
		else if (IncomePeriod.hour.toString().equalsIgnoreCase(s)) {
			return IncomePeriod.hour;
		}
		else if (IncomePeriod.second.toString().equalsIgnoreCase(s)) {
			return IncomePeriod.second;
		}
		else if (IncomePeriod.year.toString().equalsIgnoreCase(s)) {
			return IncomePeriod.year;
		}
		throw new IllegalArgumentException("Not known income period!");
	}

}
