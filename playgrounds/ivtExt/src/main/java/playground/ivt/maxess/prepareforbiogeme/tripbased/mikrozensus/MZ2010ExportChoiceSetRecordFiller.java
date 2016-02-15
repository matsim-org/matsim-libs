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
package playground.ivt.maxess.prepareforbiogeme.tripbased.mikrozensus;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.utils.objectattributes.ObjectAttributes;
import playground.ivt.maxess.prepareforbiogeme.framework.ChoiceDataSetWriter;
import playground.ivt.maxess.prepareforbiogeme.framework.ChoiceSet;
import playground.ivt.maxess.prepareforbiogeme.tripbased.RecordFillerUtils;
import playground.ivt.maxess.prepareforbiogeme.tripbased.Trip;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author thibautd
 */
public class MZ2010ExportChoiceSetRecordFiller  implements ChoiceDataSetWriter.ChoiceSetRecordFiller<Trip> {
	private static final Logger log = Logger.getLogger( MZ2010ExportChoiceSetRecordFiller.class );

	private final ObjectAttributes personAttributes;
	private final Codebook codebook = new Codebook();

	public MZ2010ExportChoiceSetRecordFiller( ObjectAttributes personAttributes ) {
		this.personAttributes = personAttributes;
	}

	public Codebook getCodebook() {
		return codebook;
	}

	@Override
	public Map<String,Number> getFieldValues(final ChoiceSet<Trip> cs) {
		final Map<String,Number> values = new LinkedHashMap<>();

		// This is awful, but BIOGEME does not understand anything else than numbers...
		values.put("P_ID", RecordFillerUtils.getId( cs ) );
		put( "P_AGE", getAge( cs.getDecisionMaker() ), values );
		put( "P_GENDER", getGender( cs.getDecisionMaker() ), values );

		put( "P_CARAVAIL", getCarAvailability( cs.getDecisionMaker() ), values );
		put( "P_BIKEAVAIL", getBikeAvailability( cs.getDecisionMaker() ), values );
		// motorcycle would also be possible

		put( "P_GA_FIRST", getGAFirst( cs.getDecisionMaker() ), values );
		put( "P_GA_SECOND", getGASecond( cs.getDecisionMaker() ), values );
		put( "P_HALBTAX", getHalbtax( cs.getDecisionMaker() ), values );
		put( "P_STRECKENABO", getStreckenAbo( cs.getDecisionMaker() ), values );
		put( "P_LOCALABO", getLocalAbo( cs.getDecisionMaker() ), values );

		put( "P_DAYOFWEEK", getDayOfWeek( cs.getDecisionMaker() ), values );
		put( "P_LICENSE", getLicense( cs.getDecisionMaker() ), values );
		put( "P_EMPLOYMENT", getEmployment( cs.getDecisionMaker() ), values );

		put( "HH_SIZE" , getHouseholdSize( cs.getDecisionMaker() ) , values );
		put( "HH_MONTHINCOME" , getHouseholdIncome( cs.getDecisionMaker() ) , values );

		values.put("C_CHOICE", RecordFillerUtils.getChoice(cs));

		for ( Map.Entry<String,Trip> alt : cs.getNamedAlternatives().entrySet() ) {
			final String name = alt.getKey();
			final Trip trip = alt.getValue();
			final double distance_m = RecordFillerUtils.getDistance( trip );
			values.put("A_" + name + "_TT", RecordFillerUtils.getTravelTime( trip ) );
			values.put("A_" + name + "_TD_M", distance_m );

			if ( name.endsWith( TransportMode.pt ) ) {
				values.put("A_" + name + "_PRICE_FIRSTCLASS", SBBPricesUtils.computeSBBTripPrice( SBBPricesUtils.SBBClass.first , false , distance_m ));
				values.put("A_" + name + "_PRICE_FIRSTCLASS_HT", SBBPricesUtils.computeSBBTripPrice( SBBPricesUtils.SBBClass.first , true , distance_m ));

				values.put("A_" + name + "_PRICE_SECONDCLASS", SBBPricesUtils.computeSBBTripPrice( SBBPricesUtils.SBBClass.second , false , distance_m ));
				values.put("A_" + name + "_PRICE_SECONDCLASS_HT", SBBPricesUtils.computeSBBTripPrice( SBBPricesUtils.SBBClass.second , true , distance_m ));
			}

			if ( name.endsWith( TransportMode.car ) ) {
				values.put("A_" + name + "_PRICE", calcPriceAuto( distance_m ));
			}
		}

		return values;
	}

	private Number getHouseholdIncome( Person decisionMaker ) {
		final String income = ( String ) personAttributes.getAttribute(
				decisionMaker.getId().toString(),
				"householdIncome" );
		codebook.writeMeaning( income );
		switch ( income ) {
			case "no Answer":
				return -98;
			case "do not know":
				return -97;
			case "less than CHF 2000":
				return 1000;
			case "CHF 2000 to 4000":
				return 3000;
			case "CHF 4001 to 6000":
				return 5000;
			case "CHF 6001 to 8000":
				return 7000;
			case "CHF 8001 to 10000":
				return 8000;
			case "CHF 10001 to 12000":
				return 11000;
			case "CHF 12001 to 14000":
				return 13000;
			case "CHF 14001 to 16000":
				return 15000;
			case "greater than CHF 16000":
				return 17000;
		}
		throw new IllegalArgumentException( income );
	}

	private Number getHouseholdSize( Person decisionMaker ) {
		final Integer size = ( Integer ) personAttributes.getAttribute(
				decisionMaker.getId().toString(),
				"householdSize" );
		codebook.writeMeaning( ""+size );
		return size;
	}

	private double calcPriceAuto( final double distance_m ) {
		// value of 0.13 CHF per km gotten from S. Schmutz master thesis
		return 0.13 * distance_m / 1000;
	}

	private void put( final String name , final Number value, Map<String,Number> map ) {
		codebook.openPage( name );
		// writing meaning is done in calculation method
		// awful, just hacked in quickly. Should be refactored before being used in other converters
		codebook.writeCoding( value );
		map.put( name, value );
		codebook.closePage();
	}

	private int getAge(final Person decisionMaker) {
		final String age = ( String ) personAttributes.getAttribute(
				decisionMaker.getId().toString(),
				"age" );
		codebook.writeMeaning( age );
		return Integer.valueOf( age );
	}

	private short getGender(final Person decisionMaker) {
		final String sex = (String)
				personAttributes.getAttribute(
					decisionMaker.getId().toString(),
					"gender" );
		codebook.writeMeaning( sex );

		switch ( sex ) {
			case "f":
				return 0;
			case "m":
				return 1;
			default:
				throw new IllegalArgumentException( "unhandled sex "+sex );
		}
	}

	private short getCarAvailability(final Person decisionMaker) {
		final String avail = (String)
				personAttributes.getAttribute(
					decisionMaker.getId().toString(),
					"availability: car" );
		codebook.writeMeaning( avail );

		switch ( avail ) {
			// yes
			case "always":
				return 1;
			// no
			case "never":
				return 0;
			case "by arrengement":
				return 2;
			// missings
			case "unspecified":
				return 991;
			case "no answer":
				return 992;
			case "???":
				return 993;
			default:
				throw new IllegalArgumentException( "unhandled car avail "+avail );
		}
	}

	private short getBikeAvailability(final Person decisionMaker) {
		final String avail = (String)
				personAttributes.getAttribute(
					decisionMaker.getId().toString(),
					"availability: bicycle" );
		codebook.writeMeaning( avail );

		switch ( avail ) {
			// yes
			case "always":
				return 1;
			// no
			case "never":
				return 0;
			case "by arrengement":
				return 2;
			// missings
			case "unspecified":
				return 991;
			case "no answer":
				return 992;
			case "???":
				return 993;
			default:
				throw new IllegalArgumentException( "unhandled bike avail "+avail );
		}
	}

	private short getGAFirst( Person decisionMaker ) {
		final String avail = (String)
				personAttributes.getAttribute(
						decisionMaker.getId().toString(),
						"abonnement: GA first class" );
		codebook.writeMeaning( avail );
		switch ( avail ) {
			case "no":
				return 0;
			case "yes":
				return 1;

			case "not known":
				return 991;
			case "no answer":
				return 992;

			default:
				throw new IllegalArgumentException( "unhandled GA first class avail "+avail );
		}
	}

	private short getGASecond( Person decisionMaker ) {
		final String avail = (String)
				personAttributes.getAttribute(
						decisionMaker.getId().toString(),
						"abonnement: GA second class" );
		codebook.writeMeaning( avail );
		switch ( avail ) {
			case "no":
				return 0;
			case "yes":
				return 1;

			case "not known":
				return 991;
			case "no answer":
				return 992;

			default:
				throw new IllegalArgumentException( "unhandled GA second class avail "+avail );
		}
	}

	private short getHalbtax( Person decisionMaker ) {
		final String avail = (String)
				personAttributes.getAttribute(
						decisionMaker.getId().toString(),
						"abonnement: Halbtax" );
		codebook.writeMeaning( avail );
		switch ( avail ) {
			case "no":
				return 0;
			case "yes":
				return 1;

			case "not known":
				return 991;
			case "no answer":
				return 992;

			default:
				throw new IllegalArgumentException( "unhandled halbtax avail "+avail );
		}
	}

	private short getStreckenAbo( Person decisionMaker ) {
		final String avail = (String)
				personAttributes.getAttribute(
						decisionMaker.getId().toString(),
						"abonnement: Stecken" );
		codebook.writeMeaning( avail );
		switch ( avail ) {
			case "no":
				return 0;
			case "yes":
				return 1;

			case "not known":
				return 991;
			case "no answer":
				return 992;

			default:
				throw new IllegalArgumentException( "unhandled strecken avail "+avail );
		}
	}

	private short getLocalAbo( Person decisionMaker ) {
		final String avail = (String)
				personAttributes.getAttribute(
						decisionMaker.getId().toString(),
						"abonnement: Verbund" );
		codebook.writeMeaning( avail );
		switch ( avail ) {
			case "no":
				return 0;
			case "yes":
				return 1;

			case "not known":
				return 991;
			case "no answer":
				return 992;

			default:
				throw new IllegalArgumentException( "unhandled verbund avail "+avail );
		}
	}

	private short getLicense( Person decisionMaker ) {
		final String avail = (String)
				personAttributes.getAttribute(
						decisionMaker.getId().toString(),
						"driving licence" );
		codebook.writeMeaning( avail );
		switch ( avail ) {
			case "no":
				return 0;
			case "yes":
				return 1;

			case "not known":
				return 991;
			case "no answer":
				return 992;

			default:
				throw new IllegalArgumentException( "unhandled license avail "+avail );
		}
	}

	private short getEmployment( Person decisionMaker ) {
		final String avail = (String)
				personAttributes.getAttribute(
						decisionMaker.getId().toString(),
						"work: employment status" );
		codebook.writeMeaning( avail );

		switch ( avail ) {
			// "yes"
			case "employee":
				return 1;
			case "apprentice-trainee":
				return 2;
			case "independent":
				return 3;
			case "Mitarbeitendes Familienmitglied":
				return 4;

			// "no"
			case "housewife/househusband":
				return 5;
			case "retired":
				return 6;
			case "unemployed":
				return 7;
			case "education or training":
				return 8;
			case "other inactive":
				return 9;
			case "disabled":
				return 10;

			// "missing"
			case "unspecified":
				return 999;

			default:
				throw new IllegalArgumentException( "unhandled employement"+avail );
		}
	}

	private short getDayOfWeek( Person decisionMaker ) {
		final String dow = (String)
				personAttributes.getAttribute(
						decisionMaker.getId().toString(),
						"day of week" );
		codebook.writeMeaning( dow );

		switch ( dow ) {
			case "monday":    return 1;
			case "tuesday":   return 2;
			case "wednesday": return 3;
			case "thursday":  return 4;
			case "friday":    return 5;
			case "saturday":  return 6;
			case "sunday":    return 7;

			default:
				throw new IllegalArgumentException( "unhandled dow "+dow );
		}
	}

}
