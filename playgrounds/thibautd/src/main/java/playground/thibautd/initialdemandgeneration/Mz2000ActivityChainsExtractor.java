/* *********************************************************************** *
 * project: org.matsim.*
 * Mz2000ActivityChainsExtractor.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.initialdemandgeneration;

import java.io.BufferedReader;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

/**
 * @author thibautd
 */
public class Mz2000ActivityChainsExtractor {
	public Scenario run(
			final String zpFile,
			final String wgFile,
			final String etFile) {
		MzPopulation population = new MzPopulation();

		try {
			// ////// add person info ///////
			BufferedReader reader = IOUtils.getBufferedReader( zpFile );

			MzPerson.notifyStructure( reader.readLine() );
			String line = reader.readLine();

			while (line != null) {
				population.addPerson(
						new MzPerson( line ) );
				line = reader.readLine();
			}

			// ////// add trip info //////////
			reader = IOUtils.getBufferedReader( wgFile );

			MzWeg.notifyStructure( reader.readLine() );
			line = reader.readLine();

			while (line != null) {
				population.addWeg(
						new MzWeg( line ) );
				line = reader.readLine();
			}

			// ////// add etap info //////////
			reader = IOUtils.getBufferedReader( etFile );

			MzEtappe.notifyStructure( reader.readLine() );
			line = reader.readLine();

			while (line != null) {
				population.addEtappe(
						new MzEtappe( line ) );
				line = reader.readLine();
			}
		} catch (IOException e) {
			throw new UncheckedIOException( e );
		}

		return population.getScenario();
	}
}

/**
 * general remarks on the fields:
 * interview number used as a person id
 */
class MzPopulation {
	private final Map<Id, MzPerson> persons = new HashMap<Id, MzPerson>();

	public Scenario getScenario() {
		ScenarioImpl scen = (ScenarioImpl) ScenarioUtils.createScenario(
					ConfigUtils.createConfig());

		Population population = scen.getPopulation();

		for (MzPerson person : persons.values()) {
			population.addPerson( person.getPerson() );
		}

		return scen;
	}

	public void addPerson(final MzPerson person) {
		if (persons.put( person.getId() , person ) != null) {
			throw new RuntimeException( "same person created twice" );
		}
	}

	public void addWeg(final MzWeg weg) {
		MzPerson enclosingPerson = persons.get( weg.getPersonId() );

		if (enclosingPerson == null) throw new RuntimeException( "trying to add a weg before the person" );

		enclosingPerson.addWeg( weg );
	}

	public void addEtappe(final MzEtappe etappe) {
		MzPerson enclosingPerson = persons.get( etappe.getPersonId() );

		if (enclosingPerson == null) throw new RuntimeException( "trying to add a etappe before the person" );

		enclosingPerson.addEtappe( etappe );
	}
}

class MzPerson implements Identifiable {
	private static final Logger log =
		Logger.getLogger(MzPerson.class);

	private static final String HOME = "h";
	private static final String EDUC = "e";
	private static final String SHOP = "s";
	private static final String WORK = "w";
	private static final String LEISURE = "l";

	// /////////////////////////////////////////////////////////////////////////
	// static fields
	private static boolean structureIsKnown = false;

	private static final String EMPLOYED_NAME = "F50003";
	private static int employedIndex = -1;

	// id: interview number
	private static final String ID_NAME = "INTNR";
	private static int idIndex = -1;

	private static final String DOW_NAME = "DAYSTTAG";
	private static int dayOfWeekIndex = -1;

	private static final String LICENCE_NAME = "F50005";
	private static int licenceIndex = -1;

	private static final String AGE_NAME = "F50001";
	private static int ageIndex = -1;

	private static final String WEIGHT_NAME = "WP";
	private static int weightIndex = -1;

	private static final String GENDER_NAME = "F50002";
	private static int genderIndex = -1;

	// /////////////////////////////////////////////////////////////////////////
	// attributes
	private final Boolean employed;
	private final Id id;
	private final int dayOfWeek;
	private final Boolean license;
	private final int age;
	private final double weight;
	private final String gender;

	// /////////////////////////////////////////////////////////////////////////
	// other fields
	private final Map<Id, MzWeg> wege = new HashMap<Id, MzWeg>();

	// /////////////////////////////////////////////////////////////////////////
	// structure extraction
	public static void notifyStructure(final String headLine) {
		String[] names = headLine.split("\t");

		for (int i=0; i < names.length; i++) {
			if (names[ i ].equals( EMPLOYED_NAME )) {
				employedIndex = i;
			}
			else if (names[ i ].equals( ID_NAME )) {
				idIndex  = i;
			}
			else if (names[ i ].equals( DOW_NAME )) {
				dayOfWeekIndex = i;
			}
			else if (names[ i ].equals( LICENCE_NAME )) {
				licenceIndex = i;
			}
			else if (names[ i ].equals( AGE_NAME )) {
				ageIndex = i;
			}
			else if (names[ i ].equals( WEIGHT_NAME )) {
				weightIndex = i;
			}
			else if (names[ i ].equals( GENDER_NAME )) {
				genderIndex = i;
			}
		}

		structureIsKnown = true;
	}

	// /////////////////////////////////////////////////////////////////////////
	// construction
	// /////////////////////////////////////////////////////////////////////////
	public MzPerson(final String line) {
		if (!structureIsKnown) throw new IllegalStateException( "structure of file unknown" );
		String[] lineArray = line.split("\t");

		this.employed = booleanField( lineArray[ employedIndex ] );
		this.id = new IdImpl( lineArray[ idIndex ].trim() );
		this.dayOfWeek = dayOfWeek( lineArray[ dayOfWeekIndex ] );
		this.license = booleanField( lineArray[ licenceIndex ] );
		this.age = Integer.parseInt( lineArray[ ageIndex ] );
		this.weight = Double.parseDouble( lineArray[ weightIndex ] );
		this.gender = gender( lineArray[ genderIndex] );
	}

	private Boolean booleanField( final String value ) {
		int intValue = Integer.parseInt( value );

		return intValue == 1 ? true :
			(intValue == 2 ? false : null);
	}

	private int dayOfWeek( final String value ) {
		int intValue = Integer.parseInt( value );

		if (intValue < 1 || intValue > 7) throw new IllegalArgumentException( "unknown day "+value );

		return intValue;
	}

	private String gender( final String value ) {
		int intValue = Integer.parseInt( value );

		return intValue == 1 ? "m" : (intValue == 2 ? "f" : null);
	}

	// /////////////////////////////////////////////////////////////////////////
	// getters
	// /////////////////////////////////////////////////////////////////////////
	@Override
	public Id getId() {
		return id;
	}

	public Person getPerson() {
		PersonImpl person = new PersonImpl( id );

		setPersonAttributes( person );

		PlanImpl plan = person.createAndAddPlan( true );
		List< MzWeg > trips = new ArrayList< MzWeg >( wege.values() );

		// get the trips in proper sequence
		Collections.sort(
				trips ,
				new Comparator< MzWeg >(){
					@Override
					public int compare(final MzWeg o1, final MzWeg o2) {
						return Double.compare( o1.getDepartureTime(), o2.getDepartureTime() );
					}
				} );

		// create plan
		Activity currentAct = plan.createAndAddActivity( HOME );
		MzAdress homeAdress = trips.size() > 0 ?
			trips.get( 0 ).getDepartureAdress() :
			null;

		// TODO: check O/D consistency (departure from previous arrival).
		for (MzWeg weg : trips) {
			// do not include round trips
			if ( weg.getArrivalAdress().equals( weg.getDepartureAdress() ) ) continue;
			
			currentAct.setEndTime( weg.getDepartureTime() );

			String actType = null;
			switch (weg.getPurpose()) {
				case work:
					actType = WORK;
					break;
				case leisure:
					actType = LEISURE;
					break;
				case educ:
					actType = EDUC;
					break;
				case shop:
					actType = SHOP;
					break;
				case service:
					log.warn( "got a service trip not round. setting activity type as leisure." );
			}

			Leg leg = weg.getLeg();
			plan.addLeg( leg );
			
			if ( weg.getArrivalAdress().equals( homeAdress ) ) {
				currentAct = plan.createAndAddActivity( HOME );
			}
			else {
				currentAct = plan.createAndAddActivity( actType );
			}

			currentAct.setStartTime( leg.getDepartureTime() );
		}

		return person;
	}

	private void setPersonAttributes( final PersonImpl person ) {
		person.setAge( age );
		person.setEmployed( employed );
		person.setLicence( license.toString() );
		person.setSex( gender );

		// score corresponds to the weight
		person.getSelectedPlan().setScore( weight );

		// day of week in the plan type
		((PlanImpl) person.getSelectedPlan()).setType( ""+dayOfWeek );
	}

	// /////////////////////////////////////////////////////////////////////////
	// creation methods
	// /////////////////////////////////////////////////////////////////////////
	public void addWeg(final MzWeg weg) {
		if (wege.put( weg.getId() , weg ) != null) {
			throw new RuntimeException( "same weg created twice" );
		}
	}

	public void addEtappe(final MzEtappe etappe) {
		MzWeg enclosingWeg = wege.get( etappe.getWegId() );

		if (enclosingWeg == null) throw new RuntimeException( "trying to add an etappe before the weg" );

		enclosingWeg.addEtappe( etappe );
	}
}

class MzWeg implements Identifiable {
	public enum Purpose {work, educ, leisure, shop, transitInteraction, service}; 
	// /////////////////////////////////////////////////////////////////////////
	// static fields
	private static boolean structureIsKnown = false;

	private static final String PERSON_NAME = "INTNR";
	private static int personIndex = -1;

	private static final String WEGNR_NAME = "WEG";
	private static int wegnrIndex = -1;

	private static final String DEPARTURE_TIME_NAME = "WVON";
	private static int departureTimeIndex = -1;

	private static final String ARRIVAL_TIME_NAME = "WBIS";
	private static int arrivalTimeIndex = -1;

	private static final String DISTANCE_NAME = "WEGDIST";
	private static int distanceIndex = -1;

	private static final String DURATION_NAME = "WDAUER2";
	private static int durationIndex = -1;

	private static final String PURPOSE_NAME = "WZWECK2";
	private static int purposeIndex = -1;

	private static final String START_ORT_NAME = "W61201";
	private static int startOrtIndex = -1;

	private static final String START_STREET_NAME = "W61202";
	private static int startStreetIndex = -1;

	private static final String START_STREET_NR_NAME = "W61203";
	private static int startStreetNrIndex = -1;

	private static final String END_ORT_NAME = "W61601";
	private static int endOrtIndex = -1;

	private static final String END_STREET_NAME = "W61602";
	private static int endStreetIndex = -1;

	private static final String END_STREET_NR_NAME = "W61603";
	private static int endStreetNrIndex = -1;

	// /////////////////////////////////////////////////////////////////////////
	// attributes
	private final Id personId;
	private final Id wegId;
	private final double departureTime;
	private final double arrivalTime;
	private final double distance;
	private final double duration;
	private final MzAdress startAdress;
	private final MzAdress endAdress;
	private final Purpose purpose;

	// /////////////////////////////////////////////////////////////////////////
	// other fields
	private final Map<Id, MzEtappe> etappen = new HashMap<Id, MzEtappe>();

	public static void notifyStructure(final String headLine) {
		String[] names = headLine.split("\t");

		for (int i=0; i < names.length; i++) {
			if (names[ i ].equals( PERSON_NAME )) {
				personIndex = i;
			}
			if (names[ i ].equals( WEGNR_NAME )) {
				wegnrIndex = i;
			}
			if (names[ i ].equals( DEPARTURE_TIME_NAME )) {
				departureTimeIndex = i;
			}
			if (names[ i ].equals( ARRIVAL_TIME_NAME )) {
				arrivalTimeIndex = i;
			}
			if (names[ i ].equals( DISTANCE_NAME )) {
				distanceIndex = i;
			}
			if (names[ i ].equals( DURATION_NAME )) {
				durationIndex = i;
			}
			if (names[ i ].equals( PURPOSE_NAME )) {
				purposeIndex = i;
			}
			if (names[ i ].equals( START_ORT_NAME )) {
				startOrtIndex = i;
			}
			if (names[ i ].equals( START_STREET_NAME )) {
				startStreetIndex = i;
			}
			if (names[ i ].equals( START_STREET_NR_NAME )) {
				startStreetNrIndex = i;
			}
			if (names[ i ].equals( END_ORT_NAME )) {
				endOrtIndex = i;
			}
			if (names[ i ].equals( END_STREET_NAME )) {
				endStreetIndex = i;
			}
			if (names[ i ].equals( END_STREET_NR_NAME )) {
				endStreetNrIndex = i;
			}
		}

		structureIsKnown = true;
	}

	// /////////////////////////////////////////////////////////////////////////
	// constructor
	// /////////////////////////////////////////////////////////////////////////
	public MzWeg ( final String line ) {
		if (!structureIsKnown) throw new IllegalStateException( "structure of file unknown" );
		String[] lineArray = line.split( "\t" );

		this.personId = new IdImpl( lineArray[ personIndex ].trim() );
		this.wegId = new IdImpl( lineArray[ wegnrIndex ].trim() );
		this.departureTime = time( lineArray[ departureTimeIndex ] );
		this.arrivalTime = time( lineArray[ arrivalTimeIndex ] );
		this.distance = distance( lineArray[ distanceIndex ] );
		this.duration = time( lineArray[ durationIndex ] );
		this.startAdress = MzAdress.createAdress(
				lineArray[ startOrtIndex ],
				lineArray[ startStreetIndex ],
				lineArray[ startStreetNrIndex ] );
		this.endAdress = MzAdress.createAdress(
				lineArray[ endOrtIndex ],
				lineArray[ endStreetIndex ],
				lineArray[ endStreetNrIndex ] );
		this.purpose = purpose( lineArray[ purposeIndex ] );
	}

	private double time( final String value ) {
		// min -> secs
		return Double.parseDouble( value ) * 60d;
	}

	private double distance( final String value ) {
		// km -> m
		return Double.parseDouble( value ) * 1000d;
	}

	private Purpose purpose( final String value ) {
		int i = Integer.parseInt( value.trim() );

		switch ( i ) {
			case 0: return Purpose.transitInteraction;
			case 1: // Arbeit
			case 4: // Geschäftliche Tätigkeit
					return Purpose.work;
			case 2: return Purpose.educ;
			case 3: return Purpose.shop;
			case 5: // Dienstfahrt
			case 6: // Serviceweg
			case 8: // Begleitweg
					return Purpose.service;
			case 9: // no answer
			default: return null; // or fail with custom exception?
		}
	}

	// /////////////////////////////////////////////////////////////////////////
	// getters
	// /////////////////////////////////////////////////////////////////////////
	@Override
	public Id getId() {
		return wegId;
	}

	public Id getPersonId() {
		return personId;
	}

	public MzAdress getDepartureAdress() {
		return startAdress;
	}

	public MzAdress getArrivalAdress() {
		return endAdress;
	}

	public Purpose getPurpose() {
		return purpose;
	}

	public double getDepartureTime() {
		return departureTime;
	}

	public Leg getLeg() {
		LegImpl leg = new LegImpl( getMainMode() );

		leg.setDepartureTime( departureTime );
		// TODO: check consistency tt / arrival time
		leg.setArrivalTime( arrivalTime );
		leg.setTravelTime( duration );

		NetworkRoute route = new LinkNetworkRouteImpl(null, null);
		leg.setRoute(route);
		route.setDistance(distance);
		route.setTravelTime(leg.getTravelTime());

		return leg;
	}

	// ////////////////////// helpers for the getters //////////////////////////
	private String getMainMode() {
		double longestDistance = Double.NEGATIVE_INFINITY;
		String mode = null;

		// main mode is the mode of the longest etap
		for (MzEtappe etappe : etappen.values()) {
			if (etappe.getDistance() > longestDistance) {
				longestDistance = etappe.getDistance();
				mode = etappe.getMode();
			}
		}

		return mode;
	}

	// /////////////////////////////////////////////////////////////////////////
	// creation methods
	// /////////////////////////////////////////////////////////////////////////
	public void addEtappe(final MzEtappe etappe) {
		if (etappen.put( etappe.getId() , etappe ) != null) {
			throw new RuntimeException( "same etappe created twice" );
		}	
	}
}

class MzEtappe implements Identifiable {
	// /////////////////////////////////////////////////////////////////////////
	// static fields
	private static boolean structureIsKnown = false;

	private static final String PERSON_NAME = "INTNR";
	private static int personIndex = -1;

	private static final String WEGNR_NAME = "WEG";
	private static int wgnrIndex = -1;

	private static final String ETAPPENR_NAME = "ETAPPE";
	private static int etappenrIndex = -1;

	private static final String DISTANCE_NAME = "F61500";
	private static int distanceIndex = -1;

	private static final String MODE_NAME = "F61300";
	private static int modeIndex = -1;

	// /////////////////////////////////////////////////////////////////////////
	// attributes
	private final Id personId;
	private final Id wegId;
	private final Id id;
	private final double distance;
	private final String mode;


	public static void notifyStructure(final String headLine) {
		String[] names = headLine.split("\t");

		for (int i=0; i < names.length; i++) {
			if (names[ i ].equals( PERSON_NAME )) {
				personIndex = i;
			}
			if (names[ i ].equals( WEGNR_NAME )) {
				wgnrIndex = i;
			}
			if (names[ i ].equals( ETAPPENR_NAME )) {
				etappenrIndex = i;
			}
			if (names[ i ].equals( DISTANCE_NAME )) {
				distanceIndex = i;
			}
			if (names[ i ].equals( MODE_NAME )) {
				modeIndex = i;
			}
		}

		structureIsKnown = true;
	}

	// /////////////////////////////////////////////////////////////////////////
	// constructor
	// /////////////////////////////////////////////////////////////////////////
	public MzEtappe(final String line) {
		if (!structureIsKnown) throw new IllegalStateException( "structure of file unknown" );
		String[] lineArray = line.split( "\t" );

		this.personId = new IdImpl( lineArray[ personIndex ].trim() );
		this.wegId = new IdImpl( lineArray[ wgnrIndex ].trim() );
		this.id = new IdImpl( lineArray[ etappenrIndex ].trim() );
		this.distance = distance( lineArray[ distanceIndex ].trim() );
		this.mode = mode( lineArray[ modeIndex ].trim() );
	}

	private String mode( final String value ) {
		int i = Integer.parseInt( value );

		switch (i) {
			case 1: return TransportMode.walk;
			case 2: return TransportMode.bike;
			case 3: // all motorised private vehicles (including
			case 4: // as passenger) are considered as "car".
			case 5: // this is done mainly becaus this will be optimised
			case 6: // anyway.
			case 23:
			case 7: return TransportMode.car;
			case 8:
			case 9:
			case 10:
			case 11:
			case 12:
			case 13:
			case 14:
			case 15:
			case 16:
			case 17: return TransportMode.pt;
			default: return "unknown";
		}
	}

	private double distance( final String value ) {
		// km -> m
		return Double.parseDouble( value ) * 1000d;
	}

	// /////////////////////////////////////////////////////////////////////////
	// getters
	// /////////////////////////////////////////////////////////////////////////
	@Override
	public Id getId() {
		return id;
	}

	public String getMode() {
		return mode;
	}

	public double getDistance() {
		return distance;
	}

	public Id getPersonId() {
		return personId;
	}

	public Id getWegId() {
		return wegId;
	}
}

class MzAdress {
	private final String adress;

	private MzAdress(
			final String ortCode,
			final String street,
			final String streetNr) {
		this.adress = (street.toLowerCase().trim()+
				" "+streetNr.toLowerCase().trim()+
				" "+ortCode.trim()).trim();
	}

	@Override
	public int hashCode() {
		return adress.hashCode();
	}

	@Override
	public boolean equals(final Object other) {
		return other instanceof MzAdress &&
			((MzAdress) other).adress.equals( adress );
	}

	// in prevision for the possible need of pooling
	static MzAdress createAdress(
			final String ortCode,
			final String street,
			final String streetNr) {
		return new MzAdress(ortCode , street , streetNr);
	}
}
