/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.core.mobsim.qsim;

import com.google.inject.Inject;
import com.sun.tools.javac.util.Pair;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.agents.HasModifiablePlan;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.interfaces.TripInfoRequest;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;

import java.util.*;

import static org.matsim.core.config.groups.PlanCalcScoreConfigGroup.createStageActivityType;

public class DrtTaxiPrebookingWakeupGenerator {
	// yyyyyy I would very much rather have this as passenger agent behavior, not as drt behavior.  Maybe the drt forces me to prebook until xyz before pickup, but it is the
	// decision of the passenger when he/she actually executes the prebooking.  Maybe there are also different prices associated with different prebooking times, then this
	// becomes even clearer.  kai, mar'19

	private static final Logger log = Logger.getLogger(DrtTaxiPrebookingWakeupGenerator.class);

	public static final String PREBOOKING_OFFSET_ATTRIBUTE_NAME = "prebookingOffset_s";

	public static final StageActivityTypes drtStageActivities = new StageActivityTypesImpl(
			createStageActivityType(TransportMode.drt), createStageActivityType(TransportMode.walk));

	private final ActivityFacilities facilities;
	private final BookingEngine bookingEngine;

	@Inject
	public DrtTaxiPrebookingWakeupGenerator(Scenario scenario, BookingEngine bookingEngine) {
		this.facilities = scenario.getActivityFacilities();
		this.bookingEngine = bookingEngine;
	}

	public Map<Double, ActivityEngineWithWakeup.AgentWakeup> generateWakeups( MobsimAgent agent ) {
		if (!(agent instanceof HasModifiablePlan)) {
			// (we don't want to treat DvrpAgents, CarrierAgents, TransitVehicleDrivers etc. here)
			return Collections.emptyMap();
		}

		Map<Double, ActivityEngineWithWakeup.AgentWakeup> wakeups = new TreeMap<>(  ) ;

		Double prebookingOffset_s = (Double)((PlanAgent)agent).getCurrentPlan()
				.getAttributes()
				.getAttribute(PREBOOKING_OFFSET_ATTRIBUTE_NAME);

		if ( prebookingOffset_s == null ) {
			log.warn("not prebooking") ;
			return wakeups ;
		}

		for (Leg drtLeg : findLegsWithModeInFuture(agent, TransportMode.drt)) {
			//				Double prebookingOffset_s = (Double)drtLeg.getAttributes().getAttribute( PREBOOKING_OFFSET_ATTRIBUTE_NAME );
//			if (prebookingOffset_s == null) {
//				log.warn("not prebooking");
//				continue;
//			}
			final double prebookingTime = drtLeg.getDepartureTime() - prebookingOffset_s;
			if (prebookingTime < agent.getActivityEndTime()){
				// yyyy and here one sees that having this in the activity engine is not very practical
				log.info( "adding agent to wakeup list" );
				wakeups.put( prebookingTime, new ActivityEngineWithWakeup.AgentWakeup(){
					@Override
					public void wakeUp( MobsimAgent agent, double now ){
						wakeUpAgent( agent, now, drtLeg );
					}
				} );
			}
		}
		for (Leg drtLeg : findLegsWithModeInFuture(agent, TransportMode.taxi)) {
			//				Double prebookingOffset_s = (Double)drtLeg.getAttributes().getAttribute( PREBOOKING_OFFSET_ATTRIBUTE_NAME );
//			if (prebookingOffset_s == null) {
//				log.warn("not prebooking");
//				continue;
//			}
			final double prebookingTime = drtLeg.getDepartureTime() - prebookingOffset_s;
			if (prebookingTime < agent.getActivityEndTime()) {
				// yyyy and here one sees that having this in the activity engine is not very practical
				log.info("adding agent to wakeup list");
				wakeups.put( prebookingTime, new ActivityEngineWithWakeup.AgentWakeup(){
					@Override
					public void wakeUp( MobsimAgent agent, double now ){
						wakeUpAgent( agent, now, drtLeg );
					}
				} );
			}
		}
		return wakeups;
	}

	public static List<Leg> findLegsWithModeInFuture(MobsimAgent agent, String mode) {
		List<Leg> retVal = new ArrayList<>();
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent);
		for (int ii = WithinDayAgentUtils.getCurrentPlanElementIndex(agent); ii < plan.getPlanElements().size(); ii++) {
			PlanElement pe = plan.getPlanElements().get(ii);
			if (pe instanceof Leg) {
				if (Objects.equals(mode, ((Leg)pe).getMode())) {
					retVal.add((Leg)pe);
				}
			}
		}
		return retVal;
	}

	private void wakeUpAgent(MobsimAgent agent, double now, Leg leg) {
		//		log.warn("entering wakeUpAgent with agentId=" + agent.getId() ) ;

		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent);

		// search for drt trip corresponding to drt leg.  Trick is using our own stage activities.
		TripStructureUtils.Trip drtTrip = TripStructureUtils.findTripAtPlanElement(leg, plan, this.drtStageActivities);
		Gbl.assertNotNull(drtTrip);

		Facility fromFacility = FacilitiesUtils.toFacility(drtTrip.getOriginActivity(), facilities);
		Facility toFacility = FacilitiesUtils.toFacility(drtTrip.getDestinationActivity(), facilities);

		final TripInfoRequest request = new TripInfoRequest.Builder().setFromFacility(fromFacility)
				.setToFacility(toFacility)
				.setTime(drtTrip.getOriginActivity().getEndTime())
				.createRequest();

		//first simulate ActivityEngineWithWakeup and then BookingEngine --> decision process
		//in the same time step
		bookingEngine.notifyTripInfoRequestSent(agent, request);
	}
}
