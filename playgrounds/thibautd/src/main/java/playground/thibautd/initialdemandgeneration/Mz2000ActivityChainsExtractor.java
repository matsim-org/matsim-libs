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
import java.util.Arrays;
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
 * used to indicate that the activity chain is inconsistent, and should not be
 * retained.
 */
class UnconsistentMzRecordException extends Exception {
	private static final long serialVersionUID = 1L;
	public UnconsistentMzRecordException(final String msg) {
		super( msg );
	}
}

/**
 * general remarks on the fields:
 * interview number used as a person id
 */
class MzPopulation {
	private static final Logger log =
		Logger.getLogger(MzPopulation.class);

	private final Map<Id, MzPerson> persons = new HashMap<Id, MzPerson>();

	public Scenario getScenario() {
		ScenarioImpl scen = (ScenarioImpl) ScenarioUtils.createScenario(
					ConfigUtils.createConfig());

		Population population = scen.getPopulation();

		Person matsimPerson;
		for (MzPerson person : persons.values()) {
			try {
				matsimPerson = person.getPerson();
			} catch (UnconsistentMzRecordException e) {
				// entry is inconsistent: inform user and pass to the next
				// log.info( "got unconsistent entry: "+e.getMessage() );
				continue;
			}
			population.addPerson( matsimPerson );
		}

		MzPerson.printStatistcs();
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
	private static final Statistics stats = new Statistics();
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
	// static methods
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

	public static void printStatistcs() {
		stats.printStats();
	}

	// /////////////////////////////////////////////////////////////////////////
	// construction
	// /////////////////////////////////////////////////////////////////////////
	public MzPerson(final String line) {
		if (!structureIsKnown) throw new IllegalStateException( "structure of file unknown" );
		String[] lineArray = line.split("\t");

		try {
			this.employed = booleanField( lineArray[ employedIndex ] );
			this.id = new IdImpl( lineArray[ idIndex ].trim() );
			this.dayOfWeek = dayOfWeek( lineArray[ dayOfWeekIndex ] );
			this.age = Integer.parseInt( lineArray[ ageIndex ] );
			this.license = licence( age , lineArray[ licenceIndex ] );
			this.weight = Double.parseDouble( lineArray[ weightIndex ] );
			this.gender = gender( lineArray[ genderIndex] );
		} catch (Exception e) {
			throw new RuntimeException(
					"problem while parsing line"+Arrays.toString(lineArray),
					e);
		}
	}

	private Boolean licence(final int age, final String value) {
		// for children, no value
		if ( age < 18 ) return false;
		return booleanField( value );
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

	/**
	 * @throws UnconsistentMzRecordException if the resulting plan contains
	 * inconsistencies.
	 */
	public Person getPerson() throws UnconsistentMzRecordException {
		stats.totalPersonsProcessed();
		PersonImpl person = new PersonImpl( id );

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
		MzAdress currentActivityAdress = homeAdress;
		double currentTime = 0;

		// TODO: check O/D consistency (departure from previous arrival).
		for (MzWeg weg : trips) {
			// do not include round trips
			if ( weg.getArrivalAdress().equals( weg.getDepartureAdress() ) ) {
				// log.info( "round trip for agent "+id );
				stats.roundTrip();
				// continue;
				throw new UnconsistentMzRecordException( "round trip" );
			}
			
			if (!weg.getDepartureAdress().equals( currentActivityAdress )) {
				stats.addressInconsistency();
				throw new UnconsistentMzRecordException( "adress inconsistency" );
			}
			if (weg.getDepartureTime() < currentTime) {
				stats.timeSequenceInconsistency();
				throw new UnconsistentMzRecordException( "time inconsistency : departure before previous arrival" );
			}
			if (Math.abs( weg.getArrivalTime() - weg.getDepartureTime()
						- weg.getDuration() ) > 1E-7 ) {
				stats.tripDurationInconsistency();
				throw new UnconsistentMzRecordException( "time inconsistency : arrival - departure is not duration" );
			}

			currentActivityAdress = weg.getArrivalAdress();
			currentTime = weg.getArrivalTime();
			currentAct.setEndTime( weg.getDepartureTime() );

			//String actType = null;
			//switch ( weg.getPurpose() ) {
			//	case work:
			//		actType = WORK;
			//		break;
			//	case service:
			//		log.warn( "got a service trip not round. setting activity type as leisure." );
			//	case leisure:
			//		actType = LEISURE;
			//		break;
			//	case educ:
			//		actType = EDUC;
			//		break;
			//	case shop:
			//		actType = SHOP;
			//		break;
			//	case unknown:
			//		actType = "";
			//		break;
			//	default:
			//		throw new RuntimeException( "unknown value "+weg.getPurpose() );
			//}
			String actType = ""+weg.getPurpose();

			Leg leg = weg.getLeg();
			plan.addLeg( leg );
			
			if ( weg.getArrivalAdress().equals( homeAdress ) ) {
			// if (false) {
				currentAct = plan.createAndAddActivity( HOME );
			}
			else {
				currentAct = plan.createAndAddActivity( actType );
			}

			currentAct.setStartTime( leg.getDepartureTime() + leg.getTravelTime() );
		}

		setPersonAttributes( person );

		stats.totallyProcessedPerson();
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

	// /////////////////////////////////////////////////////////////////////////
	// statistics tracking: helper class
	// /////////////////////////////////////////////////////////////////////////
	// TODO: "externalize"
	private static class Statistics {
		private int addressInconsistencies = 0;
		private int timeSequenceInconsistencies = 0;
		private int tripDurationInconsistencies = 0;
		private int totalPersonsProcessed = 0;
		private int totallyProcessedPersons = 0;
		private int roundTrips = 0;

		public void roundTrip() {
			roundTrips++;
		}

		public void addressInconsistency() {
			addressInconsistencies++;
		}

		public void timeSequenceInconsistency() {
			timeSequenceInconsistencies++;
		}

		public void tripDurationInconsistency() {
			tripDurationInconsistencies++;
		}

		public void totalPersonsProcessed() {
			totalPersonsProcessed++;
		}

		public void totallyProcessedPerson() {
			totallyProcessedPersons++;
		}

		public void printStats() {
			log.info( "-----------> statistics on the processing:" );
			log.info( "round trips (ignored): "+roundTrips );
			log.info( "adress inconsistencies: "+addressInconsistencies );
			log.info( "time sequence inconsistencies: "+timeSequenceInconsistencies );
			log.info( "trip duration inconsistencies: "+tripDurationInconsistencies );
			log.info( "total number of persons: "+totalPersonsProcessed );
			log.info( "number of persons retained: "+totallyProcessedPersons );
			MzAdress.printStatistics();
			log.info( "<----------- end of statistics on the processing" );
		}
	}
}

class MzWeg implements Identifiable {
	/**
	 * reproduces (almost) all the possible answers of the MZ (exception: two kind
	 * of "work" purpose, ie professional activity and work, are merged).
	 */
	public enum Purpose {
		work,
		educ,
		leisure,
		shop,
		transitTransfer,
		useService,
		servePassengerRide,
		accompany,
		unknown}; 

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
			case 0: return Purpose.transitTransfer;
			case 1: // Arbeit
			case 4: // Geschäftliche Tätigkeit
					return Purpose.work;
			case 2: return Purpose.educ;
			case 3: return Purpose.shop;
			case 5: return Purpose.useService; // Dienstfahrt
			case 6: return Purpose.servePassengerRide;// Serviceweg
			case 8: return Purpose.accompany; // Begleitweg
			case 9: // no answer
			default: return Purpose.unknown; // or fail with custom exception?
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

	public double getArrivalTime() {
		return arrivalTime;
	}

	public double getDuration() {
		return duration;
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
			case 1: // Zu Fuss
				return TransportMode.walk;
			case 2: // Velo
				return TransportMode.bike;
			case 6: // Auto als Fahrer
				return TransportMode.car;
			case 7: // Auto als Mitfahrer
				return TransportMode.ride;
			case 8: // Bahn
			case 9: // Postauto
			case 10: // Bus
			case 11: // Tram
			case 12: // Taxi
			case 13: // Reisecar
			case 14: // Lastwagen
			case 15: // Schiff
			case 16: // Flugzeug
			case 17: // Zahnradbahn, Seilbahn, Standseilbahn, Sessellift, Skilift
				return TransportMode.pt;
			case 23: // Kleinmotorrad
			case 3: // Mofa (Motorfahrrad)
			case 4: // Motorrad als Fahrer
			case 5: // Motorrad als Mitfahrer
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
	private static final Logger log =
		Logger.getLogger(MzAdress.class);

	private final String adress;
	
	private static int nCreatedAdresses = 0;
	private static int nCreatedFullAdresses = 0;

	private MzAdress(
			final String ortCode,
			final String street,
			final String streetNr) {
		this.adress = (street.toLowerCase().trim()+
				" "+streetNr.toLowerCase().trim()+
				" "+ortCode.trim()).trim();

		nCreatedAdresses++;
		if (!adress.equals( ortCode.trim() )) nCreatedFullAdresses++;
	}

	public static void printStatistics() {
		log.info( "number of created adresses: "+nCreatedAdresses );
		log.info( "number of created full adresses: "+nCreatedFullAdresses );
		log.info( "=> proportion of adresses only consisting in ZIP Code: "+
				(100 * ( ((double) nCreatedAdresses - (double) nCreatedFullAdresses)
						 / (double) nCreatedAdresses ))+
				"%");
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

	static MzAdress createAdress(
			final String ortCode,
			final String street,
			final String streetNr) {
		return new MzAdress(ortCode , street , streetNr);
	}
}
