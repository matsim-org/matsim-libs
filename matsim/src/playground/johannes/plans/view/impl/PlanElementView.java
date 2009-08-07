/* *********************************************************************** *
 * project: org.matsim.*
 * PlanElementView.java
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

import playground.johannes.plans.plain.impl.PlainPlanElementImpl;
import playground.johannes.plans.view.PlanElement;

/**
 * @author illenberger
 *
 */
public abstract class PlanElementView<T extends PlainPlanElementImpl> extends AbstractView<T> implements PlanElement {

	protected PlanElementView(T delegate) {
		super(delegate);
	}

	public double getEndTime() {
		return delegate.getEndTime();
	}

	public void setEndTime(double time) {
		delegate.setEndTime(time);
	}

}
