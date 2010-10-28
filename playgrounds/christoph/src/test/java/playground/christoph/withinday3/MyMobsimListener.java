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
package playground.christoph.withinday3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.core.mobsim.framework.events.SimulationBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.SimulationListener;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.ptproject.qsim.helpers.DefaultPersonDriverAgent;
import org.matsim.ptproject.qsim.interfaces.Mobsim;

import playground.christoph.withinday.utils.EditRoutes;
import playground.christoph.withinday.utils.ReplacePlanElements;

/**
 * @author nagel
 *
 */
public class MyMobsimListener implements SimulationListener, SimulationBeforeSimStepListener {
	
	private PersonalizableTravelCost travCostCalc;
	private PersonalizableTravelTime travTimeCalc;
	private PlansCalcRoute routeAlgo ;
	private Scenario scenario;

	MyMobsimListener ( PersonalizableTravelCost travelCostCalculator, PersonalizableTravelTime travelTimeCalculator ) {
		this.travCostCalc = travelCostCalculator ;
		this.travTimeCalc = travelTimeCalculator ;
	}

	@Override
	public void notifySimulationBeforeSimStep(SimulationBeforeSimStepEvent event) {
		
		Mobsim mobsim = (Mobsim) event.getQueueSimulation() ;
		this.scenario = mobsim.getScenario();

		Collection<PersonAgent> agentsToReplan = getAgentsToReplan( mobsim ) ; 
		
		this.routeAlgo = new PlansCalcRoute(mobsim.getScenario().getConfig().plansCalcRoute(), mobsim.getScenario().getNetwork(), 
				this.travCostCalc, this.travTimeCalc, new DijkstraFactory() );
		
		for ( PersonAgent pa : agentsToReplan ) {
			doReplanning( pa, mobsim.getSimTimer().getTimeOfDay() ) ;
		}
	}
	
	private List<PersonAgent> getAgentsToReplan(Mobsim mobsim ) {

		ArrayList<PersonAgent> list = new ArrayList<PersonAgent>();

		// don't handle the agent, if time != 12 o'clock
		if (mobsim.getSimTimer().getTimeOfDay() != 12 * 3600) {
			return list;
		}

		// select agents, which should be replanned within this time step
		for (PersonAgent agent : mobsim.getActivityEndsList()) {
			if (((PersonImpl) agent.getPerson()).getAge() == 56) {
				System.out.println("found agent");
				list.add(agent);
			}
		}

		return list;
	}

	private boolean doReplanning(PersonAgent personAgent, double now ) {
		
		Person person = personAgent.getPerson();
		Plan selectedPlan = person.getSelectedPlan();

		Leg currentLeg = personAgent.getCurrentLeg();
		Activity nextActivity = ((PlanImpl) selectedPlan).getNextActivity(currentLeg);

		// If it is not a car Leg we don't replan it.
		if (!currentLeg.getMode().equals(TransportMode.car)) return false;
		
		ActivityImpl newWorkAct = new ActivityImpl("w", this.scenario.createId("22"));
		newWorkAct.setDuration(3600);

		// Replace Activity
		new ReplacePlanElements().replaceActivity(selectedPlan, nextActivity, newWorkAct);
		
		/*
		 *  Replan Routes
		 */
		// new Route for current Leg
		new EditRoutes().replanCurrentLegRoute(selectedPlan, currentLeg, ((DefaultPersonDriverAgent)personAgent).getCurrentNodeIndex(), routeAlgo, scenario.getNetwork(), now);
		// ( compiles, but does not run, since agents are (deliberately) not instantiated as withindayreplanningagents.  kai, oct'10 )
		// Adapted code, WithinDayPersonAgents are now only needed if they have to handle WithinDayReplanners. cdobler, oct'10
		
		// new Route for next Leg
		Leg homeLeg = ((PlanImpl) selectedPlan).getNextLeg(newWorkAct);
		new EditRoutes().replanFutureLegRoute(selectedPlan, homeLeg, routeAlgo);
		
		// finally reset the cached Values of the PersonAgent - they may have changed!
		personAgent.resetCaches();
		
		return true;
	}


}
