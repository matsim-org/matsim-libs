/* *********************************************************************** *
 * project: org.matsim.*
 * Plan.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

package org.matsim.core.population;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Customizable;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.replanning.inheritance.PlanInheritanceModule;
import org.matsim.core.scenario.CustomizableUtils;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

/* deliberately package */  final class PlanImpl implements Plan {

	private Id<Plan> id=  null;

	private ArrayList<PlanElement> actsLegs = new ArrayList<>();

	private Double score = null;
	private Person person = null;

	private String type = null;

	@SuppressWarnings("unused")
	private final static Logger log = LogManager.getLogger(Plan.class);

	private Customizable customizableDelegate;

	private final Attributes attributes = new AttributesImpl();

	@Override
	public final Attributes getAttributes() {
		return this.attributes;
	}

	/* package */ PlanImpl() {}

//	@Override
//	public final Activity createAndAddActivity(final String type1) {
//		Activity a = new ActivityImpl(type1);
//		// (PlanImpl knows the corresponding Activity implementation, so it does not have to go through the factory.  kai, jun'16)
//
//		this.addActivity(a) ;
//		return a;
//	}



	//////////////////////////////////////////////////////////////////////
	// create methods
	//////////////////////////////////////////////////////////////////////

//	@Override
//	public Leg createAndAddLeg(final String mode) {
//		verifyCreateLeg();
//		Leg leg = new LegImpl( mode ) ;
//		getPlanElements().add(leg);
//		return leg;
//	}

//	private static void verifyCreateLeg(Plan plan) throws IllegalStateException {
//		if (plan.getPlanElements().size() == 0) {
//			throw new IllegalStateException("The order of 'acts'/'legs' is wrong in some way while trying to create a 'leg'.");
//		}
//	}

	//////////////////////////////////////////////////////////////////////
	// remove methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final Person getPerson() {
		return this.person;
	}

	@Override
	public void setPerson(final Person person) {
		this.person = person;
	}

	@Override
	public final Double getScore() {
		return this.score;
	}

	@Override
	public void setScore(final Double score) {
		this.score = score;
	}

    @Override
	public String getType() {
		return this.type;
	}

    @Override
	public void setType(final String type) {
		this.type = type;
	}

	@Override
	public Id<Plan> getId() {
		if(this.id!=null)
			return this.id;
		else {
			if(this.getAttributes().getAttribute(PlanInheritanceModule.PLAN_ID)!=null)
				return Id.create(this.getAttributes().getAttribute(PlanInheritanceModule.PLAN_ID).toString(),Plan.class);
			else return null;
		}

	}

	@Override
	public void setPlanId(Id<Plan> planId) {
		this.getAttributes().putAttribute(PlanInheritanceModule.PLAN_ID, planId.toString());
		this.id = planId;
	}

	@Override
	public int getIterationCreated() {
		return (int) this.getAttributes().getAttribute(PlanInheritanceModule.ITERATION_CREATED);
	}

	@Override
	public void setIterationCreated(int iteration) {
		this.getAttributes().putAttribute(PlanInheritanceModule.ITERATION_CREATED, iteration);
	}

	@Override
	public String getPlanMutator() {
		return (String) this.getAttributes().getAttribute(PlanInheritanceModule.PLAN_MUTATOR);
	}

	@Override
	public void setPlanMutator(String planMutator) {
		this.getAttributes().putAttribute(PlanInheritanceModule.PLAN_MUTATOR, planMutator);
	}

	@Override
	public final List<PlanElement> getPlanElements() {
		return this.actsLegs;
	}

	@Override
	public final void addLeg(final Leg leg) {
		this.actsLegs.add(leg);
	}

	@Override
	public final void addActivity(final Activity act) {
		this.actsLegs.add(act);
	}

	@Override
	public final String toString() {

		String scoreString = "undefined";
		if (this.getScore() != null) {
			scoreString = this.getScore().toString();
		}
		String personIdString = "undefined" ;
		if ( this.getPerson() != null ) {
			personIdString = this.getPerson().getId().toString() ;
		}

		return "[score=" + scoreString + "]" +
//				"[selected=" + PersonUtils.isSelected(this) + "]" +
				"[nof_acts_legs=" + getPlanElements().size() + "]" +
				"[type=" + this.type + "]" +
				"[personId=" + personIdString + "]" ;
	}

	@Override
	public final Map<String, Object> getCustomAttributes() {
		if (this.customizableDelegate == null) {
			this.customizableDelegate = CustomizableUtils.createCustomizable();
		}
		return this.customizableDelegate.getCustomAttributes();
	}



//	public final void setLocked() {
//		for ( PlanElement pe : this.actsLegs ) {
//			if ( pe instanceof ActivityImpl ) {
//				((ActivityImpl) pe).setLocked();
//			} else if ( pe instanceof LegImpl ) {
//				((LegImpl) pe).setLocked() ;
//			}
//		}
//	}

}
