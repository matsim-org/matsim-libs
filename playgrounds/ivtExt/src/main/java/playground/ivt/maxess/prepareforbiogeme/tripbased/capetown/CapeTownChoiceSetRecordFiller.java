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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.households.Income;
import org.matsim.utils.objectattributes.ObjectAttributes;
import playground.ivt.maxess.prepareforbiogeme.framework.ChoiceDataSetWriter;
import playground.ivt.maxess.prepareforbiogeme.framework.ChoiceSet;
import playground.ivt.maxess.prepareforbiogeme.tripbased.RecordFillerUtils;
import playground.ivt.maxess.prepareforbiogeme.tripbased.Trip;
import playground.ivt.maxess.prepareforbiogeme.tripbased.mikrozensus.Codebook;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author thibautd
 */
public class CapeTownChoiceSetRecordFiller implements ChoiceDataSetWriter.ChoiceSetRecordFiller<Trip>  {
	private final Codebook codebook = new Codebook();

	private final ObjectAttributes personAttributes;
	private final Households households;
	private final Map<Id<Person>, Id<Household>> person2household = new HashMap<>();

	public CapeTownChoiceSetRecordFiller(
			final ObjectAttributes personAttributes,
			final Households households ) {
		this.personAttributes = personAttributes;
		this.households = households;

		for ( Household hh : households.getHouseholds().values() ) {
			for ( Id<Person> member : hh.getMemberIds() ) {
				person2household.put( member , hh.getId() );
			}
		}
	}

	@Override
	public Map<String, ? extends Number> getFieldValues( ChoiceSet<Trip> cs ) {
		final Map<String,Number> values = new LinkedHashMap<>();

		values.put("P_ID", RecordFillerUtils.getId( cs ) );
		put( "P_EDUCATION" , getEducation( cs.getDecisionMaker() ) , values );
		put( "P_EMPLOYMENT" , getEmployment( cs.getDecisionMaker() ) , values );
		put( "P_LICENSE_CAR" , getLicense( cs.getDecisionMaker() ) , values );
		put( "P_LICENSE_MOTO" , getLicenseMoto( cs.getDecisionMaker() ) , values );
		put( "P_AGE" , getAge( cs.getDecisionMaker() ) , values );
		put( "P_GENDER" , getGender( cs.getDecisionMaker() ) , values );

		put( "HH_INCOME" , getIncome( cs.getDecisionMaker() ) , values );
		put( "HH_SIZE" , getHouseholdInteger( cs.getDecisionMaker(), "householdSize" ), values );
		put( "HH_OWNED_CARS" , getHouseholdInteger( cs.getDecisionMaker() , "numberOfHouseholdCarsOwned" ) , values );
		put( "HH_OWNED_MOTO" , getHouseholdInteger( cs.getDecisionMaker() , "numberOfHouseholdMotorcyclesOwned" ) , values );

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

	private Integer getHouseholdInteger( Person decisionMaker , String att ) {
		final Id<Household> hh = person2household.get( decisionMaker.getId() );
		if ( hh == null ) throw new IllegalStateException( "no household ID for person "+decisionMaker.getId() );
		return (Integer) households.getHouseholdAttributes().getAttribute( hh.toString() , att );
	}

	private Number getIncome( Person decisionMaker ) {
		final Household hh = getHousehold( decisionMaker );
		final Income income = hh.getIncome();
		// no need to test currency and period, always ZAR and month
		return income == null ? -99 : income.getIncome();
	}

	private Household getHousehold( Person decisionMaker ) {
		final Id<Household> id = person2household.get( decisionMaker.getId() );
		if ( id == null ) throw new IllegalStateException( "no household ID for person "+decisionMaker.getId() );
		return households.getHouseholds().get( id );
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
		return PersonEnums.Gender.parseFromDescription( gender ).getCode();
	}

	private void put( final String name , final Number value, Map<String,Number> map ) {
		codebook.openPage( name );
		// writing meaning is done in calculation method
		// awful, just hacked in quickly. Should be refactored before being used in other converters
		codebook.writeCoding( value );
		map.put( name, value );
		codebook.closePage();
	}

	public Codebook getCodebook() {
		return codebook;
	}
}
