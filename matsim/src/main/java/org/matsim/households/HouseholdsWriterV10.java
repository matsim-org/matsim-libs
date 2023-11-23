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
package org.matsim.households;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.core.utils.misc.Counter;
import org.matsim.households.algorithms.HouseholdAlgorithm;
import org.matsim.utils.objectattributes.AttributeConverter;
import org.matsim.utils.objectattributes.attributable.AttributesXmlWriterDelegate;
import org.matsim.vehicles.Vehicle;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author dgrether
 *
 */
public class HouseholdsWriterV10 extends MatsimXmlWriter implements HouseholdAlgorithm{
	private static final Logger log = LogManager.getLogger( HouseholdsWriterV10.class ) ;

	private List<Tuple<String, String>> atts = new ArrayList<>();
	private Households households;
	private final Map<Class<?>,AttributeConverter<?>> attributeConverters = new HashMap<>();

	public HouseholdsWriterV10(Households households) {
		this.households = households;
	}

	public <T> void putAttributeConverter(Class<T> clazz, AttributeConverter<T> converter) {
		this.attributeConverters.put( clazz , converter );
	}

	public void putAttributeConverters( final Map<Class<?>, AttributeConverter<?>> converters ) {
		this.attributeConverters.putAll( converters );
	}

	public void writeFile(String filename) throws UncheckedIOException {
		log.info( Gbl.aboutToWrite( " households", filename ) ) ;
		this.openFileAndWritePreamble(filename);
		this.writeHouseholds(this.households);
		this.writeEndAndCloseFile();
	}

	/*package*/ void openFileAndWritePreamble(String filename){
		this.openFile(filename);
		this.writeXmlHead();
		this.writeHeader();
	}

	/*package*/ void writeEndAndCloseFile(){

		this.writeEndTag(HouseholdsSchemaV10Names.HOUSEHOLDS);
		this.close();
	}


	private void writeHeader(){
		atts.clear();
		atts.add(createTuple(XMLNS, MatsimXmlWriter.MATSIM_NAMESPACE));
		atts.add(createTuple(XMLNS + ":xsi", DEFAULTSCHEMANAMESPACELOCATION));
		atts.add(createTuple("xsi:schemaLocation", MATSIM_NAMESPACE + " " + DEFAULT_DTD_LOCATION + "households_v1.0.xsd"));
		this.writeStartTag(HouseholdsSchemaV10Names.HOUSEHOLDS, atts);
	}

	private void writeHouseholds(Households basicHouseholds) throws UncheckedIOException {
		Counter counter = new Counter("[HouseholdsWriter] wrote household # ");
		for (Household h : basicHouseholds.getHouseholds().values()) {
			this.writeHousehold(h);
			counter.incCounter();
		}
		counter.printCounter();
	}

	/*package*/ void writeHousehold(Household h) throws UncheckedIOException {
		this.atts.clear();
		atts.add(createTuple(HouseholdsSchemaV10Names.ID, h.getId().toString()));
		this.writeStartTag(HouseholdsSchemaV10Names.HOUSEHOLD, atts);
		if ((h.getMemberIds() != null) && !h.getMemberIds().isEmpty()){
			this.writeMembers(h.getMemberIds());
		}
		if ((h.getVehicleIds() != null) && !h.getVehicleIds().isEmpty()) {
			this.writeStartTag(HouseholdsSchemaV10Names.VEHICLES, null);
			for (Id<Vehicle> id : h.getVehicleIds()){
				atts.clear();
				atts.add(createTuple(HouseholdsSchemaV10Names.REFID, id.toString()));
				this.writeStartTag(HouseholdsSchemaV10Names.VEHICLEDEFINITIONID, atts, true);
			}
			this.writeEndTag(HouseholdsSchemaV10Names.VEHICLES);
		}
		if (h.getIncome() != null){
			this.writeIncome(h.getIncome());
		}

		AttributesXmlWriterDelegate attributesWriter = new AttributesXmlWriterDelegate();
		attributesWriter.putAttributeConverters(this.attributeConverters);
		try {
			this.writer.write(NL);
		} catch (IOException e) {
			e.printStackTrace();
		}
		attributesWriter.writeAttributes( "\t\t" , this.writer , h.getAttributes() );

		this.writeEndTag(HouseholdsSchemaV10Names.HOUSEHOLD);
	}

	private void writeIncome(Income income) throws UncheckedIOException {
		atts.clear();
		if (income.getCurrency() != null) {
			atts.add(createTuple(HouseholdsSchemaV10Names.CURRENCY,income.getCurrency()));
		}
		atts.add(createTuple(HouseholdsSchemaV10Names.PERIOD, income.getIncomePeriod().toString()));
		this.writeStartTag(HouseholdsSchemaV10Names.INCOME, atts);
		this.writeContent(Double.toString(income.getIncome()), true);
		this.writeEndTag(HouseholdsSchemaV10Names.INCOME);
	}

	private void writeMembers(List<Id<Person>> memberIds) throws UncheckedIOException {
		this.writeStartTag(HouseholdsSchemaV10Names.MEMBERS, null);
		for (Id<Person> id : memberIds){
			atts.clear();
			atts.add(createTuple(HouseholdsSchemaV10Names.REFID, id.toString()));
			this.writeStartTag(HouseholdsSchemaV10Names.PERSONID, atts, true);
		}
		this.writeEndTag(HouseholdsSchemaV10Names.MEMBERS);
	}

	@Override
	public void run(Household household) {
		writeHousehold(household);
	}




}
