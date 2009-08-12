/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.households;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.households.Income.IncomePeriod;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * @author dgrether
 */
public class HouseholdsReaderV10 extends MatsimXmlParser {
	
	private final Households households;

	private Household currentHousehold = null;

	private List<Id> currentmembers = null;

	private Income currentincome = null;

	private HouseholdBuilder builder = null;

	private Id currentHhId = null;

	private List<Id> currentVehicleIds = null;

	private IncomePeriod currentIncomePeriod;

	private String currentincomeCurrency;


	public HouseholdsReaderV10(Households households) {
		if (households == null) {
			throw new IllegalArgumentException("Container for households must not be null!");
		}
		this.households = households;
		this.builder = households.getBuilder();
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
	 * @see org.matsim.core.utils.io.MatsimXmlParser#endTag(java.lang.String, java.lang.String, java.util.Stack)
	 */
	@Override
	public void endTag(String name, String content, Stack<String> context) {
		if (HouseholdsSchemaV10Names.HOUSEHOLD.equalsIgnoreCase(name)) {
			this.currentHousehold = createHousehold(this.currentHhId, this.currentmembers, this.currentVehicleIds);
			this.households.getHouseholds().put(this.currentHhId, this.currentHousehold);
			this.currentHousehold.setIncome(this.currentincome);
			this.currentHhId = null;
			this.currentVehicleIds = null;
			this.currentincome = null;
		}
		else if (HouseholdsSchemaV10Names.INCOME.equalsIgnoreCase(name)) {
			this.currentincome = this.builder.createIncome(Double.parseDouble(content.trim()), this.currentIncomePeriod);
			this.currentincome.setCurrency(this.currentincomeCurrency);
		}
	}

	protected Household createHousehold(Id currentHhId2, List<Id> currentmembers2, List<Id> currentVehicleIds2) {
		Household hh = this.builder.createHousehold(this.currentHhId);
		((HouseholdImpl) hh).setMemberIds(this.currentmembers);
		((HouseholdImpl) hh).setVehicleIds(this.currentVehicleIds);		
		return hh;
	}

	/**
	 * @see org.matsim.core.utils.io.MatsimXmlParser#startTag(java.lang.String, org.xml.sax.Attributes, java.util.Stack)
	 */
	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if (HouseholdsSchemaV10Names.HOUSEHOLD.equalsIgnoreCase(name)) {
			this.currentHhId = new IdImpl(atts.getValue(HouseholdsSchemaV10Names.ID));
		}
		else if (HouseholdsSchemaV10Names.MEMBERS.equalsIgnoreCase(name)) {
			this.currentmembers = new ArrayList<Id>();
		}
		else if (HouseholdsSchemaV10Names.PERSONID.equalsIgnoreCase(name)) {
			Id id = new IdImpl(atts.getValue(HouseholdsSchemaV10Names.REFID));
			this.currentmembers.add(id);
		}
		else if (HouseholdsSchemaV10Names.INCOME.equalsIgnoreCase(name)) {
			this.currentIncomePeriod = getIncomePeriod(atts.getValue(HouseholdsSchemaV10Names.PERIOD));
			this.currentincomeCurrency = atts.getValue(HouseholdsSchemaV10Names.CURRENCY);
		}
		else if (HouseholdsSchemaV10Names.VEHICLES.equalsIgnoreCase(name)){
			this.currentVehicleIds = new ArrayList<Id>();
		}
		else if (HouseholdsSchemaV10Names.VEHICLEDEFINITIONID.equalsIgnoreCase(name)) {
			Id id = new IdImpl(atts.getValue(HouseholdsSchemaV10Names.REFID));
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
		else if (IncomePeriod.week.toString().equalsIgnoreCase(s)) {
			return IncomePeriod.week;
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
