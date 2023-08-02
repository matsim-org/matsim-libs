/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractHouseholdsReaderV10
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

import java.util.*;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.misc.Counter;
import org.matsim.households.Income.IncomePeriod;
import org.matsim.utils.objectattributes.AttributeConverter;
import org.matsim.utils.objectattributes.attributable.AttributesXmlReaderDelegate;
import org.matsim.vehicles.Vehicle;
import org.xml.sax.Attributes;

/**
 * @author dgrether
 *
 */
abstract class AbstractHouseholdsReaderV10 extends MatsimXmlParser{

	private List<Id<Person>> currentmembers = null;

	private Income currentincome = null;

	private HouseholdsFactory builder = null;

	private Id<Household> currentHhId = null;

	private List<Id<Vehicle>> currentVehicleIds = null;

	private IncomePeriod currentIncomePeriod;

	private String currentincomeCurrency;

	private final Households households;

	private Counter counter = new Counter("  households # ");

	private Household currentHousehold = null;

	private final AttributesXmlReaderDelegate attributesReader = new AttributesXmlReaderDelegate();
	private org.matsim.utils.objectattributes.attributable.Attributes currAttributes =
			new org.matsim.utils.objectattributes.attributable.AttributesImpl();

	public AbstractHouseholdsReaderV10(Households households) {
		super(ValidationType.XSD_ONLY);
		if (households == null) {
			throw new IllegalArgumentException("Container for households must not be null!");
		}
		this.households = households;
		this.builder = households.getFactory();
	}

	public void putAttributeConverter( final Class<?> clazz , AttributeConverter<?> converter ) {
		attributesReader.putAttributeConverter( clazz , converter );
	}

	@Inject
	public void putAttributeConverters( final Map<Class<?>, AttributeConverter<?>> converters ) {
		attributesReader.putAttributeConverters( converters );
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
		if (HouseholdsSchemaV10Names.HOUSEHOLD.equalsIgnoreCase(name)) {
			fillHousehold();
			((HouseholdsImpl)this.households).addHousehold(currentHousehold);
			this.currentHousehold = null;
			counter.incCounter();
		}
		else if (HouseholdsSchemaV10Names.INCOME.equalsIgnoreCase(name)) {
			this.currentincome = this.builder.createIncome(Double.parseDouble(content.trim()), this.currentIncomePeriod);
			this.currentincome.setCurrency(this.currentincomeCurrency);
		}
		else if (HouseholdsSchemaV10Names.HOUSEHOLDS.equalsIgnoreCase(name)) {
			counter.printCounter();
		}
		else if (name.equalsIgnoreCase(HouseholdsSchemaV10Names.ATTRIBUTES)) {
			this.currAttributes = null;
		}
		else if (name.equalsIgnoreCase(HouseholdsSchemaV10Names.ATTRIBUTE)) {
			this.attributesReader.endTag( name , content , context );
		}
	}

	private void fillHousehold() {
		((HouseholdImpl) this.currentHousehold).setMemberIds(this.currentmembers);
		((HouseholdImpl) this.currentHousehold).setVehicleIds(this.currentVehicleIds);
		this.currentHousehold.setIncome(this.currentincome);
		this.currentHhId = null;
		this.currentVehicleIds = null;
		this.currentincome = null;
		this.currentmembers = null;
		this.currentIncomePeriod = null;
		this.currentincomeCurrency = null;
	}

	/*package*/ Household createHousehold() {
		Household hh = this.builder.createHousehold(this.currentHhId);
		((HouseholdImpl) hh).setMemberIds(this.currentmembers);
		((HouseholdImpl) hh).setVehicleIds(this.currentVehicleIds);
		hh.setIncome(this.currentincome);
		this.currentHhId = null;
		this.currentVehicleIds = null;
		this.currentincome = null;
		this.currentmembers = null;
		this.currentIncomePeriod = null;
		this.currentincomeCurrency = null;
		return hh;
	}

	/**
	 * @see org.matsim.core.utils.io.MatsimXmlParser#startTag(java.lang.String, org.xml.sax.Attributes, java.util.Stack)
	 */
	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if (HouseholdsSchemaV10Names.HOUSEHOLD.equalsIgnoreCase(name)) {
			this.currentHhId = Id.create(atts.getValue(HouseholdsSchemaV10Names.ID), Household.class);
			this.currentHousehold = this.builder.createHousehold(this.currentHhId);
			this.currentmembers = new ArrayList<>();
			this.currentVehicleIds = new ArrayList<>();
		}
		else if (HouseholdsSchemaV10Names.MEMBERS.equalsIgnoreCase(name)) {
//			this.currentmembers = new ArrayList<Id>();
		}
		else if (HouseholdsSchemaV10Names.PERSONID.equalsIgnoreCase(name)) {
			Id<Person> personId = Id.create(atts.getValue(HouseholdsSchemaV10Names.REFID), Person.class);
			this.currentmembers.add(personId);
		}
		else if (HouseholdsSchemaV10Names.INCOME.equalsIgnoreCase(name)) {
			this.currentIncomePeriod = getIncomePeriod(atts.getValue(HouseholdsSchemaV10Names.PERIOD));
			this.currentincomeCurrency = atts.getValue(HouseholdsSchemaV10Names.CURRENCY);
		}
		else if (HouseholdsSchemaV10Names.VEHICLES.equalsIgnoreCase(name)){
//			this.currentVehicleIds = new ArrayList<Id>();
		}
		else if (HouseholdsSchemaV10Names.VEHICLEDEFINITIONID.equalsIgnoreCase(name)) {
			Id<Vehicle> vehicleId = Id.create(atts.getValue(HouseholdsSchemaV10Names.REFID), Vehicle.class);
			this.currentVehicleIds.add(vehicleId);
		}
		else if (name.equalsIgnoreCase(HouseholdsSchemaV10Names.ATTRIBUTES)) {
			if (context.peek().equalsIgnoreCase(HouseholdsSchemaV10Names.HOUSEHOLD)) {
				currAttributes = this.currentHousehold.getAttributes();
				attributesReader.startTag( name , atts , context, currAttributes );
			}
		}
		else if (name.equalsIgnoreCase(HouseholdsSchemaV10Names.ATTRIBUTE)) {
			attributesReader.startTag( name , atts , context, currAttributes );
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

	/*package*/ Households getHouseholds(){
		return this.households;
	}
}
