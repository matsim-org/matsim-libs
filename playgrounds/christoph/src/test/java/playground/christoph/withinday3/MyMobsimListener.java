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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.core.mobsim.framework.PersonDriverAgent;
import org.matsim.core.mobsim.framework.events.SimulationBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.SimulationListener;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.ptproject.qsim.agents.DefaultPersonDriverAgent;
import org.matsim.ptproject.qsim.agents.ExperimentalBasicWithindayAgent;
import org.matsim.ptproject.qsim.interfaces.Mobsim;
import org.matsim.ptproject.qsim.interfaces.NetsimLink;
import org.matsim.ptproject.qsim.qnetsimengine.QVehicle;

import playground.christoph.withinday.utils.EditRoutes;
import playground.christoph.withinday.utils.ReplacePlanElements;

/**
 * @author nagel
 *
 */
public class MyMobsimListener implements SimulationListener, SimulationBeforeSimStepListener {
    private static final Logger log = Logger.getLogger("dummy");

	
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

		List<PersonAgent> set = new ArrayList<PersonAgent>();

		// don't handle the agent, if time != 12 o'clock
		if (Math.floor(mobsim.getSimTimer().getTimeOfDay()) !=  22000.0) {
			return set;
		}
		
		// find agents that are en-route (more interesting case)
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

	private boolean doReplanning(PersonAgent personAgent, double now ) {
		
		// preconditions:

		if ( !(personAgent instanceof ExperimentalBasicWithindayAgent) ) {
			log.error("agent of wrong type; returning ... " ) ;
		}
		ExperimentalBasicWithindayAgent withindayAgent = (ExperimentalBasicWithindayAgent) personAgent ;
		
		Person person = withindayAgent.getPerson();
		Plan selectedPlan = person.getSelectedPlan();

		if (selectedPlan == null) {
			log.info( " we don't have a selected plan; returning ... ") ;
			return false;
		}
		if ( !(withindayAgent.getCurrentPlanElement() instanceof Leg) ) {
			log.info( "agent not on leg; returning ... ") ;
			return false ;
		}
		if (!((Leg)withindayAgent.getCurrentPlanElement()).getMode().equals(TransportMode.car)) {
			log.info( "not a car leg; can only replan car legs; returning ... ") ;
			return false;
		}
		
		List<PlanElement> planElements = withindayAgent.getModifiablePlanElements() ;
		final Integer planElementsIndex = withindayAgent.getCurrentPlanElementIndex() ;
		
		if ( !(planElements.get(planElementsIndex+1) instanceof Activity || !(planElements.get(planElementsIndex+2) instanceof Leg)) ) {
			log.error( "this version of withinday replanning cannot deal with plans where legs and acts to not alternate; returning ...") ;
			return false ;
		}
		Activity nextActivity = (Activity) planElements.get(planElementsIndex+1) ;
		Leg legAfterNextActivity = (Leg) planElements.get(planElementsIndex+2) ;
		
		// now the real work begins:

		ActivityImpl newAct = new ActivityImpl("w", this.scenario.createId("22"));
		newAct.setDuration(3600);
		
		planElements.set( planElementsIndex+1, newAct ) ;
		

		// yyyyyy the following still needs to be improved.  For starters, it is not clear what "currentNodeIndex" is.  kai, oct'10

		/*
		 *  Replan Routes
		 */
		// new Route for current Leg
		EditRoutesKai.replanCurrentLegRoute(person, planElements, planElementsIndex, 
				withindayAgent.getCurrentRouteLinkIdIndex(), routeAlgo, now ) ;

//		new EditRoutes().replanCurrentLegRoute(selectedPlan, currentLeg, ((DefaultPersonDriverAgent)personAgent).getCurrentNodeIndex(), routeAlgo, scenario.getNetwork(), now);
		// ( compiles, but does not run, since agents are (deliberately) not instantiated as withindayreplanningagents.  kai, oct'10 )
		// Adapted code, WithinDayPersonAgents are now only needed if they have to handle WithinDayReplanners. cdobler, oct'10
		
		
		// new Route for next Leg. yyyy should be static and based on the index, but this is not the real problem 
		int homeLegindex = planElementsIndex+2;
//		Leg homeLeg = (Leg) planElements.get( homeLegindex ) ;
		new EditRoutes().replanFutureLegRoute(selectedPlan, homeLegindex, routeAlgo);
		
		// finally reset the cached Values of the PersonAgent - they may have changed!
		withindayAgent.resetCaches();
		
		return true;
	}


}
