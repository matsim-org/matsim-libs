package org.matsim.application.analysis.population;

import com.google.inject.Injector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;

import java.util.List;

import static org.matsim.api.core.v01.TransportMode.pt;
import static org.matsim.application.analysis.population.AddVttsEtcToActivities.getMUSE_h;
import static org.matsim.application.analysis.population.AgentWiseComparisonKNUtils.MUTTS_AV;
import static org.matsim.application.analysis.population.AgentWiseComparisonKNUtils.isTestPerson;

class AgentWiseRuleOfHalfComputation{
	private static final Logger log = LogManager.getLogger( AgentWiseRuleOfHalfComputation.class );
	private final Population basePopulation;
	private final TripRouter tripRouter1;
	private final Population policyPopulation;
	private final TripRouter tripRouter2;
	private final Scenario baseScenario;

	public AgentWiseRuleOfHalfComputation( Injector injector, Injector injector2 ) {
		// I cannot use this via normal injection since I need access to base and to policy.  So using the two injectors as
		// constructor arguments gives me a lot of flexibility.  Means that upstream users need to use the injector rather than
		// plugging together things individually ... but in the end, I think it is better to inforce the injector here.  kai, mar'26

		this.basePopulation = injector.getInstance( Population.class );
		this.baseScenario = injector.getInstance( Scenario.class );
		this.tripRouter1 = injector.getInstance( TripRouter.class );

		this.policyPopulation = injector2.getInstance( Population.class );
		this.tripRouter2 = injector2.getInstance( TripRouter.class );
	}

	void somehowComputeRuleOfHalf( ){
		// could store the results into person attributes, then this would work w/o tablesaw

		Counter counter = new Counter( "somehowComputeRuleOfHalf; person # " );
		for( Person policyPerson : policyPopulation.getPersons().values() ){
			counter.incCounter();

			double sumTravTimeDiffsRemainersHom = 0.;
			double sumTravTimeDiffsRemainersHet = 0.;
			double sumTravTimeDiffsSwitchersHom = 0.;
			double sumTravTimeDiffsSwitchersHet = 0.;

			double sumIchangesDiffsRemainers = 0;
			double sumIchangesDiffsSwitchers = 0.;

			Person basePerson = basePopulation.getPersons().get( policyPerson.getId() );

			if( basePerson != null ){
				Double musl = (Double) getMUSE_h( basePerson.getSelectedPlan() );
				if( musl == null ){
					log.warn( "muse is null; I do not know why; personId={}", basePerson.getId() );
					musl = MUTTS_AV;
				}
				List<TripStructureUtils.Trip> baseTrips = TripStructureUtils.getTrips( basePerson.getSelectedPlan() );
				List<TripStructureUtils.Trip> policyTrips = TripStructureUtils.getTrips( policyPerson.getSelectedPlan() );
				Gbl.assertIf( baseTrips.size() == policyTrips.size() );
				for( int ii = 0 ; ii < baseTrips.size() ; ii++ ){
					TripStructureUtils.Trip baseTrip = baseTrips.get( ii );
					final String baseTripMainMode = TripStructureUtils.identifyMainMode( baseTrip.getTripElements() );
					final TripStructureUtils.Trip policyTrip = policyTrips.get( ii );
					final String policyMainMode = TripStructureUtils.identifyMainMode( policyTrip.getTripElements() );
					if( pt.equals( policyMainMode ) ){
						if( pt.equals( baseTripMainMode ) ){
							// Altnutzer; compute base ttime etc. from actual trip:
							double sumBaseTtime = 0.;
							double sumBaseLineSwitches = -1;
							for( Leg leg : baseTrip.getLegsOnly() ){
								sumBaseTtime += leg.getTravelTime().seconds();
								if( pt.equals( leg.getMode() ) ){
									sumBaseLineSwitches++;
								}
							}
							// compute policy ttime from actual trip:
							double sumPolicyTtime = 0.;
							double sumPolicyLineSwitches = -1.;
							for( Leg leg : policyTrip.getLegsOnly() ){
								sumPolicyTtime += leg.getTravelTime().seconds();
								if( pt.equals( leg.getMode() ) ){
									sumPolicyLineSwitches++;
								}
							}
							sumTravTimeDiffsRemainersHom += (sumPolicyTtime - sumBaseTtime) * MUTTS_AV * (-1);
							sumTravTimeDiffsRemainersHet += (sumPolicyTtime - sumBaseTtime) * musl * (-1);
							sumIchangesDiffsRemainers += (sumPolicyLineSwitches - sumBaseLineSwitches);
						} else{
							// Neunutzer; rule-of-half
							// first need hypothetical base travel time
							final Result baseResult = routeTrip( policyTrip, policyMainMode, policyPerson, tripRouter1 );
							// (this is deliberately using the policyTrip (and also the policyPerson) since otherwise the departure time is off in many cases)

							// then compute hypothetical policy travel time
							final Result policyResult = routeTrip( policyTrip, policyMainMode, policyPerson, tripRouter2 );
							// sum up (rule-of-half is done later):
							sumTravTimeDiffsSwitchersHom += (policyResult.sumTtime() - baseResult.sumTtime()) * MUTTS_AV * (-1);
							sumTravTimeDiffsSwitchersHet += (policyResult.sumTtime() - baseResult.sumTtime()) * musl * (-1);
							sumIchangesDiffsSwitchers += (policyResult.sumLineSwitches() - baseResult.sumLineSwitches());
						}
					} else if ( pt.equals( baseTripMainMode ) && !pt.equals( policyMainMode ) ) {
						// Wegwechsler.  Im relations-basierten Modell würde man die Wegwechsler vorher abziehen, und die RoH nur auf die Netto-Wechsler
						// anwenden.  Hier geht das nicht.  Also machen wir die gleiche Rechnung wie bei den Neunutzern, aber mit negativem VZ.

						// first need hypothetical base travel time
						final Result baseResult = routeTrip( baseTrip, baseTripMainMode, basePerson, tripRouter1 );
						if ( isTestPerson( basePerson.getId() ) ) {
							log.warn( "personId={}; baseResult={}", basePerson.getId(), baseResult );
						}
						// then compute hypothetical policy travel time
						final Result policyResult = routeTrip( baseTrip, baseTripMainMode, basePerson, tripRouter2 );
						// (this is deliberately using the baseTrip (and also the basePerson) since otherwise the departure time is off in many cases)
						if ( isTestPerson( policyPerson.getId() ) ) {
							log.warn( "personId={}; policyResult={}", policyPerson.getId(), baseResult );
						}
						// sum up (rule-of-half is done later):
						sumTravTimeDiffsSwitchersHom -= (policyResult.sumTtime() - baseResult.sumTtime()) * MUTTS_AV * (-1);
						sumTravTimeDiffsSwitchersHet -= (policyResult.sumTtime() - baseResult.sumTtime()) * musl * (-1);
						sumIchangesDiffsSwitchers -= (policyResult.sumLineSwitches() - baseResult.sumLineSwitches());
						// (Im relations-basierten Modell würde man die Wegwechsler vorher abziehen, und die RoH nur auf die Netto-Wechsler
						// anwenden.  Hier geht das nicht.  Also machen wir die gleiche Rechnung wie bei den Neunutzern, aber mit negativem VZ.)
					}
				}
			}
			setDiffs( TTIME_HR, REM, HOM, policyPerson, sumTravTimeDiffsRemainersHom / 3600. );
			setDiffs( TTIME_HR, REM, HET, policyPerson, sumTravTimeDiffsRemainersHet / 3600. );
			setDiffs( TTIME_HR, SWI, HOM, policyPerson, sumTravTimeDiffsSwitchersHom / 3600. );
			setDiffs( TTIME_HR, SWI, HET, policyPerson, sumTravTimeDiffsSwitchersHet / 3600. );
			setDiffs( IX, REM, HOM, policyPerson, sumIchangesDiffsRemainers );
			setDiffs( IX, SWI, HOM, policyPerson, sumIchangesDiffsSwitchers );
		}

	}

	private @NotNull Result routeTrip( TripStructureUtils.Trip baseTrip, String policyMainMode, Person basePerson, TripRouter tripRouter ){
		Facility fromFacility = FacilitiesUtils.toFacility( baseTrip.getOriginActivity(), baseScenario.getActivityFacilities() );
		Facility toFacility = FacilitiesUtils.toFacility( baseTrip.getDestinationActivity(), baseScenario.getActivityFacilities() );
		final List<? extends PlanElement> planElements = tripRouter.calcRoute( policyMainMode, fromFacility, toFacility, baseTrip.getOriginActivity().getEndTime().seconds(), basePerson, null );

		// count the number of line switches:
		double sumBaseTtime = 0.;
		double sumBaseLineSwitches = -1;
		for( Leg leg : TripStructureUtils.getLegs( planElements ) ){
			sumBaseTtime += leg.getTravelTime().seconds();
			if( pt.equals( leg.getMode() ) ){
				sumBaseLineSwitches++;
			}
		}

		// return the result:
		return new Result( sumBaseTtime, sumBaseLineSwitches );
	}

	private record Result(double sumTtime, double sumLineSwitches){
	}

	static String TTIME_HR="Ttime_hr";
	static String IX="Ix";

	static String REM="Rem";
	static String SWI="Swi";

	static String HOM="Hom";
	static String HET="Het";

	static void setDiffs( String dataType, String remSwi, String homHet, Person person, double value ) {
		person.getAttributes().putAttribute( "diffs"+dataType+remSwi+homHet, value );
	}
	static Double getDiffs( String dataType, String remSwi, String homHet, Person person ) {
		return (Double) person.getAttributes().getAttribute( "diffs"+dataType+remSwi+homHet );
	}

}
