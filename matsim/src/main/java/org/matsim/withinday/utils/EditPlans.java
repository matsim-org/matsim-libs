package org.matsim.withinday.utils;

import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;

public final class EditPlans {
	private static final Logger log = Logger.getLogger( EditPlans.class ) ;
	
	private final TripRouter tripRouter ;
	private final QSim mobsim;
	private final EditTrips editTrips;
	private final PopulationFactory pf;
	public EditPlans( QSim mobsim, TripRouter tripRouter, EditTrips editTrips, PopulationFactory pf ) {
//		log.setLevel(Level.DEBUG);
		Gbl.assertNotNull( this.mobsim = mobsim );
		Gbl.assertNotNull( this.tripRouter = tripRouter );
		Gbl.assertNotNull( this.editTrips = editTrips ) ;
		Gbl.assertNotNull( this.pf = pf ) ;
	}
	public boolean addActivityAtEnd(MobsimAgent agent, Activity activity, String routingMode) {
		log.debug("entering addActivityAtEnd with routingMode=" + routingMode) ;

		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent);
		List<PlanElement> planElements = plan.getPlanElements();
		
		boolean retVal1 = false;
		
		if (isAtRealActivity(agent)) {
			retVal1 = planElements.add(pf.createLeg(routingMode));
		}
		
		final boolean retVal = planElements.add(activity);
		// (need the terminating activity in order to find the current trip. kai, nov'17)
		
		if (!isAtRealActivity(agent)) {
			retVal1 = editTrips.replanCurrentTrip(agent,mobsim.getSimTimer().getTimeOfDay(),routingMode);
		}
		
		
		WithinDayAgentUtils.resetCaches(agent);
		this.mobsim.rescheduleActivityEnd(agent);
		return (retVal1 && retVal);
	}
	public PlanElement removeActivity(MobsimAgent agent, int index, String mode) {
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent) ;
		List<PlanElement> planElements = plan.getPlanElements() ;

		checkIfNotInPastOrCurrent(agent, index);

		final Trip tripBefore = TripStructureUtils.findTripEndingAtActivity( (Activity) planElements.get(index),plan,tripRouter.getStageActivityTypes() );
		final Trip tripAfter = TripStructureUtils.findTripStartingAtActivity( (Activity)planElements.get(index),plan,tripRouter.getStageActivityTypes() );
		if ( mode==null ) {
			final String mainModeBefore = tripRouter.getMainModeIdentifier().identifyMainMode( tripBefore.getTripElements() );
			final String mainModeAfter = tripRouter.getMainModeIdentifier().identifyMainMode( tripAfter.getTripElements() );
			if ( mainModeBefore.equals( mainModeAfter ) ) {
				mode = mainModeBefore ;
			} else {
				throw new ReplanningException("mode not given and mode before removed activity != mode after removed activity; don't know which mode to use") ;
			}
		}
		PlanElement pe = planElements.remove(index) ; 
		if ( checkIfTripHasAlreadyStarted( agent, tripBefore.getTripElements() ) ) {
			editTrips.replanCurrentTrip(agent, mobsim.getSimTimer().getTimeOfDay() , mode);
		} else {
			editTrips.insertEmptyTrip(plan, tripBefore.getOriginActivity(), tripAfter.getDestinationActivity(), mode ) ;
		}
		WithinDayAgentUtils.resetCaches(agent);
		this.mobsim.rescheduleActivityEnd(agent);
		return pe ;
	}
	public final void rescheduleActivityEndtime( MobsimAgent agent, int index, double newEndTime ) {
		Activity activity = (Activity) WithinDayAgentUtils.getModifiablePlan(agent).getPlanElements().get(index) ;
		activity.setEndTime(newEndTime);
		WithinDayAgentUtils.resetCaches(agent);
		WithinDayAgentUtils.rescheduleActivityEnd(agent, mobsim);
	}
	public final Activity replaceActivity(MobsimAgent agent, int index, Activity newAct, String upstreamMode, String downstreamMode ) {
		System.err.println("here310");
		printPlan(agent) ;
		System.err.println("here320");
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent) ;
		printPlan(plan) ;
		System.err.println("here330");
		
		
		List<PlanElement> planElements = plan.getPlanElements() ;

		// make sure we have indeed an activity position:
		if ( ! ( planElements.get(index) instanceof Activity ) ) {
			throw new ReplanningException("trying to replace a non-activity in the plan by an activity; this is not possible") ;
		}
		Activity origAct = (Activity) planElements.get(index) ;

		checkIfNotStageActivity(origAct);

		checkIfNotInPastOrCurrent(agent, index);

		// set the new activity:
		planElements.set(index, newAct) ;
		System.err.println("here340");
		printPlan(plan) ;

		// trip before (if any):
		if ( index > 0 ) {
			Trip tripBeforeAct = TripStructureUtils.findTripEndingAtActivity(newAct,plan,tripRouter.getStageActivityTypes());
			Gbl.assertNotNull( tripBeforeAct );  // there could also just be a sequence of activities?!

			final List<PlanElement> currentTripElements = tripBeforeAct.getTripElements();
			final String currentMode = this.tripRouter.getMainModeIdentifier().identifyMainMode( currentTripElements ) ;

			if ( checkIfTripHasAlreadyStarted(agent, currentTripElements) ) {
				// trip has already started
				checkIfSameMode(upstreamMode, currentMode);
				this.editTrips.replanCurrentTrip(agent, this.mobsim.getSimTimer().getTimeOfDay(), currentMode );
			} else {
				// trip has not yet started
				if ( upstreamMode == null ) {
					upstreamMode = currentMode ;
				}
				this.editTrips.insertEmptyTrip( plan, tripBeforeAct.getOriginActivity(), newAct, upstreamMode );
			}
		}
		// trip after (if any):
		if ( index < planElements.size()-1 ) {
			Trip tripAfterAct = TripStructureUtils.findTripStartingAtActivity(origAct,plan,tripRouter.getStageActivityTypes());
			Gbl.assertIf( tripAfterAct!=null ); // there could also just be a sequence of activities?!
			if ( downstreamMode==null ) {
				final String currentMainMode = this.tripRouter.getMainModeIdentifier().identifyMainMode( tripAfterAct.getTripElements() );
				EditTrips.insertEmptyTrip(plan, newAct, tripAfterAct.getDestinationActivity(), currentMainMode, pf);
			} else {
				EditTrips.insertEmptyTrip(plan, newAct, tripAfterAct.getDestinationActivity(), downstreamMode, pf);
			}
		}
		WithinDayAgentUtils.resetCaches(agent);
		this.mobsim.rescheduleActivityEnd(agent);
		return origAct ;
	}
	public void insertActivity(MobsimAgent agent, int index, Activity activity, String upstreamMode, String downstreamMode ) {
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent) ;
		List<PlanElement> planElements = plan.getPlanElements() ;

		checkIfNotInPastOrCurrent(agent, index) ;
		planElements.add( index, activity ) ;
		{
			// activity before:
			Activity actBefore = findRealActBefore(agent, index);
			if ( actBefore != null ) {
				if ( EditPlans.indexOfPlanElement(agent, actBefore) < WithinDayAgentUtils.getCurrentPlanElementIndex(agent) ) {
					// we are already under way
					editTrips.replanCurrentTrip(agent, this.mobsim.getSimTimer().getTimeOfDay(), upstreamMode );
				} else {
					// we are not yet under way; inserting empty trip:
					EditTrips.insertEmptyTrip(plan, actBefore, activity, upstreamMode, pf ) ;
				}
			}
		}
		{
			// activity after:
			Activity actAfter = findRealActAfter(agent, index);
			if ( actAfter != null ) {
				EditTrips.insertEmptyTrip(plan, activity, actAfter, downstreamMode, pf ) ;
			}
		}			
		WithinDayAgentUtils.resetCaches(agent);
		this.mobsim.rescheduleActivityEnd(agent);
	}

	// === search methods: ===
	public static int indexOfPlanElement(MobsimAgent agent, PlanElement pe) {
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent) ;
		List<PlanElement> planElements = plan.getPlanElements() ;

		return planElements.indexOf(pe) ;
	}
	public static int indexOfNextActivityWithType( MobsimAgent agent, String type ) {
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent) ;
		List<PlanElement> planElements = plan.getPlanElements() ;

		for ( int index = WithinDayAgentUtils.getCurrentPlanElementIndex(agent) ; index < planElements.size() ; index++ ) {
			PlanElement pe = planElements.get(index) ;
			if ( pe instanceof Activity ) {
				if ( ((Activity)pe).getType().equals(type) ) {
					return index ;
				}
			}
		}
		return -1 ;
	}
	public static Activity findNextActivityWithType( MobsimAgent agent, String type ) {
		int index = indexOfNextActivityWithType( agent, type ) ;
		return (Activity) WithinDayAgentUtils.getModifiablePlan(agent).getPlanElements().get(index) ;
	}
	public static List<PlanElement> subList(MobsimAgent agent, int fromIndex, int toIndex) {
		return WithinDayAgentUtils.getModifiablePlan(agent).getPlanElements().subList( fromIndex, toIndex ) ;
	}

	// === convenience methods: ===
	/** 
	 * Convenience method, clarifying that this can be called without giving the mode.
	 */
	public PlanElement removeActivity(MobsimAgent agent, int index) {
		return removeActivity( agent, index, null ) ;
	}
	/**
	 * Convenience method, clarifying that this can be called without giving the mode.
	 */
	public final Activity replaceActivity(MobsimAgent agent, int index, Activity newAct) {
		return replaceActivity( agent, index, newAct, null, null ) ;
	}
	/**
	 * Convenience method, clarifying that this can be called without giving the mode.
	 */
	public void insertActivity(MobsimAgent agent, int index, Activity activity ) {
		String mode = tripRouter.getMainModeIdentifier().identifyMainMode( editTrips.findCurrentTrip(agent).getTripElements() ) ;
		insertActivity( agent, index, activity, mode, mode ) ;
	}

	// === internal utility methods: ===
	private void checkIfNotStageActivity(Activity origAct) {
		if( this.tripRouter.getStageActivityTypes().isStageActivity(origAct.getType()) ){
			throw new ReplanningException("trying to replace a helper activity (stage activity) by a real activity; this is not possible") ;
		}
	}
	private static boolean checkIfTripHasAlreadyStarted(MobsimAgent agent, final List<PlanElement> currentTripElements) {
		return currentTripElements.contains( ((PlanAgent)agent).getCurrentPlanElement() ) ;
	}
	private static void checkIfNotInPastOrCurrent(MobsimAgent agent, int index) {
		final Integer currentIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);

		// make sure we are not yet beyond the to-be-replanned activity:
		if ( index <= currentIndex ) {
			throw new ReplanningException("trying to replace an activity that lies in the past or is current; this is not possible") ;
		}
	}
	private static void checkIfSameMode(String upstreamMode, final String currentMode) {
		if ( upstreamMode!=null && !upstreamMode.equals(currentMode) ) {
			throw new ReplanningException( "cannot change mode in trip that has already started.  Don't set the mode in the request, "
					+ "or somehow make the agent to abort the current trip first." ) ;
		}
	}
	//	private static boolean locationsAreSame(Activity origAct, Activity newAct) {
	//		if ( origAct.getFacilityId() != null && newAct.getFacilityId() != null ) {
	//			return origAct.getFacilityId().equals( newAct.getFacilityId() ) ;
	//		}
	//		if ( origAct.getCoord() != null && newAct.getCoord() != null ) {
	//			return origAct.getCoord().equals( newAct.getCoord() ) ;
	//		}
	//		return false ;
	//	}
	public Activity findRealActAfter(MobsimAgent agent, int index) {
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent) ;
		List<PlanElement> planElements = plan.getPlanElements() ;
		return (Activity) planElements.get( findIndexOfRealActAfter(agent, index) ) ; 
	}
	public int findIndexOfRealActAfter(MobsimAgent agent, int index) {
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent) ;
		List<PlanElement> planElements = plan.getPlanElements() ;

		int theIndex = -1 ;
		for ( int ii=planElements.size()-1 ; ii>index; ii-- ) {
			if ( planElements.get(ii) instanceof Activity ) {
				Activity act = (Activity) planElements.get(ii) ;
				if ( !this.tripRouter.getStageActivityTypes().isStageActivity( act.getType() ) ) {
					theIndex = ii ;
				}
			}
		}
		return theIndex ;
	}
	public Activity findRealActBefore(MobsimAgent agent, int index) {
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent) ;
		List<PlanElement> planElements = plan.getPlanElements() ;

		Activity prevAct = null ;
		for ( int ii=0 ; ii<index ; ii++ ) {
			if ( planElements.get(ii) instanceof Activity ) {
				Activity act = (Activity) planElements.get(ii) ;
				if ( !this.tripRouter.getStageActivityTypes().isStageActivity( act.getType() ) ) {
					prevAct = act ;
				}
			}
		}
		return prevAct;
	}
	/**
	 * Only the PlanElements are changed - further Steps
	 * like updating the Routes of the previous and next Leg
	 * have to be done elsewhere.
	 * 
	 * @author cdobler
	 */
	public static boolean replaceLegBlindly(Plan plan, Leg oldLeg, Leg newLeg) {

		if (plan == null) return false;
		if (oldLeg == null) return false;
		if (newLeg == null) return false;

		int index = plan.getPlanElements().indexOf(oldLeg);
		// yyyy I can't say how safe this is.  There is no guarantee that the same entry is not used twice in the plan.  This will in
		// particular be a problem if we override the "equals" contract, in the sense that two legs are equal if
		// certain (or all) elements are equal.  kai, oct'10

		if (index == -1) return false;

		plan.getPlanElements().remove(index);
		plan.getPlanElements().add(index,newLeg);

		return true;
	}
	//	private Facility<?> asFacility(Activity activity) {
	//		Facility<?> hereFacility = new ActivityWrapperFacility( activity ) ;
	//		if ( activity.getFacilityId()!=null ) {
	//			ActivityFacility facility = this.mobsim.getScenario().getActivityFacilities().getFacilities().get( activity.getFacilityId() ) ;
	//			if ( facility != null ) {
	//				hereFacility = facility ;
	//			}
	//		}
	//		return hereFacility;
	//	}
	/**
	 * Only the PlanElements are changed - further Steps
	 * like updating the Routes of the previous and next Leg
	 * have to be done elsewhere.
	 * 
	 * @author cdobler
	 */
	public static boolean replaceActivityBlindly(Plan plan, Activity oldActivity, Activity newActivity) {

		if (plan == null) return false;
		if (oldActivity == null) return false;
		if (newActivity == null) return false;

		int index = plan.getPlanElements().indexOf(oldActivity);
		// yyyy I can't say how safe this is.  There is no guarantee that the same entry is not used twice in the plan.  This will in
		// particular be a problem if we override the "equals" contract, in the sense that two activities are equal if
		// certain (or all) elements are equal.  kai, oct'10

		if (index == -1) return false;

		//		/*
		//		 *  If the new Activity takes place on a different Link
		//		 *  we have to replan the Routes from an to that Activity.
		//		 */
		//		if (oldActivity.getLinkId() != newActivity.getLinkId())
		//		{
		//			
		//		}

		plan.getPlanElements().remove(index);
		plan.getPlanElements().add(index, newActivity);

		return true;
	}
	public void rescheduleActivityEnd(MobsimAgent agent) {
		// this is mostly for retrofitting existing code.  but maybe also useful by itself
		this.mobsim.rescheduleActivityEnd(agent);
	}
	public boolean isAtRealActivity(MobsimAgent agent) {
		PlanElement pe = WithinDayAgentUtils.getCurrentPlanElement(agent) ;
		if ( isRealActivity(pe) ) {
			return true ;
		} else {
			return false ;
		}
	}
	public boolean isRealActivity(PlanElement pe) {
		return pe instanceof Activity && ! ( tripRouter.getStageActivityTypes().isStageActivity( ((Activity)pe).getType() ) );
	}
	public static Plan printPlan(MobsimAgent agent1) {
		final Plan plan = WithinDayAgentUtils.getModifiablePlan(agent1);
		return printPlan(plan) ;
	}
	public static Plan printPlan(Plan plan) {
		System.err.println( "plan=" + plan );
		for ( int ii=0 ; ii<plan.getPlanElements().size() ; ii++ ) {
			System.err.println( "\t" + ii + ":\t" + plan.getPlanElements().get(ii) );
		}
		return plan;
	}
	public String getModeOfCurrentOrNextTrip(MobsimAgent agent) {
		Trip trip ;
		if ( isAtRealActivity( agent ) ) {
			Activity activity = (Activity) WithinDayAgentUtils.getCurrentPlanElement(agent) ;
			trip = editTrips.findTripAfterActivity(WithinDayAgentUtils.getModifiablePlan(agent), activity) ;
		} else {
			trip = editTrips.findCurrentTrip(agent) ;
		}
		return tripRouter.getMainModeIdentifier().identifyMainMode(trip.getTripElements()) ;
	}
	public void flushEverythingBeyondCurrent(MobsimAgent agent) {
		List<PlanElement> pes = WithinDayAgentUtils.getModifiablePlan(agent).getPlanElements() ;
		Integer index = WithinDayAgentUtils.getCurrentPlanElementIndex(agent) ;
		for ( int ii=pes.size()-1 ; ii>index ; ii-- ) {
			pes.remove(ii) ;
		}
	}
	public void rescheduleCurrentActivityEndtime(MobsimAgent agent, double newEndTime) {
		Integer index = WithinDayAgentUtils.getCurrentPlanElementIndex(agent) ;
		this.rescheduleActivityEndtime(agent, index, newEndTime);
	}
	
	public Activity createFinalActivity(String type, Id<Link> newLinkId) {
		Activity newAct = this.pf.createActivityFromLinkId(type, newLinkId);;
		newAct.setEndTime( Double.POSITIVE_INFINITY ) ;
		return newAct ;
	}
	public Activity createAgentThatKeepsMatsimAlive(String type, Id<Link> newLinkId) {
		Activity newAct = this.pf.createActivityFromLinkId( type, newLinkId);;
		newAct.setEndTime( Double.MAX_VALUE ) ;
		return newAct ;
	}
}