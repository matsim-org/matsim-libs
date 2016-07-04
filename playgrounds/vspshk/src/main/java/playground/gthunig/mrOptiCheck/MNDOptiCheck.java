package playground.gthunig.mrOptiCheck;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.pt.PtConstants;
import org.matsim.pt.router.FakeFacility;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterImpl;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import playground.gthunig.utils.StopWatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @author gthunig on 04.07.2016.
 */
public class MNDOptiCheck {
	private static final int N_TRIES = 1000;

	public static void main( final String... args ) {
		final Config config = ConfigUtils.loadConfig( args[ 0 ] );
		final Scenario sc = ScenarioUtils.loadScenario( config );

		final TransitRouterImpl transitRouter = new TransitRouterImpl( new TransitRouterConfig( config ) , sc.getTransitSchedule() );

		final StopWatch stopWatch = new StopWatch();

		if ( config.plans().getInputFile() == null ) {
			measureForRandomODs(sc, transitRouter, stopWatch );
		}
		else {
			measureForPopulation(sc, transitRouter, stopWatch );
		}

		System.out.println(stopWatch.getStoppedTime());
	}

	private static void measureForPopulation(
			final Scenario sc,
			final TransitRouterImpl reference,
			final StopWatch stopWatch ) {
		final StageActivityTypes stages = new StageActivityTypesImpl( PtConstants.TRANSIT_ACTIVITY_TYPE );
		final List<TripStructureUtils.Trip> trips = new ArrayList<>();

		final Counter planCounter = new Counter( "extract trips from plan # " );
		for ( Person person : sc.getPopulation().getPersons().values() ) {
			if ( person.getPlans().isEmpty() ) {
				continue;
			}
			planCounter.incCounter();
			trips.addAll(
					TripStructureUtils.getTrips(
							person.getSelectedPlan(),
							stages ) );
		}

		final Counter counter = new Counter( "Compute route for random trip # " );
		final Random random = new Random( 20151217 );
		for ( int i = 0; i < N_TRIES; i++ ) {
			final TripStructureUtils.Trip trip = trips.get( random.nextInt( trips.size() ) );

			counter.incCounter();

			stopWatch.reset();
			reference.calcRoute( new FakeFacility(trip.getOriginActivity().getCoord()), new FakeFacility(trip.getDestinationActivity().getCoord()), trip.getOriginActivity().getEndTime(), null );
			stopWatch.stop();
		}
		counter.printCounter();
	}

	private static void measureForRandomODs( Scenario sc,
											 TransitRouterImpl reference,
											 StopWatch stopWatch ) {
		final List<Id<TransitStopFacility>> facilityIds = new ArrayList<>( sc.getTransitSchedule().getFacilities().keySet() );
		Collections.sort( facilityIds );

		final Random random = new Random( 20151210 );
		final Counter counter = new Counter( "Compute route for random case # " );
		for ( int i = 0; i < N_TRIES; i++ ) {
			final TransitStopFacility orign =
					sc.getTransitSchedule().getFacilities().get(
							facilityIds.get(
									random.nextInt( facilityIds.size() ) ) );

			final TransitStopFacility destination =
					sc.getTransitSchedule().getFacilities().get(
							facilityIds.get(
									random.nextInt( facilityIds.size() ) ) );

			final double time = random.nextDouble() * 24 * 3600;

			counter.incCounter();

			stopWatch.reset();
			reference.calcRoute( new FakeFacility(orign.getCoord()), new FakeFacility(destination.getCoord()), time, null );
			stopWatch.stop();
		}
		counter.printCounter();
	}
}
