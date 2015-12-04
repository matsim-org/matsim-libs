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
package playground.ivt.maxess.prepareforbiogeme.tripbased;

import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.utils.objectattributes.ObjectAttributes;
import playground.ivt.maxess.prepareforbiogeme.framework.ChoiceDataSetWriter;
import playground.ivt.maxess.prepareforbiogeme.framework.ChoiceSet;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

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
		values.put("P_ID", getId( cs ) );
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

		values.put("C_CHOICE", getChoice(cs));

		for ( Map.Entry<String,Trip> alt : cs.getNamedAlternatives().entrySet() ) {
			values.put("A_" + alt.getKey() + "_TT", getTravelTime( alt.getValue() ));
		}

		return values;
	}

	private void put( final String name , final Number value, Map<String,Number> map ) {
		codebook.openPage( name );
		// writing meaning is done in calculation method
		// awful, just hacked in quickly. Should be refactored before being used in other converters
		codebook.writeCoding( value );
		map.put( name, value );
		codebook.closePage();
	}

	private final TLongObjectMap<Id<Person>> idMappings = new TLongObjectHashMap<>();
	private long getId( final ChoiceSet<Trip> cs ) {
		final Id<Person> id = cs.getDecisionMaker().getId();
		final Long longId = Long.getLong( id.toString() );

		if ( longId != null ) return longId;

		// TODO store table number to id
		// (could also be done offline, as String hashCode should be stable according to documentation)
		long value = id.toString().hashCode();

		while ( idMappings.containsKey( value ) && !idMappings.get( value ).equals( id ) ) {
			log.warn( "Already a numerical ID "+value+" (for id "+idMappings.get( value )+") when adding "+id );
			value++;
		}

		idMappings.put( value , id );
		return value;
	}

	private int getChoice(final ChoiceSet<Trip> cs) {
		// Assuming the chosen destination is the first listed one, the choice index will always be between
		// 0 and n_modes. So linear search sould actually not be that bad for large number of alternatives,
		// and actually scale BETTER than "smart" methods such as binary search...
		int index = 0;
		for ( String alt : cs.getNamedAlternatives().keySet() ) {
			if ( alt.equals( cs.getChosenName() ) ) return index;
			index++;
		}
		throw new RuntimeException( cs.getChosenName()+" not found in "+cs.getNamedAlternatives() );
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

	private double getTravelTime(final Trip alternative) {
		double tt = 0;

		for ( Leg l : alternative.getLegsOnly() ) {
			if ( l.getTravelTime() == Time.UNDEFINED_TIME ) {
				log.warn("undefined travel time for " + alternative);
				return -99;
			}
			tt += l.getTravelTime();
		}

		return tt;
	}

	public static class Codebook {
		private Map<String,Codepage> pages = new LinkedHashMap<>();

		private Codepage currentPage = null;
		private String meaning = null;
		private Number coding = null;

		private void openPage( final String name ) {
			currentPage = pages.get( name );

			if ( currentPage == null ) {
				currentPage = new Codepage( name );
				pages.put( name , currentPage );
			}
		}

		private void writeMeaning( final String meaning ) {
			this.meaning = meaning;
		}

		private void writeCoding( final Number coding ) {
			this.coding = coding;
		}

		private void closePage() {
			currentPage.add( meaning , coding );
			currentPage = null;
			meaning = null;
			coding = null;
		}

		public Map<String, Codepage> getPages() {
			return Collections.unmodifiableMap( pages );
		}
	}

	public static class Codepage {
		private final String variableName;

		private final Map<Number, String> codingToMeaning = new TreeMap<>(  );
		private final Map<Number, Integer> codingToCount = new TreeMap<>(  );

		private Codepage( String variableName ) {
			this.variableName = variableName;
		}

		private void add( final String meaning , final Number coding ) {
			codingToMeaning.put( coding , meaning );
			MapUtils.addToInteger( coding , codingToCount , 0 , 1 );
		}

		public String getVariableName() {
			return variableName;
		}

		public Map<Number, String> getCodingToMeaning() {
			return Collections.unmodifiableMap( codingToMeaning );
		}

		public Map<Number, Integer> getCodingToCount() {
			return Collections.unmodifiableMap( codingToCount );
		}

	}
}
