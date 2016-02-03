/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.ivt.maxess.prepareforbiogeme.tripbased.capetown;

import org.matsim.api.core.v01.population.Person;
import org.matsim.utils.objectattributes.ObjectAttributes;
import playground.ivt.maxess.prepareforbiogeme.framework.ChoiceDataSetWriter;
import playground.ivt.maxess.prepareforbiogeme.framework.ChoiceSet;
import playground.ivt.maxess.prepareforbiogeme.tripbased.RecordFillerUtils;
import playground.ivt.maxess.prepareforbiogeme.tripbased.Trip;
import playground.ivt.maxess.prepareforbiogeme.tripbased.mikrozensus.Codebook;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author thibautd
 */
public class CapeTownChoiceSetRecordFiller implements ChoiceDataSetWriter.ChoiceSetRecordFiller<Trip>  {
	private final Codebook codebook = new Codebook();

	private final ObjectAttributes personAttributes;

	public CapeTownChoiceSetRecordFiller( ObjectAttributes personAttributes ) {
		this.personAttributes = personAttributes;
	}

	@Override
	public Map<String, ? extends Number> getFieldValues( ChoiceSet<Trip> cs ) {
		final Map<String,Number> values = new LinkedHashMap<>();

		values.put("P_ID", RecordFillerUtils.getId( cs ) );
		put( "P_EDUCATION" , getEducation( cs.getDecisionMaker() ) , values );
		put( "P_EMPLOYEMENT" , getEmployment( cs.getDecisionMaker() ) , values );
		put( "P_LICENSE_CAR" , getLicense( cs.getDecisionMaker() ) , values );
		put( "P_LICENSE_MOTO" , getLicenseMoto( cs.getDecisionMaker() ) , values );
		put( "P_AGE" , getAge( cs.getDecisionMaker() ) , values );
		put( "P_GENDER" , getGender( cs.getDecisionMaker() ) , values );

		values.put("C_CHOICE", RecordFillerUtils.getChoice(cs));

		for ( Map.Entry<String,Trip> alt : cs.getNamedAlternatives().entrySet() ) {
			final String name = alt.getKey();
			final Trip trip = alt.getValue();
			final double distance_m = RecordFillerUtils.getDistance( trip );
			values.put( "A_" + name + "_TT", RecordFillerUtils.getTravelTime( trip ) );
			values.put( "A_" + name + "_TD_M", distance_m );
		}

		return values;
	}

	private Number getAge( Person decisionMaker ) {
		final String birth = (String)
				personAttributes.getAttribute(
						decisionMaker.getId().toString(),
						"yearOfBirth" );
		final double age = 2012 - Integer.valueOf( birth );
		codebook.writeMeaning( ""+age );
		return age;
	}

	private Number getLicenseMoto( Person decisionMaker ) {
		final String license = (String)
				personAttributes.getAttribute(
						decisionMaker.getId().toString(),
						"license_motorcycle" );
		codebook.writeMeaning( license );
		return PersonEnums.LicenseMotorcycle.parseFromDescription( license ).getCode();
	}

	private Number getLicense( Person decisionMaker ) {
		final String license = (String)
				personAttributes.getAttribute(
						decisionMaker.getId().toString(),
						"license_car" );
		codebook.writeMeaning( license );
		return PersonEnums.LicenseCar.parseFromDescription( license ).getCode();
	}

	private Number getEmployment( Person decisionMaker ) {
		final String employment = (String)
				personAttributes.getAttribute(
						decisionMaker.getId().toString(),
						"employment" );
		codebook.writeMeaning( employment );
		return PersonEnums.Employment.parseFromDescription( employment ).getCode();
	}

	private Number getEducation( Person decisionMaker ) {
		final String education = (String)
				personAttributes.getAttribute(
						decisionMaker.getId().toString(),
						"education" );
		codebook.writeMeaning( education );
		return PersonEnums.Education.parseFromDescription( education ).getCode();
	}

	private Number getGender( Person decisionMaker ) {
		final String gender = (String)
				personAttributes.getAttribute(
						decisionMaker.getId().toString(),
						"gender" );
		codebook.writeMeaning( gender );
		return PersonEnums.Education.parseFromDescription( gender ).getCode();
	}

	private void put( final String name , final Number value, Map<String,Number> map ) {
		codebook.openPage( name );
		// writing meaning is done in calculation method
		// awful, just hacked in quickly. Should be refactored before being used in other converters
		codebook.writeCoding( value );
		map.put( name, value );
		codebook.closePage();
	}
}
