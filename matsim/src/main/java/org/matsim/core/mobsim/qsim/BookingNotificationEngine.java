package org.matsim.core.mobsim.qsim;

import static org.matsim.core.router.TripStructureUtils.Trip;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.interfaces.TripInfo;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.withinday.utils.EditPlans;
import org.matsim.withinday.utils.EditTrips;

import com.google.inject.Inject;

final class BookingNotificationEngine implements MobsimEngine {

	// Could implement this as a generalized version of the bdi-abm implementation: can send notifications to agent, and agent can react.  Similar to the drive-to action.
	// Notifications and corresponding handlers could then be registered. On the other hand, it is easy to add an engine such as this one; how much does it help to have another
	// layer of infrastructure?  Am currently leaning towards the second argument.  kai, mar'19

	private InternalInterface internalInterface;

	private Map< MobsimAgent, TripInfo > map = new ConcurrentHashMap<>(  ) ;
	// yyyy not sure about possible race conditions here! kai, feb'19

	private EditTrips editTrips = null ;
	private EditPlans editPlans = null ;

	private final TripRouter tripRouter;

	@Inject
	BookingNotificationEngine( TripRouter tripRouter, Scenario scenario ) {
		this.tripRouter = tripRouter ;
		this.editTrips = new EditTrips( tripRouter, scenario ) ;
	}

	@Override
	public void onPrepareSim(){
	}

	@Override
	public void afterSim(){
		throw new RuntimeException( "not implemented" );
	}

	@Override
	public void setInternalInterface( InternalInterface internalInterface ){
		this.internalInterface = internalInterface ;
		this.editPlans = new EditPlans( internalInterface.getMobsim(), tripRouter, editTrips ) ;
	}

	@Override
	public void doSimStep( double time ){
		for( Map.Entry<MobsimAgent, TripInfo> entry : map.entrySet() ){
			MobsimAgent agent = entry.getKey();
			TripInfo tripInfo = entry.getValue();

			Gbl.assertIf( WithinDayAgentUtils.getCurrentPlanElement( agent ) instanceof  Activity );

			Plan plan = WithinDayAgentUtils.getModifiablePlan( agent ) ;

			Trip theTrip = null ;
			for ( Trip drtTrip : TripStructureUtils.getTrips( plan, ActivityEngineWithWakeup.drtStageActivities ) ) {
				// recall that we have set the activity end time of the current activity to infinity, so we cannot use that any more.  :-( ?!
				// could instead use some kind of ID.  Not sure if that would really be better.
				if ( CoordUtils.calcEuclideanDistance( drtTrip.getOriginActivity().getCoord(), tripInfo.getPickupLocation().getCoord() ) > 1000. ) {
					continue ;
				}
				if ( CoordUtils.calcEuclideanDistance( drtTrip.getDestinationActivity().getCoord(), tripInfo.getDropoffLocation().getCoord() ) > 1000.  ) {
					continue ;
				}
				theTrip = drtTrip ;
				break ;
			}
			Gbl.assertNotNull( theTrip );
			Iterator<Trip> walkTripsIter = TripStructureUtils.getTrips( theTrip.getTripElements(), new StageActivityTypesImpl( TransportMode.walk ) ).iterator();

			// ---

			Gbl.assertIf( walkTripsIter.hasNext() );
			Trip accessWalkTrip = walkTripsIter.next();
			accessWalkTrip.getDestinationActivity().setCoord( tripInfo.getPickupLocation().getCoord() );
			accessWalkTrip.getDestinationActivity().setLinkId( tripInfo.getPickupLocation().getLinkId()  );
			accessWalkTrip.getDestinationActivity().setFacilityId( null );

			List<? extends PlanElement> pe = editTrips.replanFutureTrip( accessWalkTrip, plan, TransportMode.walk );
			List<Leg> legs = TripStructureUtils.getLegs( pe );
			double ttime = 0.;
			for( Leg leg : legs ) {
				if ( leg.getRoute() != null ) {
					ttime += leg.getTravelTime() ;
				} else {
					ttime += leg.getTravelTime() ;
				}
			}
			double buffer = 300. ;
			editPlans.rescheduleCurrentActivityEndtime( agent, tripInfo.getExpectedBoardingTime()-ttime-buffer );

			// ---

			Gbl.assertIf( walkTripsIter.hasNext() );
			Trip egressWalkTrip = walkTripsIter.next() ;
			final Activity egressWalkOriginActivity = egressWalkTrip.getOriginActivity() ;
			egressWalkOriginActivity.setCoord( tripInfo.getDropoffLocation().getCoord() );
			egressWalkOriginActivity.setLinkId( tripInfo.getDropoffLocation().getLinkId() );
			egressWalkOriginActivity.setFacilityId( null );

			editTrips.replanFutureTrip( egressWalkTrip, plan, TransportMode.walk ) ;
			// yy maybe better do this at dropoff?

			// ----

			Gbl.assertIf( ! walkTripsIter.hasNext() );

		}

	}

	public synchronized final void notifyChangedTripInformation( MobsimAgent agent, TripInfo tripinfo ) {
		// (we are in the mobsim, so we don't need to play around with IDs)

		map.put( agent, tripinfo ) ;
	}

}
