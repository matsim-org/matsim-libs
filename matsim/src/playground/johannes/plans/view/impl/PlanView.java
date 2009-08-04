/* *********************************************************************** *
 * project: org.matsim.*
 * PlanImpl.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.johannes.plans.view.impl;

import java.util.List;

import playground.johannes.plans.plain.impl.PlainPlanImpl;
import playground.johannes.plans.view.Activity;
import playground.johannes.plans.view.Leg;
import playground.johannes.plans.view.Plan;

/**
 * @author illenberger
 *
 */
public class PlanView extends AbstractView<PlainPlanImpl> implements Plan {

	
	public PlanView(PlainPlanImpl rawPlan) {
		super(rawPlan);
//		delegate = new PlainPlanImpl<Activity, Leg>();
//		for(PlainActivity rawAct : rawPlan.getActivities()) {
//			Activity act = new ActivityImpl(rawAct);
//			delegate.getActivities().add(act);
//		}
//		for(PlainLeg<?> rawLeg : rawPlan.getLegs()) {
//			Leg leg = new LegImpl(rawLeg);
//			delegate.getLegs().add(leg);
//		}
			
	}
	
	public List<Activity> getActivities() {
//		return delegate.getActivities();
		return null;
	}

	
	public List<Leg> getLegs() {
//		return delegate.getLegs();
		return null;
	}

	/* (non-Javadoc)
	 * @see playground.johannes.plans.view.impl.AbstractView#update()
	 */
	@Override
	protected void update() {
		// TODO Auto-generated method stub
		
	}

}
