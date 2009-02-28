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

package org.matsim.population.algorithms;

import org.matsim.basic.v01.BasicPlanImpl.ActIterator;
import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.Network;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Population;
import org.matsim.population.PopulationImpl;

/**
 * @author laemmel
 *
 */
public class PlansFilterActInArea extends AbstractPersonAlgorithm {

	private final Network network;
	private final Population plans;
	private String actType;

	public PlansFilterActInArea(final Network net, final String a){
		this.network = net;
		this.actType = a;
		this.plans =  new PopulationImpl();
	}

	public void setArea(final String a){
		this.actType = a;
	}

	@Override
	public void run(final Person person) {
		ActIterator it = person.getSelectedPlan().getIteratorAct();
		while (it.hasNext()){
			Act act = (Act) it.next();
			String type = act.getType();
			if (type.contains(this.actType)) {
				if (this.network.getLink(act.getLink().getId()) != null) {
					this.plans.addPerson(person);
				}
				return;
			}
		}
	}

	public Population getPlans(){
		return this.plans;
	}

}
