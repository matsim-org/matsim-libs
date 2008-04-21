/* *********************************************************************** *
 * project: org.matsim.*
 * PlansFilterActInArea.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.plans.algorithms;

import org.matsim.basic.v01.BasicAct;
import org.matsim.basic.v01.BasicPlanImpl.ActIterator;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Person;
import org.matsim.plans.Plans;

/**
 * @author laemmel
 *
 */
public class PlansFilterActInArea extends PersonAlgorithm {

	private final NetworkLayer network;
	private final Plans plans;
	private String actType;

	public PlansFilterActInArea(final NetworkLayer net, final String a){
		this.network = net;
		this.actType = a;
		this.plans =  new Plans();
	}

	public void setArea(final String a){
		this.actType = a;
	}

	@Override
	public void run(final Person person) {
		ActIterator it = person.getSelectedPlan().getIteratorAct();
		while (it.hasNext()){
			BasicAct act = it.next();
			String type = act.getType();
			if (type.contains(this.actType)) {
				if(this.network.getLink(act.getLink().getId().toString()) != null) {
					try {
						this.plans.addPerson(person);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				return;
			}
		}
	}

	public Plans getPlans(){
		return this.plans;
	}

}
