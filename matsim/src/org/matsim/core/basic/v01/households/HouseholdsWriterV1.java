/* *********************************************************************** *
 * project: org.matsim.*
 * HouseholdsWriterV1
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
package org.matsim.core.basic.v01.households;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;


/**
 * @author dgrether
 *
 */
public class HouseholdsWriterV1 extends MatsimXmlWriter {
	
	private List<Tuple<String, String>> atts = new ArrayList<Tuple<String, String>>();
	private BasicHouseholds<BasicHousehold> households;
	
	public HouseholdsWriterV1(BasicHouseholds<BasicHousehold> households) {
		this.households = households;
	}
	
	public void writeFile(String filename) throws FileNotFoundException, IOException {
		this.openFile(filename);
		this.writeXmlHead();
		this.writeHouseholds(this.households);
		this.close();
	}
	
	private void writeHouseholds(BasicHouseholds<BasicHousehold> basicHouseholds) throws IOException {
		atts.clear();
		atts.add(this.createTuple(XMLNS, MatsimXmlWriter.MATSIM_NAMESPACE));
		atts.add(this.createTuple(XMLNS + ":xsi", DEFAULTSCHEMANAMESPACELOCATION));
		atts.add(this.createTuple("xsi:schemaLocation", MATSIM_NAMESPACE + " " + DEFAULT_DTD_LOCATION + "households_v1.0.xsd"));
		this.writeStartTag(HouseholdsSchemaV1Names.HOUSEHOLDS, atts);
		for (BasicHousehold h : basicHouseholds.getHouseholds().values()) {
			this.writeHousehold(h);
		}
		this.writeEndTag(HouseholdsSchemaV1Names.HOUSEHOLDS);
	}

	private void writeHousehold(BasicHousehold h) throws IOException {
		this.atts.clear();
		atts.add(this.createTuple(HouseholdsSchemaV1Names.ID, h.getId().toString()));
		this.writeStartTag(HouseholdsSchemaV1Names.HOUSEHOLD, atts);
		if ((h.getMemberIds() != null) && !h.getMemberIds().isEmpty()){
			this.writeMembers(h.getMemberIds());
		}
		if ((h.getVehicleIds() != null) && !h.getVehicleIds().isEmpty()) {
			this.writeStartTag(HouseholdsSchemaV1Names.VEHICLES, null);
			for (Id id : h.getVehicleIds()){
				atts.clear();
				atts.add(this.createTuple(HouseholdsSchemaV1Names.REFID, id.toString()));
				this.writeStartTag(HouseholdsSchemaV1Names.VEHICLEDEFINITIONID, atts, true);
			}
			this.writeEndTag(HouseholdsSchemaV1Names.VEHICLES);
		}
		if (h.getIncome() != null){
			this.writeIncome(h.getIncome());
		}
		this.writeEndTag(HouseholdsSchemaV1Names.HOUSEHOLD);
	}

	private void writeIncome(BasicIncome income) throws IOException {
		atts.clear();
		if (income.getCurrency() != null) {
			atts.add(this.createTuple(HouseholdsSchemaV1Names.CURRENCY,income.getCurrency()));
		}
		atts.add(this.createTuple(HouseholdsSchemaV1Names.PERIOD, income.getIncomePeriod().toString()));
		this.writeStartTag(HouseholdsSchemaV1Names.INCOME, atts);
		this.writeContent(Double.toString(income.getIncome()), true);
		this.writeEndTag(HouseholdsSchemaV1Names.INCOME);
	}

	private void writeMembers(List<Id> memberIds) throws IOException {
		this.writeStartTag(HouseholdsSchemaV1Names.MEMBERS, null);
		for (Id id : memberIds){
			atts.clear();
			atts.add(this.createTuple(HouseholdsSchemaV1Names.REFID, id.toString()));
			this.writeStartTag(HouseholdsSchemaV1Names.PERSONID, atts, true);
		}
		this.writeEndTag(HouseholdsSchemaV1Names.MEMBERS);
	}


	
	
}
