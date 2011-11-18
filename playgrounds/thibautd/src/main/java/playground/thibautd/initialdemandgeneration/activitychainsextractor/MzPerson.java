package playground.thibautd.initialdemandgeneration.activitychainsextractor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.geometry.CoordImpl;

class MzPerson implements Identifiable {
	private static final Logger log =
		Logger.getLogger(MzPerson.class);

	private static final String HOME = "h";
	private static final String EDUC = "e";
	private static final String SHOP = "s";
	private static final String WORK = "w";
	private static final String LEISURE = "l";

	private static final double SHORT_DURATION = 10 * 60;
	private static final Coord COORD = new CoordImpl(0, 0);

	// /////////////////////////////////////////////////////////////////////////
	// static fields
	private static final Statistics stats = new Statistics();
	private static boolean structureIsKnown = false;


	private static interface Consts2000 {
		static final String EDUCATION_NAME = "F50004";
		static final String EMPLOYED_NAME = "F50003";
		static final String ID_NAME = "INTNR";
		static final String DOW_NAME = "DAYSTTAG";
		static final String LICENCE_NAME = "F50005";
		static final String AGE_NAME = "F50001";
		static final String WEIGHT_NAME = "WP";
		static final String GENDER_NAME = "F50002";
	}

	private static interface Consts1994 {
		static final String EDUCATION_NAME = "ZP04";
		static final String EMPLOYED_NAME = "ZP03";
		static final String PERSON_NAME = "PERSON";
		static final String HH_NAME = "HAUSHALT";
		static final String DOW_NAME = "ZP_WTAGF";
		static final String LICENCE_NAME = "ZP05";
		static final String AGE_NAME = "ZP01";
		static final String WEIGHT_NAME = "WP";
		static final String GENDER_NAME = "ZP02";
	}

	private static int employedIndex = -1;
	private static int educationIndex = -1;
	private static int personIndex = -1;
	private static int hhIndex = -1;
	private static int dayOfWeekIndex = -1;
	private static int licenceIndex = -1;
	private static int ageIndex = -1;
	private static int weightIndex = -1;
	private static int genderIndex = -1;

	// /////////////////////////////////////////////////////////////////////////
	// attributes
	private final Boolean employed;
	private final Boolean education;
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

		switch (GlobalMzInformation.getMzYear()) {
			case 2000:
				for (int i=0; i < names.length; i++) {
					if (names[ i ].equals( Consts2000.EMPLOYED_NAME )) {
						employedIndex = i;
					}
					if (names[ i ].equals( Consts2000.EDUCATION_NAME )) {
						educationIndex = i;
					}
					else if (names[ i ].equals( Consts2000.ID_NAME )) {
						personIndex  = i;
					}
					else if (names[ i ].equals( Consts2000.DOW_NAME )) {
						dayOfWeekIndex = i;
					}
					else if (names[ i ].equals( Consts2000.LICENCE_NAME )) {
						licenceIndex = i;
					}
					else if (names[ i ].equals( Consts2000.AGE_NAME )) {
						ageIndex = i;
					}
					else if (names[ i ].equals( Consts2000.WEIGHT_NAME )) {
						weightIndex = i;
					}
					else if (names[ i ].equals( Consts2000.GENDER_NAME )) {
						genderIndex = i;
					}
				}
				break;
			case 1994:
				for (int i=0; i < names.length; i++) {
					if (names[ i ].equals( Consts1994.EMPLOYED_NAME )) {
						employedIndex = i;
					}
					if (names[ i ].equals( Consts1994.EDUCATION_NAME )) {
						educationIndex = i;
					}
					else if (names[ i ].equals( Consts1994.PERSON_NAME )) {
						personIndex  = i;
					}
					else if (names[ i ].equals( Consts1994.HH_NAME )) {
						hhIndex  = i;
					}
					else if (names[ i ].equals( Consts1994.DOW_NAME )) {
						dayOfWeekIndex = i;
					}
					else if (names[ i ].equals( Consts1994.LICENCE_NAME )) {
						licenceIndex = i;
					}
					else if (names[ i ].equals( Consts1994.AGE_NAME )) {
						ageIndex = i;
					}
					else if (names[ i ].equals( Consts1994.WEIGHT_NAME )) {
						weightIndex = i;
					}
					else if (names[ i ].equals( Consts1994.GENDER_NAME )) {
						genderIndex = i;
					}
				}
				break;
			default:
				throw new IllegalStateException( "unhandled mz year "+GlobalMzInformation.getMzYear() );
		}

		structureIsKnown = true;
	}

	public static void printStatistcs() {
		stats.printStats();
	}

	static Id id94(final String pers , final String hh) {
		return new IdImpl( pers.trim() + "-" + hh.trim() );
	}

	// /////////////////////////////////////////////////////////////////////////
	// construction
	// /////////////////////////////////////////////////////////////////////////
	public MzPerson(final String line) {
		if (!structureIsKnown) throw new IllegalStateException( "structure of file unknown" );
		String[] lineArray = line.split("\t");

		try {
			switch (GlobalMzInformation.getMzYear()) {
				case 2000:
					this.id = new IdImpl( lineArray[ personIndex ].trim() );
					break;
				case 1994:
					this.id = id94( lineArray[ personIndex ] , lineArray[ hhIndex ] );
					break;
				default:
					throw new IllegalStateException( "unhandled year "+GlobalMzInformation.getMzYear());
			}

			this.employed = booleanField( lineArray[ employedIndex ] );
			this.education = booleanField( lineArray[ educationIndex ] );
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

	public int getDayOfWeek() {
		return dayOfWeek;
	}

	/**
	 * @throws UnhandledMzRecordException if the resulting plan contains
	 * inconsistencies.
	 */
	public Person getPerson() throws UnhandledMzRecordException {
		// TODO: rearrange and separate in *a lot* of little methods: awfully
		// messy and unreadable!
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
		Activity currentAct = plan.createAndAddActivity( HOME , COORD );
		MzAdress homeAdress = trips.size() > 0 ?
			trips.get( 0 ).getDepartureAdress() :
			null;
		MzAdress currentActivityAdress = homeAdress;
		MzWeg.Purpose lastPurpose = null;
		boolean lastActivityWasHome = true;
		double currentTime = 0;
		double sharedDeparture = Double.NaN;

		// TODO: check O/D consistency (departure from previous arrival).
		for (MzWeg weg : trips) {
			// do not include round trips
			// pb: location not precise enough to do this.
			// if ( weg.getArrivalAdress().equals( weg.getDepartureAdress() ) ) {
			// 	// // log.info( "round trip for agent "+id );
			// 	stats.roundTrip();
			// 	continue;
			// 	// throw new UnhandledMzRecordException( "round trip" );
			// }
			
			if (!weg.getDepartureAdress().equals( currentActivityAdress )) {
				stats.addressInconsistency();
				throw new UnhandledMzRecordException( "adress inconsistency" );
			}
			if (weg.getDepartureTime() < currentTime) {
				stats.timeSequenceInconsistency();
				throw new UnhandledMzRecordException( "time inconsistency : departure before previous arrival" );
			}
			if (Math.abs( weg.getArrivalTime() - weg.getDepartureTime()
						- weg.getDuration() ) > 1E-7 ) {
				stats.tripDurationInconsistency();
				throw new UnhandledMzRecordException( "time inconsistency : arrival - departure is not duration" );
			}

			Leg leg = weg.getLeg();

			currentActivityAdress = weg.getArrivalAdress();

			// is this a "serve passenger" ride?
			if (weg.getPurpose().equals( MzWeg.Purpose.servePassengerRide ) ||
					weg.getPurpose().equals( MzWeg.Purpose.accompany ) ||
					weg.getPurpose().equals( MzWeg.Purpose.transitTransfer )) {
				if ( MzWeg.Purpose.servePassengerRide.equals( lastPurpose ) ||
						MzWeg.Purpose.accompany.equals( lastPurpose ) ||
						MzWeg.Purpose.transitTransfer.equals( lastPurpose )) {
					// several serve passengers: just check that the in-ride activity
					// is acceptable
					if (weg.getDepartureTime() - currentTime > SHORT_DURATION) {
						stats.longServePassengerAct();
						throw new UnhandledMzRecordException( "cannot handle long serve passenger activities" );
					}
					currentTime = weg.getArrivalTime();
					continue;
				}
				// just remember that we served a passenger
				lastPurpose = MzWeg.Purpose.servePassengerRide;
				sharedDeparture = weg.getDepartureTime();
				currentTime = weg.getArrivalTime();
				currentAct.setEndTime( weg.getDepartureTime() );
				continue;
			}
			else if ( MzWeg.Purpose.servePassengerRide.equals( lastPurpose ) ||
						MzWeg.Purpose.accompany.equals( lastPurpose ) ||
						MzWeg.Purpose.transitTransfer.equals( lastPurpose )) {
				if (weg.getDepartureTime() - currentTime > SHORT_DURATION) {
					stats.longServePassengerAct();
					throw new UnhandledMzRecordException( "cannot handle long serve passenger activities" );
				}

				leg.setDepartureTime( sharedDeparture );
				leg.setTravelTime( leg.getTravelTime() + (weg.getDepartureTime() - sharedDeparture) );
			}
			else {
				currentAct.setEndTime( weg.getDepartureTime() );
			}

			currentTime = weg.getArrivalTime();

			String actType = "unknown";

			switch (weg.getPurpose()) {
				case work:
					actType = WORK;
					break;
				case commercialActivity:
				case shop:
				case useService:
					actType = SHOP;
					break;
				case educ:
					actType = EDUC;
					break;
				case leisure:
					actType = LEISURE;
					break;
				case servePassengerRide:
				case accompany:
				case transitTransfer:
					throw new RuntimeException( "got serve passenger activities after they were supposed to be processed" );
				case unknown:
					stats.unhandledActivityType();
					throw new UnhandledMzRecordException( "cannot handle activity type "+weg.getPurpose() );
			}

			plan.addLeg( leg );
			
			//if ( weg.getArrivalAdress().equals( homeAdress ) ) {
			// if (false) {
			if (weg.getPurpose().equals( lastPurpose ) &&
					weg.getArrivalAdress().equals( homeAdress ) &&
					!lastActivityWasHome) {
				currentAct = plan.createAndAddActivity( HOME , COORD );
				lastActivityWasHome = true;
			}
			else {
				currentAct = plan.createAndAddActivity( actType , COORD);
				lastActivityWasHome = false;
			}

			lastPurpose = weg.getPurpose();
			currentAct.setStartTime( leg.getDepartureTime() + leg.getTravelTime() );
		}

		if (!currentAct.getType().equals( HOME )) {
			stats.nonHomeBased();
			throw new UnhandledMzRecordException( "non home based plan" );
		}

		setPersonAttributes( person );

		removeRoundTrips( person );

		stats.totallyProcessedPerson();
		return person;
	}

	private void setPersonAttributes( final PersonImpl person ) {
		person.setAge( age );
		person.setEmployed( employed );
		person.setLicence( license ? "yes" : "no" );
		person.setSex( gender );

		// score corresponds to the weight
		person.getSelectedPlan().setScore( weight );

		if (education) {
			person.createDesires( "education" ).putActivityDuration( EDUC , 1234 );
		}

		// day of week in the plan type
		// ((PlanImpl) person.getSelectedPlan()).setType( ""+dayOfWeek );
	}

	private void removeRoundTrips( final PersonImpl person ) {
		Activity lastAct = null;
		List<PlanElement> newPlanElements = new ArrayList<PlanElement>();
		List<PlanElement> pePlanInternal = person.getSelectedPlan().getPlanElements();
		Iterator<PlanElement> iterator = pePlanInternal.iterator();

		while (iterator.hasNext()) {
			PlanElement pe = iterator.next();
			
			if (pe instanceof Activity) {
				newPlanElements.add( pe );
				lastAct = (Activity) pe;
			}
			else {
				Activity destination = (Activity) iterator.next();

				if ( !(destination.getType().equals( HOME ) &&
							lastAct.getType().equals( HOME )) ) {
					lastAct = destination;
					newPlanElements.add( pe );
					newPlanElements.add( destination );
				}
				else {
					lastAct.setEndTime( destination.getEndTime() );
					lastAct.setMaximumDuration( 
							lastAct.getEndTime() - lastAct.getStartTime() );
					stats.roundTrip();
				}
			}
		}

		pePlanInternal.clear();
		pePlanInternal.addAll( newPlanElements );
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
		private int unhandledActivityType = 0;
		private int nonHomeBased = 0;
		private int longServePassengerAct = 0;
		private int numerousServePassengersLegs = 0;

		public void numerousServePassengersLegs() {
			numerousServePassengersLegs++;
		}

		public void longServePassengerAct() {
			longServePassengerAct++;
		}

		public void nonHomeBased() {
			nonHomeBased++;
		}

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

		public void unhandledActivityType() {
			unhandledActivityType++;
		}

		public void printStats() {
			log.info( "-----------> statistics on the processing." );
			log.info( "beware: the counts count the reason for which plan construction was aborted" );
			log.info( "Actual number of interviews of each type may  be bigger." );
			log.info( "" );
			log.info( "round trips: "+roundTrips );
			log.info( "adress inconsistencies: "+addressInconsistencies );
			log.info( "time sequence inconsistencies: "+timeSequenceInconsistencies );
			log.info( "trip duration inconsistencies: "+tripDurationInconsistencies );
			log.info( "plans with unhandled activity types: "+unhandledActivityType );
			log.info( "plans with long serve passenger activities: "+longServePassengerAct );
			// log.info( "plans with numerous serve passenger legs: "+numerousServePassengersLegs );
			log.info( "non home based plans: "+nonHomeBased );
			log.info( "total number of persons: "+totalPersonsProcessed );
			log.info( "number of persons retained: "+totallyProcessedPersons );
			MzAdress.printStatistics();
			log.info( "<----------- end of statistics on the processing" );
		}
	}
}
