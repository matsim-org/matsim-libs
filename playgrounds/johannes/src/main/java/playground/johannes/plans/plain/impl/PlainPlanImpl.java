/* *********************************************************************** *
 * project: org.matsim.*
 * RawPlanImpl.java
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
package playground.johannes.plans.plain.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import playground.johannes.plans.plain.PlainPlan;
import playground.johannes.plans.plain.PlainPlanElement;

/**
 * @author illenberger
 *
 */
public class PlainPlanImpl extends AbstractModifiable implements PlainPlan {
	
	private ArrayList<PlainPlanElementImpl> elements;

	private List<PlainPlanElementImpl> unmodifiableElements;
	
	private Double score;
	
	public PlainPlanImpl() {
		elements = new ArrayList<PlainPlanElementImpl>();
		unmodifiableElements = Collections.unmodifiableList(elements);
	}
	
	public void addPlanElement(PlainPlanElement element) {
		elements.add((PlainPlanElementImpl) element);
		elements.trimToSize();
		modified();
	}

	public List<? extends PlainPlanElementImpl> getPlanElements() {
		return unmodifiableElements;
//		return elements;
	}

	public Double getScore() {
		return score;
	}

	public void removePlanElement(PlainPlanElement element) {
		elements.remove(element);
		modified();
	}

	public void setScore(Double score) {
		this.score = score;
		modified();
	}

}
