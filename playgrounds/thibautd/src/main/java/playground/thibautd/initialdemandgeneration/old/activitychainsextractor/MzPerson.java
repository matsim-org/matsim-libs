package playground.thibautd.initialdemandgeneration.old.activitychainsextractor;

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

import playground.thibautd.initialdemandgeneration.old.activitychainsextractor.MzConfig.Statistics;

class MzPerson implements Identifiable {
	private static final String HOME = "h";
	private static final String EDUC = "e";
	private static final String SHOP = "s";
	private static final String WORK = "w";
	private static final String LEISURE = "l";

	private static final double SHORT_DURATION = 10 * 60;
	private static final Coord COORD = new CoordImpl(0, 0);

	// /////////////////////////////////////////////////////////////////////////
	// static fields
	private final Statistics stats;

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
	// construction
	// /////////////////////////////////////////////////////////////////////////
	public MzPerson(
			final Statistics stats,
			final Id id,
			final Boolean employed,
			final Boolean education,
			final int dayOfWeek,
			final Boolean license,
			final int age,
			final double weight,
			final String gender) {
		this.stats = stats;
		this.id = id;
		this.employed = employed;
		this.education = education;
		this.dayOfWeek = dayOfWeek;
		this.age = age;
		this.license = license;
		this.weight = weight;
		this.gender = gender;
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
}
