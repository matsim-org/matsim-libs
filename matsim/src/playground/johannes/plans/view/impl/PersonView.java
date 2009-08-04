/* *********************************************************************** *
 * project: org.matsim.*
 * PersonImpl.java
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

import playground.johannes.plans.plain.impl.PlainPersonImpl;
import playground.johannes.plans.view.Person;
import playground.johannes.plans.view.Plan;

/**
 * @author illenberger
 *
 */
public class PersonView extends AbstractView<PlainPersonImpl> implements Person {
	
	private List<Plan> plans;
	
	public PersonView(PlainPersonImpl plainPerson) {
		super(plainPerson);
	}
	
	public List<Plan> getPlans() {
		synchronize();
		return plans;
	}

	@Override
	protected void update() {
		
	}

}
