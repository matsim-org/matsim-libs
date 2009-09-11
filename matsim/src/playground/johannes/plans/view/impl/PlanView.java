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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import playground.johannes.plans.plain.impl.PlainActivityImpl;
import playground.johannes.plans.plain.impl.PlainLegImpl;
import playground.johannes.plans.plain.impl.PlainPlanElementImpl;
import playground.johannes.plans.plain.impl.PlainPlanImpl;
import playground.johannes.plans.view.Plan;
import playground.johannes.plans.view.PlanElement;

/**
 * @author illenberger
 *
 */
public class PlanView extends AbstractView<PlainPlanImpl> implements Plan {

	private ArrayList<PlanElementView<?>> planElements = new ArrayList<PlanElementView<?>>(0);

	private List<PlanElementView<?>> unmodifiabelElements;
	
	public PlanView(PlainPlanImpl rawPlan) {
		super(rawPlan);
		unmodifiabelElements = Collections.unmodifiableList(planElements);
	}
	
	@Override
	protected void update() {
		Collection<? extends PlainPlanElementImpl> newElements = synchronizeCollections(delegate.getPlanElements(), planElements);
		
		for(PlainPlanElementImpl e : newElements) {
			PlanElementView<?> view;
			if(e instanceof PlainActivityImpl) {
				view = new ActivityView((PlainActivityImpl)e);
			} else {
				view = new LegView((PlainLegImpl)e);
			}
			planElements.add(view);
		}
		planElements.trimToSize();
	}

	public void addPlanElement(PlanElement element) {
		delegate.addPlanElement(((PlanElementView<?>)element).getDelegate());
		planElements.add((PlanElementView<?>) element);
	}

	public List<? extends PlanElement> getPlanElements() {
		synchronize();
		return unmodifiabelElements;
	}

	public Double getScore() {
		return delegate.getScore();
	}

	public void removePlanElement(PlanElement element) {
		delegate.removePlanElement(((PlanElementView<?>)element).getDelegate());
		planElements.remove(element);
	}

	public void setScore(Double score) {
		delegate.setScore(score);
	}

}
