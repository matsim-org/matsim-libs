/* *********************************************************************** *
 * project: org.matsim.*
 * MyMobsimListener.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.taxicab;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.core.mobsim.framework.PersonDriverAgent;
import org.matsim.core.mobsim.framework.events.SimulationBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.SimulationListener;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.pt.qsim.PassengerAgent;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.agents.ExperimentalBasicWithindayAgent;
import org.matsim.ptproject.qsim.interfaces.Netsim;
import org.matsim.ptproject.qsim.interfaces.NetsimLink;
import org.matsim.ptproject.qsim.qnetsimengine.QVehicle;

/**As stated in the package info, this class is an <i>untested</i> design suggestion.  Comments are welcome.  kai, dec'10
 *
 * @author nagel
 */
public class MyWithinDayMobsimListener implements SimulationListener, SimulationBeforeSimStepListener {
    private static final Logger log = Logger.getLogger(MyWithinDayMobsimListener.class);


	private PersonalizableTravelCost travCostCalc;
	private PersonalizableTravelTime travTimeCalc;
	private PlansCalcRoute routeAlgo ;
	private Scenario scenario;

	MyWithinDayMobsimListener ( PersonalizableTravelCost travelCostCalculator, PersonalizableTravelTime travelTimeCalculator ) {
		this.travCostCalc = travelCostCalculator ;
		this.travTimeCalc = travelTimeCalculator ;
	}

	@Override
	public void notifySimulationBeforeSimStep(SimulationBeforeSimStepEvent event) {

		Netsim mobsim = (Netsim) event.getQueueSimulation() ;
		this.scenario = mobsim.getScenario();

		Collection<PersonAgent> agentsToReplan = getAgentsToReplan( mobsim ) ;

		this.routeAlgo = new PlansCalcRoute(mobsim.getScenario().getConfig().plansCalcRoute(), mobsim.getScenario().getNetwork(),
				this.travCostCalc, this.travTimeCalc, new DijkstraFactory() );

		for ( PersonAgent pa : agentsToReplan ) {
			doReplanning( pa, mobsim ) ;
		}
	}

	private List<PersonAgent> getAgentsToReplan(Netsim mobsim ) {

		List<PersonAgent> set = new ArrayList<PersonAgent>();

		// somehow find the agents (this is just an example):
		for (NetsimLink link:mobsim.getNetsimNetwork().getNetsimLinks().values()){
			for (QVehicle vehicle : link.getAllNonParkedVehicles()) {
				PersonDriverAgent agent=vehicle.getDriver();
				System.out.println(agent.getPerson().getId());
				if (((PersonImpl) agent.getPerson()).getAge() == 18) {
					System.out.println("found agent");
					set.add(agent);
				}
			}
		}

		return set;

	}

	private boolean doReplanning(PersonAgent personAgent, Netsim mobsim ) {

		// preconditions:

		if ( !(personAgent instanceof ExperimentalBasicWithindayAgent) ) {
			log.error("agent of wrong type; returning ... " ) ;
		}
		ExperimentalBasicWithindayAgent withindayAgent = (ExperimentalBasicWithindayAgent) personAgent ;

		Plan plan = withindayAgent.getModifiablePlan() ;

		if (plan == null) {
			log.info( " we don't have a selected plan; returning ... ") ;
			return false;
		}

		// =============================================================================================================
		// =============================================================================================================
		// since this is a use case, let us enumerate relevant data structure operations:

		if ( withindayAgent.getCurrentPlanElement() instanceof Activity ) {

			// (I) @ activity:
			double oldTime = -1. ;
			double newTime = -1. ;
			mobsim.rescheduleActivityEnd(withindayAgent, oldTime, newTime) ;
			// might be nice to be able to actively remove the agent from the activity, but this is strictly
			// speaking not necessary since the departure can be scheduled for immediately.  kai, nov'10

		} else if ( withindayAgent.getCurrentPlanElement() instanceof Leg ) {
			Leg leg = (Leg) withindayAgent.getCurrentPlanElement() ;

			if ( TransportMode.car.equals( leg.getMode() ) ) {

				// (II) car leg

				// (a) inside vehicle and waiting to get inside traffic
				// at this point, there is no replanning implemented for this situation.  kai, nov'10

				// (b) on link and changing arrival to arrival on current link
				// I think you do this by changing the destinationLinkId in the driver and then reset the caches.
				// But I am not sure.  kai, nov'10

				// (c) on link and changing next link
				// re-program chooseNextLinkId() ;

			} else if ( TransportMode.pt.equals( leg.getMode() )) {

				// (III) pt leg

				// (a) waiting for pt and changing the desired line:
				// can be done via reprogramming getEnterTransitRoute() ;

				// (b) aborting a wait for pt:
				TransitStopFacility stop = null ;
				((QSim)mobsim).getTransitEngine().getAgentTracker().removeAgentFromStop((PassengerAgent)withindayAgent, stop.getId());
				// after this, it needs to start something else, e.g.:
				mobsim.scheduleActivityEnd(withindayAgent) ;
				// or
				mobsim.arrangeAgentDeparture(withindayAgent) ;

				// (c) while inside vehicle and changing the desired stop to get off:
				// can be done via reprogramming getExitAtStop() ;
			}

		}

		// finally reset the cached Values of the PersonAgent - they may have changed!
		withindayAgent.resetCaches();

		return true;
	}


}
