package org.matsim.core.mobsim.qsim;

import com.google.inject.Inject;
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
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.facilities.Facility;
import org.matsim.withinday.utils.EditPlans;
import org.matsim.withinday.utils.EditTrips;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.matsim.core.router.TripStructureUtils.*;

final class BookingNotificationEngine implements MobsimEngine {

	// Could implement this as a generalized version of the bdi-abm implementation: can send notifications to agent, and agent can react.  Similar to the drive-to action.
	// Notifications and corresponding handlers could then be registered.

	// On the other hand, it is easy to add an engine such as this one; how much does it help to have another layer of infrastructure?

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
			MobsimAgent agent = entry.getKey();;
			TripInfo tripInfo = entry.getValue();
			Facility pickupLocation = tripInfo.getPickupLocation();
			double pickupTime = tripInfo.getExpectedBoardingTime();
			String mode = tripInfo.getMode();;

			Plan plan = WithinDayAgentUtils.getModifiablePlan( agent );
			Integer currentPlanElementIndex = WithinDayAgentUtils.getCurrentPlanElementIndex( agent );;

			// find next leg with given mode:
//			PlanElement pe = null ;

			int drtLegPEIndex = -1 ;
			for ( int ii = currentPlanElementIndex ; ii < plan.getPlanElements().size() ; ii++ ) {
				PlanElement pe = plan.getPlanElements().get(ii) ;
				if ( pe instanceof Leg && ((Leg) pe).getMode().equals( TransportMode.drt )) {
					drtLegPEIndex = ii ;
					break ;
				}
			}
			// several cases:
			// (1) agent is still at previous activity
			// (2) agent is at previous leg (e.g. in train)
			// (3) agent is already on walk leg towards pickup location but needs to change destination
			// (4) agent is already waiting but now needs to change position

			// I think that the following, once debugged, would address (1) and (2), but not (3)

			// using custom stage activities here, since we only want the drt part of the trip.  Normally we have
			//   access_walk -- drt_interaction -- drt -- drt_interaction -- egress_walk .
			// But we can have network walk, in which case it gets quite complicated
			//   access_walk -- walk_interaction -- (network)walk -- walk_interaction -- egress(?)_walk -- drt_interaction -- drt -- ...
			// since we need to end the initial walk at a facility and thus need to bushwhack to it.
			StageActivityTypes stageActivitiesForWalk = new StageActivityTypesImpl( "walk_interaction" ) ;
			// (only want to replan the walk part!)

			Trip walkTrip = TripStructureUtils.findTripEndingAtActivity( (Activity) plan.getPlanElements().get(drtLegPEIndex-1 ), plan, stageActivitiesForWalk );
			// MATSIM-481 = decision to always alternate acts and legs

			Gbl.assertNotNull( walkTrip );

			int walkTripStartPEIndex = WithinDayAgentUtils.indexOfPlanElement( agent, walkTrip.getOriginActivity() );
			Gbl.assertIf( currentPlanElementIndex <= walkTripStartPEIndex  );
			// otherwise we are case (3) or (4) or completely confused

			String technicalMainModeOfWalkTrip = tripRouter.getMainModeIdentifier().identifyMainMode( walkTrip.getTripElements() );
			double dpTime = walkTrip.getOriginActivity().getEndTime() ; // yyyy will need other estimate of this
			editTrips.replanFutureTrip( walkTrip, plan, technicalMainModeOfWalkTrip, dpTime ) ;

		}

	}

	public synchronized final void notifyChangedTripInformation( MobsimAgent agent, TripInfo tripinfo ) {
		// (we are in the mobsim, so we don't need to play around with IDs)

		map.put( agent, tripinfo ) ;
	}

}
