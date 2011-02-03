/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.mrieser.core.mobsim.transit;

import java.util.Iterator;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.pt.Umlauf;
import org.matsim.pt.UmlaufStueckI;
import org.matsim.pt.qsim.TransitStopAgentTracker;
import org.matsim.vehicles.Vehicle;

import playground.mrieser.core.mobsim.api.PlanAgent;

/*package*/ class TransitDriverPlanAgent implements PlanAgent {

	private final Umlauf umlauf;
	private final Vehicle vehicle;
	private final TransitStopAgentTracker agentTracker;
	private final Plan plan;
	private final Iterator<PlanElement> peIterator;
	private PlanElement currentPlanElement = null;
	private final Iterator<UmlaufStueckI> umlaufIterator;
	private UmlaufStueckI currentUmlaufStueck = null;

	public TransitDriverPlanAgent(final Umlauf umlauf, final Vehicle vehicle, final TransitStopAgentTracker agentTracker, final String transitMode) {
		this.umlauf = umlauf;
		this.vehicle = vehicle;
		this.agentTracker = agentTracker;

		this.umlaufIterator = this.umlauf.getUmlaufStuecke().iterator();

		this.plan = new PlanImpl();
		for (UmlaufStueckI stage : umlauf.getUmlaufStuecke()) {
			Leg leg = new LegImpl(transitMode);
			leg.setRoute(stage.getCarRoute());
			plan.addLeg(leg);
		}
		this.peIterator = this.plan.getPlanElements().iterator();
		useNextPlanElement();
	}

	@Override
	public Plan getPlan() {
		return this.plan;
	}

	@Override
	public PlanElement getCurrentPlanElement() {
		return this.currentPlanElement;
	}

	@Override
	public PlanElement useNextPlanElement() {
		if (this.peIterator.hasNext()) {
			this.currentPlanElement = this.peIterator.next();
			this.currentUmlaufStueck = this.umlaufIterator.next();
		} else {
			this.currentPlanElement = null;
			this.currentUmlaufStueck = null;
		}
		return this.currentPlanElement;
	}

	@Override
	public double getWeight() {
		return 1.0;
	}
//
//	public Vehicle getVehicle() {
//		return vehicle;
//	}

	public UmlaufStueckI getCurrentUmlaufStueck() {
		return currentUmlaufStueck;
	}

}
